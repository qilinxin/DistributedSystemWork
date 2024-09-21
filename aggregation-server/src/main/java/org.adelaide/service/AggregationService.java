package org.adelaide.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import jakarta.annotation.PostConstruct;
import org.adelaide.dto.CommonResult;
import org.adelaide.dto.WeatherInfoDTO;
import org.adelaide.dto.WeatherInfoWrapperDTO;
import org.adelaide.util.LamportClockUtil;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class AggregationService {

    private static final String PROJECT_ROOT_PATH = System.getProperty("user.dir");
    private static Map<String, WeatherInfoWrapperDTO> WEATHER_MAP = new HashMap<>();
    private final Gson gson = new Gson();

    private static boolean newFileFlag = false;

    // LamportClockUtil 实例，用于处理 Lamport Clock
    private final LamportClockUtil lamportClock = new LamportClockUtil();

    // 创建 ScheduledExecutorService，用于定时任务
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // 启动定时任务，监听 WEATHER_MAP 的数据
    @PostConstruct
    public void startWeatherMapListener() {
        // 每隔 10 秒检查一次 WEATHER_MAP 中的数据
        scheduler.scheduleAtFixedRate(this::monitorWeatherMap, 0, 5, TimeUnit.SECONDS);
    }

    // 定时监听 WEATHER_MAP 中的变化
    private void monitorWeatherMap() {
        System.out.println("Monitoring WEATHER_MAP. Current size: " + WEATHER_MAP.size());
        int delCnt = 0;
        // 遍历并检查 WEATHER_MAP 中的数据
        Iterator<Map.Entry<String, WeatherInfoWrapperDTO>> iterator = WEATHER_MAP.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, WeatherInfoWrapperDTO> entry = iterator.next();
            if (System.currentTimeMillis() - entry.getValue().getLastUpdateTime() > 30000) {
                System.out.println("WeatherInfoDTO: " + entry.getValue().getWeatherInfo().getId() + " removed by outdated ");
                iterator.remove(); // 安全地删除元素
                delCnt += 1;
            }
        }
        if (delCnt > 0) {
            try {
                updateFileInfo();
            } catch (IOException e) {
                //no message or monitor,so just print error info
                e.printStackTrace();
            }
        }
    }

    public CommonResult queryWeatherById(int clock, String id) {
        // 处理接收到的时间事件
        lamportClock.receiveEvent(clock);

        // 输出当前时钟值和请求的 ID
        System.out.println("Clock + id ==" + lamportClock.getTime() + ";;" + id);

        WeatherInfoWrapperDTO res = WEATHER_MAP.get(id);
        if (res == null || res.getWeatherInfo() == null) {
            return new CommonResult(500, "weather info not found, please check");
        }
        return new CommonResult(200,"", WEATHER_MAP.get(id).getWeatherInfo(), lamportClock.getTime());
    }

    // 使用 @PostConstruct 注解，在 Spring 容器启动完成后执行该方法
    @PostConstruct
    public void loadWeatherData() {
        String projectRootPath = System.getProperty("user.dir");
        String filePath = Paths.get(projectRootPath, "weatherInfoMap.json").toString();

        try (FileReader reader = new FileReader(filePath)) {
            Type weatherMapType = new TypeToken<Map<String, WeatherInfoDTO>>() {}.getType();
            WEATHER_MAP = gson.fromJson(reader, weatherMapType);

            if (WEATHER_MAP != null) {
                System.out.println("成功加载 weatherInfoMap，共 " + WEATHER_MAP.size() + " 条记录。");
            } else {
                System.out.println("weatherInfoMap.json 文件内容无效，初始化空的 WEATHER_MAP。");
                WEATHER_MAP = new HashMap<>();
                newFileFlag = true;
            }
        } catch (FileNotFoundException e) {
            // 文件不存在时，初始化一个新的空 map
            System.out.println("文件未找到，创建一个新的空的 WEATHER_MAP。");
            WEATHER_MAP = new HashMap<>();
            newFileFlag = true;
        } catch (IOException e) {
            // 捕获其他 IO 错误
            System.out.println("未能加载 weatherInfoMap.json 文件，可能文件不存在或读取失败。");
            e.printStackTrace();  // 打印详细错误堆栈
            newFileFlag = true;
        }
    }

    public CommonResult updateWeatherInfo(String weatherInfoStr, int clock) {
        // 处理接收到的时间事件
        lamportClock.receiveEvent(clock);

        WeatherInfoDTO weatherInfo = gson.fromJson(weatherInfoStr, WeatherInfoDTO.class);
        if (weatherInfo == null || weatherInfo.getId() == null) {
            return new CommonResult(204, "weather data error, please check!");
        }

        // 记录更新事件时递增 Lamport Clock
        int currentClock = lamportClock.increment();
        System.out.println("Updating weather info with Lamport clock: " + currentClock);
        int code = newFileFlag ? 201 : 200;
        newFileFlag = false;
        WEATHER_MAP.put(weatherInfo.getId(), new WeatherInfoWrapperDTO(weatherInfo));
        try {
            this.updateFileInfo();
        }catch (IOException e) {
            code = 500;
        }
        return new CommonResult(code, clock);
    }

    public Map<String, WeatherInfoWrapperDTO> queryCacheInfo() {
        return WEATHER_MAP;
    }

    private synchronized void updateFileInfo() throws IOException {

        String filePath = Paths.get(PROJECT_ROOT_PATH, "weatherInfoMap.json").toString();

        // 使用 try-with-resources 确保 FileWriter 被自动关闭
        try (FileWriter writer = new FileWriter(filePath)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(WEATHER_MAP, writer);
            System.out.println("weatherInfoMap.json is updated");
        }
    }
}
