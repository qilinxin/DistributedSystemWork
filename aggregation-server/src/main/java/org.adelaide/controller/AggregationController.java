package org.adelaide.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.adelaide.dto.CommonResult;
import org.adelaide.dto.WeatherInfoWrapperDTO;
import org.adelaide.service.AggregationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AggregationController is responsible for handling HTTP requests for managing weather information.
 * It provides endpoints for querying weather data, updating data, and monitoring the system status.
 */
@Controller
@RequestMapping("/Agg") // Defines the base path for all endpoints in this controller
public class AggregationController {

    @Autowired
    AggregationService aggregationService;

    @Autowired
    private HttpServletRequest request;

    /**
     * A simple endpoint to test the connectivity with the Aggregation Server.
     *
     * @return A {@code String} message indicating successful connection.
     *
     * Example Request:
     * GET /Agg/hello
     *
     * Example Response:
     * "Hello from Aggregation Server!"
     */
    @GetMapping("/hello")
    @ResponseBody  // Indicates that the return value should be used as the response body
    public String sayHello() {
        return "Hello from Aggregation Server!";  // Returns a simple greeting string as a response
    }

    /**
     * Queries the weather information by a given weather ID.
     * The request also includes a Lamport clock value to ensure consistency in distributed environments.
     *
     * @param clock the Lamport clock value provided by the client to handle distributed event timing (default is 0)
     * @param id    the unique identifier for the weather record to be queried
     * @return      a {@code CommonResult} object containing either the weather information or an error message if not found
     *
     * Usage:
     * This endpoint is used to retrieve specific weather information by ID, allowing clients to see real-time or cached data.
     * The Lamport clock helps in maintaining a consistent logical time between the client and the server.
     *
     * Example Request:
     * GET /Agg/queryWeatherById?clock=123&id=IDS60999
     *
     * Example Response:
     * {
     *   "code": 200,
     *   "message": "",
     *   "data": {
     *     "id": "IDS60999",
     *     "name": "Adelaide (West Terrace / ngayirdapira)",
     *     "state": "SA",
     *     "time_zone": "CST",
     *     "lat": -34.9,
     *     "lon": 138.6,
     *     "local_date_time": "15/04:00pm",
     *     "local_date_time_full": "20230715160000",
     *     "air_temp": 13.3,
     *     "apparent_t": 9.5,
     *     "cloud": "Partly cloudy",
     *     "dewpt": 5.7,
     *     "press": 1023.9,
     *     "rel_hum": 60,
     *     "wind_dir": "S",
     *     "wind_spd_kmh": 15,
     *     "wind_spd_kt": 8
     *   },
     *   "clock": 124
     * }
     */
    @GetMapping("/queryWeatherById")
    @ResponseBody
    public CommonResult queryWeatherById(@RequestParam(defaultValue = "0") int clock, @RequestParam(required = false) String id) {
        return aggregationService.queryWeatherById(clock, id);
    }

    /**
     * Creates or updates weather information. The method accepts a JSON string containing weather details
     * and a Lamport clock value to synchronize the logical time.
     *
     * @param weatherInfoStr the JSON string representing the weather information to be created or updated
     * @param clock          the Lamport clock value to track the update event
     * @return               a {@code CommonResult} indicating the result of the update operation, including success or failure
     *
     * Usage:
     * This endpoint is used to create new weather records or update existing ones.
     * The Lamport clock is incremented to maintain proper ordering of events in a distributed system.
     *
     * Example Request:
     * POST /Agg/saveOrUpdateWeatherInfo?clock=123
     *
     * Request Body (JSON):
     * {
     *   "id": "IDS60999",
     *   "name": "Adelaide (West Terrace / ngayirdapira)",
     *   "state": "SA",
     *   "time_zone": "CST",
     *   "lat": -34.9,
     *   "lon": 138.6,
     *   "local_date_time": "15/04:00pm",
     *   "local_date_time_full": "20230715160000",
     *   "air_temp": 13.3,
     *   "apparent_t": 9.5,
     *   "cloud": "Partly cloudy",
     *   "dewpt": 5.7,
     *   "press": 1023.9,
     *   "rel_hum": 60,
     *   "wind_dir": "S",
     *   "wind_spd_kmh": 15,
     *   "wind_spd_kt": 8
     * }
     *
     * Example Response:
     * {
     *   "code": 200,
     *   "message": "Weather information updated successfully.",
     *   "clock": 124
     * }
     */
    @PostMapping("/saveOrUpdateWeatherInfo")
    @ResponseBody
    public CommonResult saveOrUpdateWeatherInfo(@RequestBody String weatherInfoStr, @RequestParam int clock) {
        String port = request.getHeader("Client-Port");
        System.out.println("server: " + port);
        return aggregationService.saveOrUpdateWeatherInfo(weatherInfoStr, port, clock);
    }

    /**
     * Retrieves all current cached weather information in the system.
     *
     * @return a {@code Map<String, WeatherInfoWrapperDTO>} containing all weather information, where the key is the weather ID and the value is a {@code WeatherInfoWrapperDTO}
     *
     * Usage:
     * This endpoint allows clients to access all current weather data stored in the system cache.
     * It is helpful for clients that need to retrieve multiple records at once, for monitoring or bulk operations.
     *
     * Example Request:
     * GET /Agg/queryCacheInfo
     *
     * Example Response:
     * {
     *   "IDS60999": {
     *     "weatherInfo": {
     *       "id": "IDS60999",
     *       "name": "Adelaide (West Terrace / ngayirdapira)",
     *       "state": "SA",
     *       "time_zone": "CST",
     *       "lat": -34.9,
     *       "lon": 138.6,
     *       "local_date_time": "15/04:00pm",
     *       "local_date_time_full": "20230715160000",
     *       "air_temp": 13.3,
     *       "apparent_t": 9.5,
     *       "cloud": "Partly cloudy",
     *       "dewpt": 5.7,
     *       "press": 1023.9,
     *       "rel_hum": 60,
     *       "wind_dir": "S",
     *       "wind_spd_kmh": 15,
     *       "wind_spd_kt": 8
     *     },
     *     "lastUpdateTime": 1695890400000
     *   }
     * }
     */
    @GetMapping("/queryCacheInfo")
    @ResponseBody
    public Map<String, Map<String, WeatherInfoWrapperDTO>> queryCacheInfo() {
        return aggregationService.queryCacheInfo();
    }
}
