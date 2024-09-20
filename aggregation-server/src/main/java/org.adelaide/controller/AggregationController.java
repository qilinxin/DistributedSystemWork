package org.adelaide.controller;

import org.adelaide.dto.WeatherInfoDTO;
import org.adelaide.service.AggregationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/Agg") // 定义类级别的路径
public class AggregationController {

    @Autowired
    AggregationService aggregationService;

    @GetMapping("/hello")
    @ResponseBody  // 表示直接返回响应体
    public String sayHello() {
        return "Hello from Aggregation Server!";  // 返回字符串作为响应
    }

    @GetMapping("/queryWeatherById")
    @ResponseBody
    public String queryWeatherById(String id) {
        return aggregationService.queryWeatherById(id);
    }

    @PostMapping("/updateWeatherById")
    @ResponseBody
    public String updateWeatherById(WeatherInfoDTO weatherInfoDTO, int clock) {
        return aggregationService.updateWeatherInfo();
    }
}
