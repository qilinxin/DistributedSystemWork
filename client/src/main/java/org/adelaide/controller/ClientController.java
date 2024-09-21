package org.adelaide.controller;

import org.adelaide.dto.CommonResult;
import org.adelaide.util.LamportClockUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping("/Client")
public class ClientController {

    private final RestTemplate restTemplate = new RestTemplate();

    private final static String AggUrl = "http://localhost:4567/Agg";

    // 使用 LamportClockUtil 来管理 Lamport 时钟
    private final LamportClockUtil lamportClock = new LamportClockUtil();

    // GET 请求，发送前递增 Lamport 时钟
    @GetMapping("/checkClientToAgg")
    @ResponseBody
    public String checkClientToAgg() {
        String url = AggUrl + "/hello";

        // 发送请求前递增本地时钟
        int currentClock = lamportClock.sendEvent();
        System.out.println("Sending request with Lamport Clock: " + currentClock);

        // 发送请求，返回结果
        String result = restTemplate.getForObject(url + "?clock=" + currentClock, String.class);
        return "client======" + result + " (Lamport Clock: " + currentClock + ")";
    }

    // 查询天气信息，根据 ID 和时钟
    @GetMapping("/queryWeatherById")
    @ResponseBody
    public CommonResult queryWeatherById(String id) {
        // 每次查询前递增本地时钟
        int currentClock = lamportClock.sendEvent();
        System.out.println("Sending query with Lamport Clock: " + currentClock);

        String url = AggUrl + "/queryWeatherById?id=" + id + "&clock=" + currentClock;

        // 获取远程服务的响应
        CommonResult response = restTemplate.getForObject(url, CommonResult.class);

        // 在接收响应后，同步时钟
        if (response != null && response.getClock() > currentClock) {
            int receivedClock = response.getClock();
            lamportClock.receiveEvent(receivedClock);  // 更新本地时钟
            System.out.println("Updated local Lamport Clock to: " + lamportClock.getTime());
        }

        return response;
    }
}
