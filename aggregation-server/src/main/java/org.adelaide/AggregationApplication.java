package org.adelaide;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;


@SpringBootApplication
public class AggregationApplication {

    public static void main(String[] args) {
//        SpringApplication.run(AggregationApplication.class, args);
        new SpringApplicationBuilder(AggregationApplication.class)
//                .properties("server.port=4567")  // Specify the port
                .run(args);
    }
}