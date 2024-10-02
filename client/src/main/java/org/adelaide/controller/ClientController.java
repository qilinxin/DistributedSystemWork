package org.adelaide.controller;

import org.adelaide.dto.CommonResult;
import org.adelaide.util.LamportClockUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping("/Client")
public class ClientController {

    private final RestTemplate restTemplate = new RestTemplate();

    private final static String AggUrl = "http://localhost:4567/Agg";

    // Use LamportClockUtil to manage the Lamport clock
    private final LamportClockUtil lamportClock = LamportClockUtil.getInstance();

    /**
     * Sends a GET request to the aggregation server to check connectivity.
     * The Lamport clock is incremented before sending the request.
     *
     * @return A response from the aggregation server, including the current Lamport clock value.
     */
    @GetMapping("/checkClientToAgg")
    @ResponseBody
    public String checkClientToAgg() {
        String url = AggUrl + "/hello";

        // Increment the local clock before sending the request
        int currentClock = lamportClock.sendEvent();
        System.out.println("Sending request with Lamport Clock: " + currentClock);

        // Send the request and return the result
        String result = restTemplate.getForObject(url + "?clock=" + currentClock, String.class);
        return "client======" + result + " (Lamport Clock: " + currentClock + ")";
    }

    /**
     * Queries weather information by ID.
     * The Lamport clock is incremented before each query.
     * Upon receiving a response, the local clock is synchronized based on the response's clock value.
     *
     * @param id The ID of the weather information to query.
     * @return The weather information and the status of the query.
     */
    @GetMapping("/queryWeatherById")
    @ResponseBody
    @Retryable(value = { RuntimeException.class }, maxAttempts = 3, backoff = @Backoff(delay = 5000))
    public CommonResult queryWeatherById(String id) {
        // Increment the local clock before each query
        int currentClock = lamportClock.sendEvent();
        System.out.println("Sending query with Lamport Clock: " + currentClock);

        String url = AggUrl + "/queryWeatherById?id=" + id + "&clock=" + currentClock;

        // Get the response from the remote service
        CommonResult response = restTemplate.getForObject(url, CommonResult.class);

        // Synchronize the clock upon receiving the response
        if (response != null && response.getClock() > currentClock) {
            int receivedClock = response.getClock();
            lamportClock.receiveEvent(receivedClock);  // Update the local clock
            System.out.println("Updated local Lamport Clock to: " + lamportClock.getTime());
        }

        return response;
    }
}
