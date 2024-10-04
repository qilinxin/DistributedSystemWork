import com.google.gson.Gson;
import org.adelaide.AggregationApplication;
import org.adelaide.dto.CommonResult;
import org.adelaide.dto.WeatherInfoDTO;
import org.adelaide.dto.WeatherInfoWrapperDTO;
import org.adelaide.service.AggregationService;
import org.adelaide.util.JsonUtil;
import org.adelaide.util.LamportClockUtil;
import org.adelaide.util.RandomWeatherInfoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for AggregationService.
 * This class contains unit tests for various functionalities of the AggregationService.
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes= AggregationApplication.class)
public class AggregationServiceTest {

    @InjectMocks
    private AggregationService aggregationService;

    private Gson gson = new Gson();

    @Value("${update_flag}")
    private boolean updateFlag;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Clear the caches before each test to ensure a clean state
        AggregationService.WEATHER_MAP_FOR_QUERY.clear();
        AggregationService.WEATHER_MAP_FOR_STORE.clear();
    }

    /**
     * Test exceeding cache capacity.
     * This test ensures that when the cache reaches its capacity, the oldest entries are removed.
     */
    @Test
    public void testCapacity() throws IllegalAccessException {
        int capacity = 25;

        // Insert double the capacity to force the eviction of old entries
        for (int i = 0; i <= capacity; i++) {
            aggregationService.saveOrUpdateWeatherInfo(gson.toJson(RandomWeatherInfoUtil.createMockWeatherInfo("Capacity" + i)), "abcd", 123);
        }

        // Assert
        for (int i = 0; i < capacity; i++) {
            if (i <= 5) {
                assertFalse(AggregationService.WEATHER_MAP_FOR_QUERY.containsKey("Capacity" + i), "The first five data should be removed when the capacity is exceeded");
            } else {
                assertTrue(AggregationService.WEATHER_MAP_FOR_QUERY.containsKey("Capacity" + i), "The other data should be in the storage");
            }
        }
    }

    /**
     * Test querying weather information by its ID.
     * The purpose of this test is to ensure that when given a valid weather ID, the service returns the correct weather information.
     */
    @Test
    public void testQueryWeatherById() {
        // Arrange
        String weatherId = "IDS60999";
        WeatherInfoDTO mockWeatherInfo = RandomWeatherInfoUtil.createMockWeatherInfo();
        mockWeatherInfo.setId(weatherId);
        AggregationService.WEATHER_MAP_FOR_QUERY.put(weatherId, mockWeatherInfo);
        int clock = 123;

        // Act
        CommonResult result = aggregationService.queryWeatherById(123, weatherId);

        // Assert
        assertNotNull(result, "The result should not be null");
        assertNotEquals(result.getClock(), clock, "The clock value should be updated");
        assertEquals(200, result.getCode(), "The response code should be 200 for a successful query");
        assertEquals(weatherId, ((WeatherInfoDTO) result.getData()).getId(), "The weather ID should match the requested ID");
    }

    /**
     * Test saving new weather information.
     * This test ensures that when new weather information is provided, it is saved in the cache correctly.
     */
    @Test
    public void testSaveWeatherInfo() {
        // Arrange
        WeatherInfoDTO wi = RandomWeatherInfoUtil.createMockWeatherInfo();
        String weatherInfoStr = gson.toJson(wi);
        String port = "8080";
        String id = wi.getId();
        int clock = 123;

        assertFalse(AggregationService.WEATHER_MAP_FOR_QUERY.containsKey(id), "The query cache should not contain the new ID initially");

        // Act
        CommonResult result = aggregationService.saveOrUpdateWeatherInfo(weatherInfoStr, port, clock);

        // Assert
        assertNotNull(result, "The result should not be null");
        assertEquals(200, result.getCode(), "The response code should be 200 for a successful save");
        assertTrue(AggregationService.WEATHER_MAP_FOR_QUERY.containsKey(id), "The query cache should contain the new ID after saving");
        assertTrue(AggregationService.WEATHER_MAP_FOR_STORE.containsKey(port), "The store cache should contain data for the given port");
    }

    /**
     * Test updating existing weather information.
     * This test ensures that when updating weather information with an existing ID, the data is updated correctly in the cache.
     */
    @Test
    public void testUpdateWeatherInfo() {
        // Arrange
        WeatherInfoDTO wi = RandomWeatherInfoUtil.createMockWeatherInfo();
        String weatherInfoStr = gson.toJson(wi);
        String port = "8080";
        String id = wi.getId();
        int clock = 123;

        // Act - Initial save
        CommonResult result = aggregationService.saveOrUpdateWeatherInfo(weatherInfoStr, port, clock);

        // Assert - Initial save
        assertNotNull(result, "The initial save result should not be null");
        assertEquals(200, result.getCode(), "The response code should be 200 for a successful save");
        assertTrue(AggregationService.WEATHER_MAP_FOR_QUERY.containsKey(id), "The query cache should contain the ID after initial save");
        assertTrue(AggregationService.WEATHER_MAP_FOR_STORE.containsKey(port), "The store cache should contain data for the given port");

        // Act - Update save
        WeatherInfoDTO newWi = RandomWeatherInfoUtil.createMockWeatherInfo();
        newWi.setId(id);
        String newWeatherInfoStr = gson.toJson(newWi);
        CommonResult result2 = aggregationService.saveOrUpdateWeatherInfo(newWeatherInfoStr, port, clock);

        // Assert - Update save
        assertNotEquals(wi, AggregationService.WEATHER_MAP_FOR_QUERY.get(id), "The updated data should be different from the original data");
    }

    /**
     * Test querying all weather information from the cache.
     * This test ensures that the cache returns all the stored weather data correctly.
     */
    @Test
    public void testQueryCacheInfo() {
        // Arrange
        Map<String, WeatherInfoWrapperDTO> weatherMap = new HashMap<>();
        WeatherInfoDTO mockWeatherInfo = RandomWeatherInfoUtil.createMockWeatherInfo();
        String id = mockWeatherInfo.getId();
        weatherMap.put(id, new WeatherInfoWrapperDTO(mockWeatherInfo, 123));
        AggregationService.WEATHER_MAP_FOR_STORE.put("8080", weatherMap);

        // Act
        Map<String, Map<String, WeatherInfoWrapperDTO>> result = aggregationService.queryCacheInfo();

        // Assert
        assertNotNull(result, "The query result should not be null");
        assertFalse(result.isEmpty(), "The query result should not be empty");
    }

    /**
     * Test loading weather data from a JSON file.
     * This test verifies that weather information is loaded properly from the file into the cache.
     */
    @Test
    public void testLoadWeatherData() {
        // Act
        aggregationService.loadWeatherData();

        // Assert
        assertFalse(AggregationService.WEATHER_MAP_FOR_STORE.isEmpty(), "The cache should not be empty after loading data from the file");
    }

    /**
     * Test updating cache information to the file.
     * This test ensures that the weather information can be written to a JSON file without throwing an exception.
     */
    @Test
    public void testUpdateFileInfo() throws Exception {
        // Arrange
        WeatherInfoDTO mockWeatherInfo = RandomWeatherInfoUtil.createMockWeatherInfo();
        mockWeatherInfo.setId("IDS60999");
        WeatherInfoWrapperDTO wrapperDTO = new WeatherInfoWrapperDTO(mockWeatherInfo, 123);

        AggregationService.WEATHER_MAP_FOR_STORE.put("8080", new HashMap<>());
        AggregationService.WEATHER_MAP_FOR_STORE.get("8080").put("IDS60999", wrapperDTO);

        // Act & Assert
        assertDoesNotThrow(() -> aggregationService.updateFileInfo(), "No exception should be thrown when updating the file");
    }

    /**
     * Test deleting the oldest weather data.
     * This test ensures that when the oldest data is identified, it is deleted correctly from the cache.
     */
    @Test
    public void testDeleteOldestData() {
        // Arrange
        WeatherInfoWrapperDTO oldestInfo = new WeatherInfoWrapperDTO(RandomWeatherInfoUtil.createMockWeatherInfo(), System.currentTimeMillis() - 40000);
        WeatherInfoWrapperDTO newInfo = new WeatherInfoWrapperDTO(RandomWeatherInfoUtil.createMockWeatherInfo(), System.currentTimeMillis());

        Map<String, WeatherInfoWrapperDTO> innerMap = new HashMap<>();
        innerMap.put("oldest", oldestInfo);
        innerMap.put("new", newInfo);
        AggregationService.WEATHER_MAP_FOR_STORE.put("port1", innerMap);

        // Act
        aggregationService.deleteOldestData();

        // Assert
        assertFalse(AggregationService.WEATHER_MAP_FOR_STORE.get("port1").containsKey("oldest"), "The oldest data should have been deleted");
    }



    /**
     * Test data expiration.
     * This test ensures that data older than the specified expiration time is removed from the cache.
     */
    @Test
    public void testExpired() throws InterruptedException {
        String port1 = "1234";
        String port2 = "2345";

        // Insert data into port1 and port2 to test expiration
        for (int i = 1; i < 10; i++) {
            aggregationService.saveOrUpdateWeatherInfo(gson.toJson(RandomWeatherInfoUtil.createMockWeatherInfo()), port1, 123);
        }
        assertFalse(AggregationService.WEATHER_MAP_FOR_STORE.get(port1).isEmpty(), "Port1 cache should not be empty");

        Thread.sleep(20000); // Wait for 20 seconds

        for (int i = 1; i < 10; i++) {
            aggregationService.saveOrUpdateWeatherInfo(gson.toJson(RandomWeatherInfoUtil.createMockWeatherInfo()), port2, 123);
        }
        assertFalse(AggregationService.WEATHER_MAP_FOR_STORE.get(port2).isEmpty(), "Port2 cache should not be empty");

        Thread.sleep(15000); // Wait for additional 15 seconds

        // Assert that port1 data is expired, while port2 data is still available
        assertTrue(AggregationService.WEATHER_MAP_FOR_STORE.get(port1).isEmpty(), "Port1 cache should be empty after expiration time");
        assertFalse(AggregationService.WEATHER_MAP_FOR_STORE.get(port2).isEmpty(), "Port2 cache should still contain data");
    }

    // Additional test methods following the previous file's methods:

    /**
     * Test querying weather information with an invalid ID.
     * Ensures that querying a non-existent weather ID returns an appropriate error.
     */
    @Test
    public void testQueryWeatherByInvalidId() {
        // Arrange
        String invalidId = "INVALID_ID";
        int clock = 123;

        // Act
        CommonResult result = aggregationService.queryWeatherById(clock, invalidId);

        // Assert
        assertNotNull(result, "The result should not be null");
        assertEquals(404, result.getCode(), "The response code should be 404 for a non-existent ID");
        assertEquals("weather info not found, please check", result.getMessage(), "The message should indicate that the data was not found");
    }

    /**
     * Test saving weather information with malformed JSON data.
     * Ensures that the service handles malformed JSON appropriately.
     */
    @Test
    public void testSaveWeatherInfoWithMalformedJson() {
        // Arrange
        String malformedJson = "{\"id\":\"IDS60901\",\"name\":\"Test Station\",\"air_temp\":}"; // Malformed JSON
        String port = "8080";
        int clock = 123;

        // Act
        CommonResult result = aggregationService.saveOrUpdateWeatherInfo(malformedJson, port, clock);

        // Assert
        assertNotNull(result, "The result should not be null");
        assertEquals(500, result.getCode(), "The response code should be 500 for malformed JSON");
        assertEquals("Internal server error", result.getMessage(), "The message should indicate parsing failure");
    }

    /**
     * Test handling of a save request with no content.
     * Ensures that the service handles empty input appropriately.
     */
    @Test
    public void testSaveWeatherInfoWithNoContent() {
        // Arrange
        String emptyContent = "";
        String port = "8080";
        int clock = 123;

        // Act
        CommonResult result = aggregationService.saveOrUpdateWeatherInfo(emptyContent, port, clock);

        // Assert
        assertNotNull(result, "The result should not be null");
        assertEquals(204, result.getCode(), "The response code should be 204 for no content");
        assertEquals("weather data error, please check!", result.getMessage(), "The message should indicate no content");
    }

    /**
     * Test concurrent saving of weather information.
     * Ensures that the service handles concurrent save requests correctly.
     */
    @Test
    public void testConcurrentSaveWeatherInfo() throws InterruptedException {
        int numThreads = 10;
        CountDownLatch latch = new CountDownLatch(numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < numThreads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    // Arrange
                    WeatherInfoDTO wi = RandomWeatherInfoUtil.createMockWeatherInfo();
                    wi.setId("IDS6090" + index);
                    String weatherInfoStr = gson.toJson(wi);
                    String port = "8080";
                    int clock = 123;

                    // Act
                    CommonResult result = aggregationService.saveOrUpdateWeatherInfo(weatherInfoStr, port, clock);

                    // Assert
                    assertNotNull(result, "The result should not be null");
                    assertEquals(200, result.getCode(), "The response code should be 200 for a successful save");
                    assertTrue(AggregationService.WEATHER_MAP_FOR_QUERY.containsKey(wi.getId()), "The query cache should contain the new ID after saving");
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Final assertions
        assertEquals(numThreads, AggregationService.WEATHER_MAP_FOR_QUERY.size(), "All weather data should be saved in the query cache");
    }

    /**
     * Test the Lamport clock increment on each save operation.
     * Ensures that the Lamport clock is correctly incremented with each operation.
     */
    @Test
    public void testLamportClockIncrementOnSave() {
        // Arrange
        WeatherInfoDTO wi1 = RandomWeatherInfoUtil.createMockWeatherInfo();
        WeatherInfoDTO wi2 = RandomWeatherInfoUtil.createMockWeatherInfo();
        String weatherInfoStr1 = gson.toJson(wi1);
        String weatherInfoStr2 = gson.toJson(wi2);
        String port = "8080";
        int initialClock = LamportClockUtil.getInstance().getTime();

        // Act
        CommonResult result1 = aggregationService.saveOrUpdateWeatherInfo(weatherInfoStr1, port, initialClock);
        int clockAfterFirstSave = result1.getClock();
        CommonResult result2 = aggregationService.saveOrUpdateWeatherInfo(weatherInfoStr2, port, clockAfterFirstSave);

        // Assert
        assertTrue(clockAfterFirstSave > initialClock, "Lamport clock should increment after first save");
        assertTrue(result2.getClock() > clockAfterFirstSave, "Lamport clock should increment after second save");
    }

    /**
     * Test the Lamport clock synchronization when receiving a higher clock value.
     * Ensures that the service updates its clock appropriately.
     */
    @Test
    public void testLamportClockSynchronization() {
        // Arrange
        WeatherInfoDTO wi = RandomWeatherInfoUtil.createMockWeatherInfo();
        String weatherInfoStr = gson.toJson(wi);
        String port = "8080";
        int currentClock = LamportClockUtil.getInstance().getTime();
        int higherClock = currentClock + 10;

        // Act
        CommonResult result = aggregationService.saveOrUpdateWeatherInfo(weatherInfoStr, port, higherClock);

        // Assert
        assertTrue(result.getClock() > higherClock, "Service clock should be updated to higher clock value plus one");
    }

    /**
     * Test handling of fully formatted weather data.
     * Ensures that all fields are correctly parsed and stored.
     */
    @Test
    public void testFullyFormattedWeatherData() {
        // Arrange
        String fullyFormattedData = "{"
                + "\"id\": \"IDS60901\","
                + "\"name\": \"Adelaide (West Terrace / ngayirdapira)\","
                + "\"state\": \"SA\","
                + "\"time_zone\": \"CST\","
                + "\"lat\": -34.9,"
                + "\"lon\": 138.6,"
                + "\"local_date_time\": \"15/04:00pm\","
                + "\"local_date_time_full\": \"20230715160000\","
                + "\"air_temp\": 13.3,"
                + "\"apparent_t\": 9.5,"
                + "\"cloud\": \"Partly cloudy\","
                + "\"dewpt\": 5.7,"
                + "\"press\": 1023.9,"
                + "\"rel_hum\": 60,"
                + "\"wind_dir\": \"S\","
                + "\"wind_spd_kmh\": 15,"
                + "\"wind_spd_kt\": 8"
                + "}";
        String port = "8080";
        int clock = 123;

        // Act
        CommonResult result = aggregationService.saveOrUpdateWeatherInfo(fullyFormattedData, port, clock);

        // Assert
        assertNotNull(result, "The result should not be null");
        assertEquals(200, result.getCode(), "The response code should be 200 for a successful save");

        // Verify that the data is stored correctly
        WeatherInfoDTO storedData = AggregationService.WEATHER_MAP_FOR_QUERY.get("IDS60901");
        assertNotNull(storedData, "Stored data should not be null");
        assertEquals("Adelaide (West Terrace / ngayirdapira)", storedData.getName(), "Station name should match");
        assertEquals("SA", storedData.getState(), "State should match");
        // Continue asserting other fields as needed
    }

    /**
     * Test data eviction when exceeding capacity.
     * Ensures that the oldest data is removed when capacity is exceeded.
     */
    @Test
    public void testDataEvictionOnCapacityExceeded() throws IllegalAccessException {
        int capacity = 20; // Assuming capacity is 20
        String port = "8080";
        int clock = 123;

        // Insert capacity + 5 entries to exceed capacity
        for (int i = 0; i < capacity + 5; i++) {
            WeatherInfoDTO wi = RandomWeatherInfoUtil.createMockWeatherInfo("IDS" + i);
            String weatherInfoStr = gson.toJson(wi);
            aggregationService.saveOrUpdateWeatherInfo(weatherInfoStr, port, clock);
        }

        // Assert
        assertEquals(capacity, AggregationService.WEATHER_MAP_FOR_QUERY.size(), "Cache size should not exceed capacity");
        // Optionally, verify that the oldest entries are removed
        for (int i = 0; i < 5; i++) {
            assertFalse(AggregationService.WEATHER_MAP_FOR_QUERY.containsKey("IDS" + i), "Oldest data should have been evicted");
        }
    }

    /**
     * Test the service's failure tolerance by simulating exceptions during data saving.
     * Ensures that the service handles exceptions gracefully.
     */
    @Test
    public void testFailureToleranceDuringSave() {
        // Arrange
        WeatherInfoDTO wi = RandomWeatherInfoUtil.createMockWeatherInfo();
        String weatherInfoStr = gson.toJson(wi);
        String port = null; // Simulate a null port to cause an exception
        int clock = 123;

        // Act
        CommonResult result = aggregationService.saveOrUpdateWeatherInfo(weatherInfoStr, port, clock);

        // Assert
        assertNotNull(result, "The result should not be null");
        assertEquals(204, result.getCode(), "The response code should be 500 for an exception during save");
        assertEquals("weather data error, please check!", result.getMessage(), "The message should indicate an error occurred");
    }

    /**
     * Test that the service can handle high concurrency without data loss or corruption.
     * Simulates multiple threads querying and updating data simultaneously.
     */
    @Test
    public void testHighConcurrency() throws InterruptedException {
        int numThreads = 50;
        CountDownLatch latch = new CountDownLatch(numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        String port = "8080";

        for (int i = 0; i < numThreads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    // Alternate between save and query operations
                    if (index % 2 == 0) {
                        // Save operation
                        WeatherInfoDTO wi = RandomWeatherInfoUtil.createMockWeatherInfo();
                        wi.setId("IDS" + index);
                        String weatherInfoStr = gson.toJson(wi);
                        aggregationService.saveOrUpdateWeatherInfo(weatherInfoStr, port, LamportClockUtil.getInstance().getTime());
                    } else {
                        // Query operation
                        aggregationService.queryWeatherById(LamportClockUtil.getInstance().getTime(), "IDS" + (index - 1));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        assertTrue(AggregationService.WEATHER_MAP_FOR_QUERY.size() > 0, "Cache should contain data after concurrent operations");
    }

    // Existing test methods...

}
