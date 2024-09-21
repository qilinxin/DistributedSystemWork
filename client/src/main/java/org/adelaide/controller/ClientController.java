package org.adelaide.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.atomic.AtomicInteger;

@Controller
@RequestMapping("/Client") // 定义类级别的路径
public class ClientController {

    private final RestTemplate restTemplate = new RestTemplate();

    private final static String AggUrl = "http://localhost:4567/Agg";

    private final static AtomicInteger clock = new AtomicInteger(0);

    @GetMapping("/checkClientToAgg")
    @ResponseBody  // 表示直接返回响应体
    public String checkClientToAgg() {
        String url = AggUrl + "/hello";
        return "client======" + restTemplate.getForObject(url, String.class);
    }

    @GetMapping("/queryWeatherById")
    @ResponseBody
    public String queryWeatherById(String id) {
         String url = AggUrl + "/queryWeatherById?id=" + id + "&clock=" + clock;
        return restTemplate.getForObject(url, String.class);
    }

}
