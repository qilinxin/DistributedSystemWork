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
@RequestMapping("/Content") // 定义类级别的路径
public class ContentController {

    private final RestTemplate restTemplate = new RestTemplate();

    private final static String AggUrl = "http://localhost:4567/Agg";

    // Lamport Clock 的 AtomicInteger
    private final static AtomicInteger clock = new AtomicInteger(0);

    // 发送一个简单的 GET 请求，输出当前 Lamport Clock
    @GetMapping("/checkClientToAgg")
    @ResponseBody  // 表示直接返回响应体
    public String checkClientToAgg() {
        String url = AggUrl + "/hello";

        // 每次请求发送之前递增本地时钟
        int currentClock = clock.incrementAndGet();
        System.out.println("Sending request with Lamport Clock: " + currentClock);

        // 发出请求并返回结果
        String result = restTemplate.getForObject(url, String.class);
        return "client======" + result + " (Lamport Clock: " + currentClock + ")";
    }

    // 更新天气信息，同时同步 Lamport Clock
    @PutMapping("/updateWeatherInfo")
    @ResponseBody  // 表示直接返回响应体
    public CommonResult updateWeatherInfo(@RequestBody String weatherData) {
        String url = AggUrl + "/updateWeatherInfo";

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 每次更新时先递增 Lamport Clock
        int currentClock = clock.incrementAndGet();
        System.out.println("Sending update request with Lamport Clock: " + currentClock);

        // 请求体 JSON 数据
        System.out.println("weatherData ===" + weatherData);

        // 创建 HttpEntity 传递请求头和请求体
        HttpEntity<String> request = new HttpEntity<>(weatherData, headers);

        // 发送 POST 请求，同时传递 clock 参数
        CommonResult response = restTemplate.postForObject(url + "?clock=" + currentClock, request, CommonResult.class);

        // 在接收响应后，更新本地 Lamport Clock
        if (response != null && response.getClock() > currentClock) {
            int receivedClock = response.getClock();
            // 更新本地时钟为 max(本地时钟, 接收到的时钟) + 1
            clock.updateAndGet(localClock -> Math.max(localClock, receivedClock) + 1);
            System.out.println("Updated local Lamport Clock to: " + clock.get());
        }

        // 输出响应
        System.out.println("Response: " + response);

        return response;
    }
}
