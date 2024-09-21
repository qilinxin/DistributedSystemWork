package org.adelaide.dto;

public class WeatherInfoWrapperDTO {
    WeatherInfoDTO weatherInfo;
    long lastUpdateTime;

    public WeatherInfoWrapperDTO(WeatherInfoDTO weatherInfo) {
        this.weatherInfo = weatherInfo;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public WeatherInfoDTO getWeatherInfo() {
        return weatherInfo;
    }

    public void setWeatherInfo(WeatherInfoDTO weatherInfo) {
        this.weatherInfo = weatherInfo;
    }

    public Long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
}
