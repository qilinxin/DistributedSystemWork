package org.adelaide.controller;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.atomic.AtomicInteger;

@Controller
@RequestMapping("/Context") // 定义类级别的路径
public class ContextController {

    private final RestTemplate restTemplate = new RestTemplate();

    private final static String AggUrl = "http://localhost:4567/Agg";

    private final static AtomicInteger clock = new AtomicInteger(0);


    @GetMapping("/checkClientToAgg")
    @ResponseBody  // 表示直接返回响应体
    public String checkClientToAgg() {
        String url = AggUrl + "/hello";
        return "client======" + restTemplate.getForObject(url, String.class);
    }

    @PostMapping("/updateWeatherInfo")
    @ResponseBody  // 表示直接返回响应体
    public String updateWeatherInfo(@RequestBody String weatherData) {
        String url = AggUrl + "/updateWeatherInfo";

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 请求体 JSON 数据
        System.out.println("weatherData ==="+weatherData);
        // 创建 HttpEntity 传递请求头和请求体
        HttpEntity<String> request = new HttpEntity<>(weatherData, headers);

        // 发送 POST 请求
        String response = restTemplate.postForObject(url + "?clock=" + 10, request, String.class);
        // 输出响应
        System.out.println("Response: " + response);

        return "client======" + restTemplate.getForObject(url, String.class);
    }


}