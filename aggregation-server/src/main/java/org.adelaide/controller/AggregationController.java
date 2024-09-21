package org.adelaide.controller;

import org.adelaide.dto.WeatherInfoDTO;
import org.adelaide.service.AggregationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    public Object queryWeatherById(@RequestParam(defaultValue = "0") int clock, @RequestParam(required = false) String id) {
        return aggregationService.queryWeatherById(clock, id);
    }

    @PostMapping("/updateWeatherInfo")
    @ResponseBody
    public String updateWeatherInfo(@RequestBody String weatherInfoStr,@RequestParam int clock) {
        return aggregationService.updateWeatherInfo(weatherInfoStr, clock);
    }

    @GetMapping("/queryCacheInfo")
    @ResponseBody
    public Map<String, WeatherInfoDTO> queryCacheInfo() {
        return aggregationService.queryCacheInfo();
    }
}
