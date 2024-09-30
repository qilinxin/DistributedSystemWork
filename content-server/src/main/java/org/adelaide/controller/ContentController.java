package org.adelaide.controller;

import org.adelaide.dto.CommonResult;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.atomic.AtomicInteger;

@Controller
@RequestMapping("/Content")
public class ContentController {

    private final RestTemplate restTemplate = new RestTemplate();

    private final static String AggUrl = "http://localhost:4567/Agg";

    // AtomicInteger to manage the Lamport Clock
    private final static AtomicInteger clock = new AtomicInteger(0);

    /**
     * Sends a GET request to check the connection between the client and the aggregation server.
     * The Lamport clock is incremented before each request is sent.
     *
     * @return A response from the aggregation server, including the current Lamport clock value.
     */
    @GetMapping("/checkClientToAgg")
    @ResponseBody
    public String checkClientToAgg() {
        String url = AggUrl + "/hello";

        // Increment the local clock before sending the request
        int currentClock = clock.incrementAndGet();
        System.out.println("Sending request with Lamport Clock: " + currentClock);

        // Send the request and return the result
        String result = restTemplate.getForObject(url, String.class);
        return "client======" + result + " (Lamport Clock: " + currentClock + ")";
    }

    /**
     * Updates the weather information on the aggregation server.
     * The Lamport clock is incremented before sending the update request.
     * Upon receiving the response, the local Lamport clock is synchronized based on the response clock.
     *
     * @param weatherData The weather data to be updated, in JSON format.
     * @return The result of the update operation, including status and updated clock.
     */
    @PutMapping("/saveOrUpdateWeatherInfo")
    @ResponseBody
    public CommonResult saveOrUpdateWeatherInfo(@RequestBody String weatherData) {
        String url = AggUrl + "/saveOrUpdateWeatherInfo";

        // Set request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Increment the local clock before sending the update request
        int currentClock = clock.incrementAndGet();
        System.out.println("Sending update request with Lamport Clock: " + currentClock);

        // Print the weather data being sent
        System.out.println("weatherData ===" + weatherData);

        // Create HttpEntity to pass request headers and body
        HttpEntity<String> request = new HttpEntity<>(weatherData, headers);

        // Send the POST request, including the clock parameter
        CommonResult response = restTemplate.postForObject(url + "?clock=" + currentClock, request, CommonResult.class);

        // Update the local Lamport clock upon receiving the response
        if (response != null && response.getClock() > currentClock) {
            int receivedClock = response.getClock();
            // Update local clock to max(local clock, received clock) + 1
            clock.updateAndGet(localClock -> Math.max(localClock, receivedClock) + 1);
            System.out.println("Updated local Lamport Clock to: " + clock.get());
        }

        // Print the response
        System.out.println("Response: " + response);

        return response;
    }
}
