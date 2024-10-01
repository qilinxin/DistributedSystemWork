package org.adelaide.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.PostConstruct;
import org.adelaide.dto.CommonResult;
import org.adelaide.dto.WeatherInfoDTO;
import org.adelaide.dto.WeatherInfoWrapperDTO;
import org.adelaide.util.LamportClockUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AggregationService {

    private static final Logger logger = LoggerFactory.getLogger(AggregationService.class);


    private static final String PROJECT_ROOT_PATH = System.getProperty("user.dir");
    //provide cache for query method. In actual use, the number of query operations should be much greater than the number of update operations.
    private static final Map<String, WeatherInfoDTO> WEATHER_MAP_FOR_QUERY = new HashMap<>();

    private static final Map<String, Map<String, WeatherInfoWrapperDTO>> WEATHER_MAP_FOR_STORE = new HashMap<>();
    private static final AtomicInteger VERSION_ID = new AtomicInteger(0);
    private final Gson gson = new Gson();
    private static boolean newFileFlag = false;

    @Value("update_flag")
    private static boolean Update_Flag;

    // Create ScheduledExecutorService for periodic tasks
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // LamportClockUtil instance for handling Lamport Clock
    private final LamportClockUtil lamportClock = LamportClockUtil.getInstance();

    /**
     * Starts the periodic task to monitor the WEATHER_MAP information when the system starts.
     * This method is annotated with {@code @PostConstruct} to indicate that it should be run after dependency injection is complete.
     */
    @PostConstruct
    public void startWeatherMapListener() {
        // Check WEATHER_MAP data every 5 seconds
        scheduler.scheduleAtFixedRate(this::monitorWeatherMap, 0, 5, TimeUnit.SECONDS);
    }

    /**
     * Queries the weather information by its ID and updates the Lamport Clock based on the given clock value.
     *
     * @param clock the Lamport clock value received from the client request
     * @param id    the weather ID used to query the WEATHER_MAP
     * @return      a {@code CommonResult} containing the queried weather information or an error message if not found
     */
    public CommonResult queryWeatherById(int clock, String id) {
        // Handle received event time
        lamportClock.receiveEvent(clock);

        // Print current clock value and requested ID
        System.out.println("Clock + id ==" + lamportClock.getTime() + ";;" + id);

        WeatherInfoDTO res = WEATHER_MAP_FOR_QUERY.get(id);
        if (res == null) {
            return new CommonResult(500, "weather info not found, please check");
        }
        return new CommonResult(200, "", res, lamportClock.getTime());
    }

    /**
     * Loads weather information from a JSON file during system startup.
     * If the file is not found, an empty WEATHER_MAP is initialized.
     * This method is annotated with {@code @PostConstruct} to indicate that it should run after dependency injection is complete.
     */
    @PostConstruct
    public void loadWeatherData() {
        // Load the file from the resources directory
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("weatherInfoMap.json")) {
            if (inputStream != null) {
                try (InputStreamReader reader = new InputStreamReader(inputStream)) {
                    Type weatherMapType = new TypeToken<Map<String, WeatherInfoWrapperDTO>>() {}.getType();
                    WEATHER_MAP_FOR_STORE.putAll(gson.fromJson(reader, weatherMapType));

                    if (!WEATHER_MAP_FOR_STORE.isEmpty()) {
                        // Retrieve maximum version number and set it, default to 1 if not present
                        VERSION_ID.set(WEATHER_MAP_FOR_STORE.values().stream()
                                .flatMap(innerMap -> innerMap.values().stream())
                                .mapToInt(WeatherInfoWrapperDTO::getVersionId)
                                .max()
                                .orElse(1));
                        logger.info("Successfully loaded weatherInfoMap, total records: {} ", WEATHER_MAP_FOR_QUERY.size());
                    } else {
                        logger.info("Invalid weatherInfoMap.json content, initializing empty WEATHER_MAP.");
                        newFileFlag = true;
                    }
                }
            } else {
                // File not found in resources
                System.out.println("File not found in resources, creating a new empty WEATHER_MAP.");
                newFileFlag = true;
            }
        } catch (IOException e) {
            // Catch other IO errors
            System.out.println("Failed to load weatherInfoMap.json file, it may not exist or failed to read.");
            newFileFlag = true;
        }
        logger.info(gson.toJson(WEATHER_MAP_FOR_STORE));
    }

    /**
     * Creates or updates the weather information in the WEATHER_MAP.
     * The Lamport Clock is incremented during this update to track the logical time of the event.
     *
     * @param weatherInfoStr JSON string representing the weather information
     * @param clock          the Lamport clock value received from the client request
     * @return               a {@code CommonResult} indicating whether the operation was successful or failed
     */
    public CommonResult saveOrUpdateWeatherInfo(String weatherInfoStr, String port, int clock) {
        // Handle received event time
        lamportClock.receiveEvent(clock);
        // Increment Lamport Clock when recording an update event
        int currentClock = lamportClock.increment();
        logger.info("Updating weather info with Lamport clock: {}", currentClock);


        WeatherInfoDTO weatherInfo = gson.fromJson(weatherInfoStr, WeatherInfoDTO.class);
        String currentKey = weatherInfo.getId();

        if (StringUtils.isEmpty(currentKey)) {
            return new CommonResult(204, "weather data error, please check!");
        }

        int code = newFileFlag ? 201 : 200;
        newFileFlag = false;
        if (WEATHER_MAP_FOR_QUERY.size() >= 20 && WEATHER_MAP_FOR_QUERY.containsKey(currentKey)) {
            deleteOldestData();
        }

        //update cache
        WEATHER_MAP_FOR_QUERY.put(currentKey, weatherInfo);
        //if current port map not exists ,create and put, otherwise update
        if(WEATHER_MAP_FOR_STORE.get(port) == null) {
            Map<String, WeatherInfoWrapperDTO> resMap = new HashMap<>();
            resMap.put(currentKey, new WeatherInfoWrapperDTO(weatherInfo, currentClock));
            WEATHER_MAP_FOR_STORE.put(port, resMap);
        } else {
            WEATHER_MAP_FOR_STORE.get(port).put(currentKey, new WeatherInfoWrapperDTO(weatherInfo, currentClock));
        }


        //modify file after update caches
        try {
            this.updateFileInfo();
        } catch (IOException e) {
            code = 500;
        }
        return new CommonResult(code, clock);
    }

    /**
     * Queries all cached weather information in the WEATHER_MAP.
     *
     * @return a map of all weather information currently stored in the cache
     */
    public Map<String, Map<String, WeatherInfoWrapperDTO>> queryCacheInfo() {
        if (WEATHER_MAP_FOR_STORE.isEmpty()) {
            loadWeatherData();
        }
        return WEATHER_MAP_FOR_STORE;
    }

    /**
     * Writes the current WEATHER_MAP data to the weatherInfoMap.json file.
     * The method is synchronized to ensure thread safety when multiple threads attempt to write the file concurrently.
     *
     * @throws IOException if the file operation fails, such as issues with writing to disk
     */
    private synchronized void updateFileInfo() throws IOException {
        String filePath = Paths.get(PROJECT_ROOT_PATH, "weatherInfoMap.json").toString();

        // Use try-with-resources to ensure FileWriter is automatically closed
        try (FileWriter writer = new FileWriter(filePath)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(WEATHER_MAP_FOR_STORE, writer);
            System.out.println("weatherInfoMap.json is updated");
        }
    }

    /**
     * Monitors the WEATHER_MAP by iterating over its entries and removing those that have not been updated for more than 30 seconds.
     * If any entries are removed, the updated information is saved back to the file.
     */
    private void monitorWeatherMap() {
//        System.out.println("Monitoring WEATHER_MAP. Current size: " + WEATHER_MAP.size());
        int delCnt = checkExpiredData();
        if (delCnt > 0) {
            try {
                updateFileInfo();
            } catch (IOException e) {
                // No specific handling for monitored update failures, just print error information
                e.printStackTrace();
            }
        }
    }


    private int checkExpiredData() {
        int delCnt = 0;
        for (Map.Entry<String, Map<String, WeatherInfoWrapperDTO>> entry : WEATHER_MAP_FOR_STORE.entrySet()) {
            Iterator<Map.Entry<String, WeatherInfoWrapperDTO>> innerIterator = entry.getValue().entrySet().iterator();
            while (innerIterator.hasNext()) {
                Map.Entry<String, WeatherInfoWrapperDTO> innerEntry = innerIterator.next();
                if (System.currentTimeMillis() - innerEntry.getValue().getLastUpdateTime() > 30000) {
                    System.out.println("WeatherInfoDTO: " + innerEntry.getValue().getWeatherInfo().getId() + " removed due to being outdated");
                    innerIterator.remove();
                    //delete info in the query cache
                    WEATHER_MAP_FOR_QUERY.remove(innerEntry.getKey());
                    delCnt++;
                }
            }
        }
        return delCnt;
    }

    private void deleteOldestData() {
        // Initialize variables to keep track of the oldest entry
        String oldestOuterKey = null;
        String oldestInnerKey = null;
        WeatherInfoWrapperDTO oldestWrapper = null;

        // Iterate over the map to find the oldest entry
        for (Map.Entry<String, Map<String, WeatherInfoWrapperDTO>> outerEntry : WEATHER_MAP_FOR_STORE.entrySet()) {
            String outerKey = outerEntry.getKey();
            Map<String, WeatherInfoWrapperDTO> innerMap = outerEntry.getValue();

            for (Map.Entry<String, WeatherInfoWrapperDTO> innerEntry : innerMap.entrySet()) {
                String innerKey = innerEntry.getKey();
                WeatherInfoWrapperDTO wrapper = innerEntry.getValue();

                // Update the oldestWrapper if this wrapper has an older timestamp
                if (oldestWrapper == null || wrapper.getLastUpdateTime() < oldestWrapper.getLastUpdateTime()) {
                    oldestOuterKey = outerKey;
                    oldestInnerKey = innerKey;
                    oldestWrapper = wrapper;
                }
            }
        }

        // If we found the oldest entry, remove it from the map
        if (oldestOuterKey != null && oldestInnerKey != null) {
            Map<String, WeatherInfoWrapperDTO> innerMap = WEATHER_MAP_FOR_STORE.get(oldestOuterKey);
            if (innerMap != null) {
                innerMap.remove(oldestInnerKey);
            }
        }
    }
}
