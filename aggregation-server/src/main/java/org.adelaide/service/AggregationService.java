package org.adelaide.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import jakarta.annotation.PostConstruct;
import org.adelaide.dto.WeatherInfoDTO;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class AggregationService {

    private static final String PROJECT_ROOT_PATH = System.getProperty("user.dir");

    private static Map<String, WeatherInfoDTO> WEATHER_MAP = new HashMap<>();

    Gson gson = new Gson();


    public Object queryWeatherById(int clock, String id) {
        System.out.println("clock + id ==" + clock + ";;" + id);
//        if (StringUtils.isEmpty(id)) {
//            return "invalid id";
//        }
        WeatherInfoDTO res = queryWeatherFromCache(id);
        if (res == null) {
            return "no weather found";
        }
        return res;
    }


    // 使用 @PostConstruct 注解，在 Spring 容器启动完成后执行该方法
    @PostConstruct
    public void loadWeatherData() {
        // 获取项目根目录路径
        String projectRootPath = System.getProperty("user.dir");
        // 定义文件路径为项目根目录下
        String filePath = Paths.get(projectRootPath, "weatherInfoMap.json").toString();

        // 读取 JSON 文件
        try (FileReader reader = new FileReader(filePath)) {
            Gson gson = new Gson();
            Type weatherMapType = new TypeToken<Map<String, WeatherInfoDTO>>() {}.getType();
            WEATHER_MAP = gson.fromJson(reader, weatherMapType);

            System.out.println("成功加载 weatherInfoMap，共 " + WEATHER_MAP.size() + " 条记录。");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String updateWeatherInfo(String weatherInfoStr, int clock) {
        WeatherInfoDTO weatherInfo = gson.fromJson(weatherInfoStr, WeatherInfoDTO.class);
        if (weatherInfo == null || weatherInfo.getId() == null) {
            return "weather date error, please check!";
        }
        WEATHER_MAP.put(weatherInfo.getId(), weatherInfo);
        return this.updateFileInfo();
    }

    public Map<String, WeatherInfoDTO> queryCacheInfo() {
        return WEATHER_MAP;
    }

    /*--------------------------------------------------------------------- */


    /**
     * update info to file, add synchronized to  avoid resource conflict
     * @return update result
     */
    private synchronized String updateFileInfo() {
        String filePath = Paths.get(PROJECT_ROOT_PATH, "weatherInfoMap.json").toString();

        // 写入 JSON 文件
        try (FileWriter writer = new FileWriter(filePath)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(WEATHER_MAP, writer);
            System.out.println("JSON 数据已保存到项目根目录的 weatherInfoMap.json 文件中");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "update weather info success";
    }

    private WeatherInfoDTO queryWeatherFromCache(String id) {
//        WeatherInfoDTO res = WILIST.get(id);
        String fakeInfo = "{\n" +
                "  \"id\": \"IDS60901\",\n" +
                "  \"name\": \"Adelaide (West Terrace /  ngayirdapira)\",\n" +
                "  \"state\": \"SA\",\n" +
                "  \"time_zone\": \"CST\",\n" +
                "  \"lat\": -34.9,\n" +
                "  \"lon\": 138.6,\n" +
                "  \"local_date_time\": \"15/04:00pm\",\n" +
                "  \"local_date_time_full\": \"20230715160000\",\n" +
                "  \"air_temp\": 13.3,\n" +
                "  \"apparent_t\": 9.5,\n" +
                "  \"cloud\": \"Partly cloudy\",\n" +
                "  \"dewpt\": 5.7,\n" +
                "  \"press\": 1023.9,\n" +
                "  \"rel_hum\": 60,\n" +
                "  \"wind_dir\": \"S\",\n" +
                "  \"wind_spd_kmh\": 15,\n" +
                "  \"wind_spd_kt\": 8\n" +
                "}\n";
        WeatherInfoDTO res = gson.fromJson(fakeInfo, WeatherInfoDTO.class);
        return res;
    }

}
