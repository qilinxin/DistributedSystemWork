package org.adelaide.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.PostConstruct;
import org.adelaide.dto.CommonResult;
import org.adelaide.dto.WeatherInfoDTO;
import org.adelaide.dto.WeatherInfoWrapperDTO;
import org.adelaide.util.JsonUtil;
import org.adelaide.util.LamportClockUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AggregationService {

    private static final Logger logger = LoggerFactory.getLogger(AggregationService.class);

    //provide cache for query method. In actual use, the number of query operations should be much greater than the number of update operations.

    private static final AtomicInteger VERSION_ID = new AtomicInteger(0);
    private final Gson gson = new Gson();
    private static boolean newFileFlag = false;

    @Value("update_flag")
    private static boolean Update_Flag;

    // Create ScheduledExecutorService for periodic tasks
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // LamportClockUtil instance for handling Lamport Clock
    private final LamportClockUtil lamportClock = LamportClockUtil.getInstance();

    public static final Map<String, WeatherInfoDTO> WEATHER_MAP_FOR_QUERY = new ConcurrentHashMap<>();

    public static final Map<String, Map<String, WeatherInfoWrapperDTO>> WEATHER_MAP_FOR_STORE = new ConcurrentHashMap<>();

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
        if (StringUtils.isEmpty(id)) {
            return new CommonResult(204, "weather info not found, please check");
        }
        // Handle received event time
        lamportClock.receiveEvent(clock);

        // Print current clock value and requested ID
        System.out.println("Clock + id ==" + lamportClock.getTime() + ";;" + id);

        WeatherInfoDTO res = WEATHER_MAP_FOR_QUERY.get(id);
        if (res == null) {
            return new CommonResult(404, "weather info not found, please check");
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
        Path path = Paths.get("weatherInfoMap.json");
        Path absolutePath = path.toAbsolutePath();
        // 打印绝对路径
        System.out.println("Absolute path of the file: " + absolutePath);
        try (InputStream inputStream = Files.newInputStream(path)) {
            try (InputStreamReader reader = new InputStreamReader(inputStream)) {
                Type weatherMapType = new TypeToken<Map<String, Map<String, WeatherInfoWrapperDTO>>>() {}.getType();

                // Deserialize JSON into WEATHER_MAP_FOR_STORE
                Map<String, Map<String, WeatherInfoWrapperDTO>> loadedData = gson.fromJson(reader, weatherMapType);

                if (loadedData != null && !loadedData.isEmpty()) {
                    // If data is loaded successfully and is not empty, put it into WEATHER_MAP_FOR_STORE
                    WEATHER_MAP_FOR_STORE.putAll(loadedData);
                    for (Map.Entry<String, Map<String, WeatherInfoWrapperDTO>> outerEntry : WEATHER_MAP_FOR_STORE.entrySet()) {
                        Map<String, WeatherInfoWrapperDTO> innerMap = outerEntry.getValue();

                        for (Map.Entry<String, WeatherInfoWrapperDTO> innerEntry : innerMap.entrySet()) {
                            WeatherInfoWrapperDTO wrapper = innerEntry.getValue();
                            // Convert WeatherInfoWrapperDTO to WeatherInfoDTO
                            WeatherInfoDTO weatherInfo = wrapper.getWeatherInfo();
                            // Put the converted WeatherInfoDTO into WEATHER_MAP_FOR_QUERY
                            WEATHER_MAP_FOR_QUERY.put(weatherInfo.getId(), weatherInfo);
                        }
                    }
                    // Retrieve maximum version number and set it, default to 1 if not present
                    VERSION_ID.set(WEATHER_MAP_FOR_STORE.values().stream()
                            .flatMap(innerMap -> innerMap.values().stream())
                            .mapToInt(WeatherInfoWrapperDTO::getVersionId)
                            .max()
                            .orElse(1));

                    logger.info("Successfully loaded weatherInfoMap, total records: {} ", WEATHER_MAP_FOR_STORE.size());
                } else {
                    // Loaded data is either null or empty
                    logger.info("Invalid or empty weatherInfoMap.json content, initializing empty WEATHER_MAP.");
                    newFileFlag = true;
                }
            }
            logger.info(JsonUtil.toJson(WEATHER_MAP_FOR_STORE));

        } catch (IOException e) {
            // Catch other IO errors
            logger.error("Failed to load weatherInfoMap.json file, it may not exist or failed to read.", e);
            newFileFlag = true;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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


        WeatherInfoDTO weatherInfo = null;
        if (StringUtils.isBlank(weatherInfoStr) || StringUtils.isBlank(port)) {
            return new CommonResult(204, "weather data error, please check!");
        }
        try {
            weatherInfo = JsonUtil.fromJson(weatherInfoStr, WeatherInfoDTO.class);
        } catch (IllegalAccessException | InstantiationException | IllegalArgumentException e) {
            return new CommonResult(500, "Internal server error");
        }
        String currentKey = weatherInfo.getId();

        if (StringUtils.isEmpty(currentKey)) {
            return new CommonResult(204, "weather data error, please check!");
        }

        int code = newFileFlag ? 201 : 200;
        newFileFlag = false;
        if (WEATHER_MAP_FOR_QUERY.size() >= 20 && !WEATHER_MAP_FOR_QUERY.containsKey(currentKey)) {
            deleteOldestData();
        }

        //update weather in the cache for query
        WEATHER_MAP_FOR_QUERY.put(currentKey, weatherInfo);
        //if current port map not exists ,create and put, otherwise update
        if(WEATHER_MAP_FOR_STORE.isEmpty() || WEATHER_MAP_FOR_STORE.get(port) == null) {
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
        return new CommonResult(code, currentClock);
    }

    /**
     * Queries all cached weather information in the WEATHER_MAP.
     *
     * @return a map of all weather information currently stored in the cache
     */
    public Map<String, Map<String, WeatherInfoWrapperDTO>> queryCacheInfo() {
        System.out.println("WEATHER_MAP_FOR_STORE.isEmpty()======"+WEATHER_MAP_FOR_STORE.isEmpty());
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
    public synchronized void updateFileInfo() throws IOException {
        // Get the file from resources directory
        Path path = Paths.get("weatherInfoMap.json");
        File file = path.toFile();

        // Ensure the file is writable
        if (file.exists() && file.canWrite()) {
            try (FileWriter writer = new FileWriter(file)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(WEATHER_MAP_FOR_STORE, writer);
                System.out.println("weatherInfoMap.json is updated at: " + file.getAbsolutePath());
            }
        } else {
            System.out.println("Cannot write to the existing weatherInfoMap.json file in resources.");
        }
    }

    /**
     * Monitors the WEATHER_MAP by iterating over its entries and removing those that have not been updated for more than 30 seconds.
     * If any entries are removed, the updated information is saved back to the file.
     */
    private void monitorWeatherMap() {
//        System.out.println("Monitoring WEATHER_MAP. Current size: " + WEATHER_MAP.size());
//        int delCnt = checkExpiredData();
//        if (delCnt > 0) {
//            try {
//                updateFileInfo();
//            } catch (IOException e) {
//                // No specific handling for monitored update failures, just print error information
//                e.printStackTrace();
//            }
//        }
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

    public void deleteOldestData() {
        synchronized (WEATHER_MAP_FOR_STORE) {
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
                        System.out.println("print info oldestOuterKey = " + outerKey + ", oldestInnerKey =  " + innerKey + ", oldestWrapper = " + wrapper);
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
                    WEATHER_MAP_FOR_QUERY.remove(oldestInnerKey);
                    innerMap.remove(oldestInnerKey);
                    // If the inner map becomes empty, remove the outer key
                    if (innerMap.isEmpty()) {
                        WEATHER_MAP_FOR_STORE.remove(oldestOuterKey);
                    }
                }
            }
        }
    }

}
