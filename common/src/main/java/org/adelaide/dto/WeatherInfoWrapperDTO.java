package org.adelaide.dto;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wrapper class for WeatherInfoDTO that includes a last update time field.
 * This class is used to add additional metadata (lastUpdateTime and versionId)
 * to the WeatherInfoDTO object without modifying the original class.
 */
public class WeatherInfoWrapperDTO {

    // The original WeatherInfoDTO object containing weather information
    private WeatherInfoDTO weatherInfo;

    // The timestamp representing the last time the weather information was updated
    private long lastUpdateTime;

    // The version ID for tracking updates
    private int versionId;

    /**
     * Constructs a WeatherInfoWrapperDTO with the provided WeatherInfoDTO object.
     * Initializes the lastUpdateTime field to the current system time and versionId to 0.
     *
     * @param weatherInfo the WeatherInfoDTO object to wrap
     */
    public WeatherInfoWrapperDTO(WeatherInfoDTO weatherInfo, int versionId) {
        this.weatherInfo = weatherInfo;
        this.lastUpdateTime = System.currentTimeMillis();
        this.versionId = versionId;
    }

    /**
     * Retrieves the wrapped WeatherInfoDTO object.
     *
     * @return the WeatherInfoDTO object
     */
    public WeatherInfoDTO getWeatherInfo() {
        return weatherInfo;
    }

    /**
     * Sets the wrapped WeatherInfoDTO object.
     *
     * @param weatherInfo the new WeatherInfoDTO object
     */
    public void setWeatherInfo(WeatherInfoDTO weatherInfo) {
        this.weatherInfo = weatherInfo;
    }

    /**
     * Retrieves the last update time of the WeatherInfoDTO object.
     *
     * @return the timestamp of the last update as a long value
     */
    public Long getLastUpdateTime() {
        return lastUpdateTime;
    }

    /**
     * Sets the last update time for the WeatherInfoDTO object.
     *
     * @param lastUpdateTime the new last update time as a Long value
     */
    public void setLastUpdateTime(Long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    /**
     * Retrieves the current version ID of the WeatherInfoDTO object.
     *
     * @return the version ID as an integer
     */
    public int getVersionId() {
        return versionId;
    }

    /**
     * Sets the version ID for the WeatherInfoDTO object.
     *
     * @param versionId the new version ID
     */
    public void setVersionId(int versionId) {
        this.versionId = versionId;
    }

}
