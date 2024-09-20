package main.java;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // 标注为控制器，并且每个方法都会自动序列化为 JSON 响应
public class AggregationServer {

    @GetMapping("/hello") // 映射 HTTP GET 请求到 /hello 路径
    public String sayHello() {
        return "Hello, World!";
    }
}