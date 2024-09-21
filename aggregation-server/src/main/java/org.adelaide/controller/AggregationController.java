package org.adelaide.controller;

import org.adelaide.dto.CommonResult;
import org.adelaide.dto.WeatherInfoDTO;
import org.adelaide.dto.WeatherInfoWrapperDTO;
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

    /**
     * test connection between system
     * @return
     */
    @GetMapping("/hello")
    @ResponseBody  // 表示直接返回响应体
    public String sayHello() {
        return "Hello from Aggregation Server!";  // 返回字符串作为响应
    }

    /**
     * query weather info by id
     * @param clock lamport clock
     * @param id    weather id
     * @return  info
     */
    @GetMapping("/queryWeatherById")
    @ResponseBody
    public CommonResult queryWeatherById(@RequestParam(defaultValue = "0") int clock, @RequestParam(required = false) String id) {
        return aggregationService.queryWeatherById(clock, id);
    }

    /**
     * create or update weather info
     * @param weatherInfoStr    json string
     * @param clock             lamport clock
     * @return  update res
     */
    @PostMapping("/saveOrUpdateWeatherInfo")
    @ResponseBody
    public CommonResult saveOrUpdateWeatherInfo(@RequestBody String weatherInfoStr, @RequestParam int clock) {
        return aggregationService.saveOrUpdateWeatherInfo(weatherInfoStr, clock);
    }

    /**
     * query current living data
     * @return  all weather
     */
    @GetMapping("/queryCacheInfo")
    @ResponseBody
    public Map<String, WeatherInfoWrapperDTO> queryCacheInfo() {
        return aggregationService.queryCacheInfo();
    }
}
