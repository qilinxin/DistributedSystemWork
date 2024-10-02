package org.adelaide;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class ClientApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(ClientApplication.class)
                .properties("server.port=6789")  // Specify the port
                .run(args);
    }
}