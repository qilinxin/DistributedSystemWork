package org.adelaide;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;

import java.lang.annotation.*;

@SpringBootApplication
public class AggregationApplication {

    public static void main(String[] args) {
//        SpringApplication.run(AggregationApplication.class, args);
        new SpringApplicationBuilder(AggregationApplication.class)
                .properties("server.port=4567")  // Specify the port
                .run(args);
    }
}