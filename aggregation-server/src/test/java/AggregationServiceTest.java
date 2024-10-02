import com.google.gson.Gson;
import org.adelaide.AggregationApplication;
import org.adelaide.dto.CommonResult;
import org.adelaide.dto.WeatherInfoDTO;
import org.adelaide.dto.WeatherInfoWrapperDTO;
import org.adelaide.service.AggregationService;
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

import static org.junit.jupiter.api.Assertions.*;

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
        System.out.println("result.getClock() == " + result.getClock());

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
        System.out.println("result2.getClock() == " + result2.getClock());

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
     * Test exceeding cache capacity.
     * This test ensures that when the cache reaches its capacity, the oldest entries are removed.
     */
    @Test
    public void testCapacity() {
        int capacity = 20;
        WeatherInfoDTO first = RandomWeatherInfoUtil.createMockWeatherInfo();
        String firstId = first.getId();
        System.out.println("firstId === " + firstId);
        aggregationService.saveOrUpdateWeatherInfo(gson.toJson(first), "abcd", 123);

        // Insert double the capacity to force the eviction of old entries
        for (int i = 1; i < capacity * 2; i++) {
            aggregationService.saveOrUpdateWeatherInfo(gson.toJson(RandomWeatherInfoUtil.createMockWeatherInfo()), "abcd", 123);
        }

        // Assert
        System.out.println(AggregationService.WEATHER_MAP_FOR_QUERY.keySet().size());
        assertFalse(AggregationService.WEATHER_MAP_FOR_QUERY.containsKey(firstId), "The first inserted data should be removed when the capacity is exceeded");
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

        Thread.sleep(20000);

        for (int i = 1; i < 10; i++) {
            aggregationService.saveOrUpdateWeatherInfo(gson.toJson(RandomWeatherInfoUtil.createMockWeatherInfo()), port2, 123);
        }
        assertFalse(AggregationService.WEATHER_MAP_FOR_STORE.get(port2).isEmpty(), "Port2 cache should not be empty");

        Thread.sleep(15000);

        // Assert that port1 data is expired, while port2 data is still available
        assertTrue(AggregationService.WEATHER_MAP_FOR_STORE.get(port1).isEmpty(), "Port1 cache should be empty after expiration time");
        assertFalse(AggregationService.WEATHER_MAP_FOR_STORE.get(port2).isEmpty(), "Port2 cache should still contain data");
    }
}
