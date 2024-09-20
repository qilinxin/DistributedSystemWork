package org.adelaide.service;

import org.adelaide.dto.WeatherInfoDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AggregationService {

    Map<String, WeatherInfoDTO> wiList = new HashMap<>();

    public String queryWeatherById(String id) {
        if (StringUtils.isEmpty(id)) {
            return "invalid id";
        }
        return "aggregationService.queryWeatherById(id);";
    }


    public void loadWeatherInfoFromFile() {

    }

    public String updateWeatherInfo() {
        return "1";
    }



}
