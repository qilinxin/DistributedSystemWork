package org.adelaide.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AggregationService {

    private static final Logger logger = LoggerFactory.getLogger(AggregationService.class);


    private static final String PROJECT_ROOT_PATH = System.getProperty("user.dir");
    private static Map<String, WeatherInfoWrapperDTO> WEATHER_MAP = new HashMap<>();
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
     * Monitors the WEATHER_MAP by iterating over its entries and removing those that have not been updated for more than 30 seconds.
     * If any entries are removed, the updated information is saved back to the file.
     */
    private void monitorWeatherMap() {
        System.out.println("Monitoring WEATHER_MAP. Current size: " + WEATHER_MAP.size());
        int delCnt = 0;

        // Iterate over and check the WEATHER_MAP data
        Iterator<Map.Entry<String, WeatherInfoWrapperDTO>> iterator = WEATHER_MAP.entrySet().iterator();
        while (iterator.hasNext() && Update_Flag) {
            Map.Entry<String, WeatherInfoWrapperDTO> entry = iterator.next();
            if (System.currentTimeMillis() - entry.getValue().getLastUpdateTime() > 30000) {
                System.out.println("WeatherInfoDTO: " + entry.getValue().getWeatherInfo().getId() + " removed due to being outdated");
                iterator.remove(); // Safely remove element
                delCnt++;
            }
        }

        if (delCnt > 0) {
            try {
                updateFileInfo();
            } catch (IOException e) {
                // No specific handling for monitored update failures, just print error information
                e.printStackTrace();
            }
        }
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

        WeatherInfoWrapperDTO res = WEATHER_MAP.get(id);
        if (res == null || res.getWeatherInfo() == null) {
            return new CommonResult(500, "weather info not found, please check");
        }
        return new CommonResult(200, "", res.getWeatherInfo(), lamportClock.getTime());
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
                    WEATHER_MAP.putAll(gson.fromJson(reader, weatherMapType));

                    if (!WEATHER_MAP.isEmpty()) {
                        // Retrieve maximum version number and set it, default to 1 if not present
                        VERSION_ID.set(WEATHER_MAP.values().stream()
                                .mapToInt(WeatherInfoWrapperDTO::getVersionId)
                                .max()
                                .orElse(1));
                        System.out.println("Successfully loaded weatherInfoMap, total records: " + WEATHER_MAP.size());
                    } else {
                        System.out.println("Invalid weatherInfoMap.json content, initializing empty WEATHER_MAP.");
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
        logger.info(gson.toJson(WEATHER_MAP));
    }

    /**
     * Creates or updates the weather information in the WEATHER_MAP.
     * The Lamport Clock is incremented during this update to track the logical time of the event.
     *
     * @param weatherInfoStr JSON string representing the weather information
     * @param clock          the Lamport clock value received from the client request
     * @return               a {@code CommonResult} indicating whether the operation was successful or failed
     */
    public CommonResult saveOrUpdateWeatherInfo(String weatherInfoStr, int clock) {
        // Handle received event time
        lamportClock.receiveEvent(clock);

        WeatherInfoDTO weatherInfo = gson.fromJson(weatherInfoStr, WeatherInfoDTO.class);
        if (weatherInfo == null || weatherInfo.getId() == null) {
            return new CommonResult(204, "weather data error, please check!");
        }

        // Increment Lamport Clock when recording an update event
        int currentClock = lamportClock.increment();
        System.out.println("Updating weather info with Lamport clock: " + currentClock);
        int code = newFileFlag ? 201 : 200;
        newFileFlag = false;
        WEATHER_MAP.put(weatherInfo.getId(), new WeatherInfoWrapperDTO(weatherInfo, VERSION_ID.incrementAndGet()));
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
    public Map<String, WeatherInfoWrapperDTO> queryCacheInfo() {
        if (WEATHER_MAP.isEmpty()) {
            loadWeatherData();
        }
        return WEATHER_MAP;
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
            gson.toJson(WEATHER_MAP, writer);
            System.out.println("weatherInfoMap.json is updated");
        }
    }
}
