package org.adelaide.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WeatherInfoDTO {

    private String id;
    private String name;
    private String state;

    @JsonProperty("time_zone")
    private String timeZone;

    private double lat;
    private double lon;

    @JsonProperty("local_date_time")
    private String localDateTime;

    @JsonProperty("local_date_time_full")
    private String localDateTimeFull;

    @JsonProperty("air_temp")
    private double airTemp;

    @JsonProperty("apparent_t")
    private double apparentTemp;

    private String cloud;
    private double dewpt;
    private double press;

    @JsonProperty("rel_hum")
    private int relativeHumidity;

    @JsonProperty("wind_dir")
    private String windDirection;

    @JsonProperty("wind_spd_kmh")
    private int windSpeedKmh;

    @JsonProperty("wind_spd_kt")
    private int windSpeedKt;

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(String localDateTime) {
        this.localDateTime = localDateTime;
    }

    public String getLocalDateTimeFull() {
        return localDateTimeFull;
    }

    public void setLocalDateTimeFull(String localDateTimeFull) {
        this.localDateTimeFull = localDateTimeFull;
    }

    public double getAirTemp() {
        return airTemp;
    }

    public void setAirTemp(double airTemp) {
        this.airTemp = airTemp;
    }

    public double getApparentTemp() {
        return apparentTemp;
    }

    public void setApparentTemp(double apparentTemp) {
        this.apparentTemp = apparentTemp;
    }

    public String getCloud() {
        return cloud;
    }

    public void setCloud(String cloud) {
        this.cloud = cloud;
    }

    public double getDewpt() {
        return dewpt;
    }

    public void setDewpt(double dewpt) {
        this.dewpt = dewpt;
    }

    public double getPress() {
        return press;
    }

    public void setPress(double press) {
        this.press = press;
    }

    public int getRelativeHumidity() {
        return relativeHumidity;
    }

    public void setRelativeHumidity(int relativeHumidity) {
        this.relativeHumidity = relativeHumidity;
    }

    public String getWindDirection() {
        return windDirection;
    }

    public void setWindDirection(String windDirection) {
        this.windDirection = windDirection;
    }

    public int getWindSpeedKmh() {
        return windSpeedKmh;
    }

    public void setWindSpeedKmh(int windSpeedKmh) {
        this.windSpeedKmh = windSpeedKmh;
    }

    public int getWindSpeedKt() {
        return windSpeedKt;
    }

    public void setWindSpeedKt(int windSpeedKt) {
        this.windSpeedKt = windSpeedKt;
    }
}
