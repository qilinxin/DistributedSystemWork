package org.adelaide;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.retry.annotation.EnableRetry;

import java.util.HashMap;
import java.util.Map;
@SpringBootApplication
@EnableRetry
public class ContentApplication {

    public static void main(String[] args) {
        // Disable JMX before any application instances are started
        System.setProperty("spring.jmx.enabled", "false");

        // Start the first instance on port 8081
        ConfigurableApplicationContext instance1 = startInstance(8081, "instance1");

        // Start the second instance on port 8082
        ConfigurableApplicationContext instance2 = startInstance(8082, "instance2");

        // Start the third instance on port 8083
        ConfigurableApplicationContext instance3 = startInstance(8083, "instance3");

        // You can continue to start other instances here
    }

    private static ConfigurableApplicationContext startInstance(int port, String instanceName) {
        SpringApplication app = new SpringApplication(ContentApplication.class);

        // Set different ports and instance names
        Map<String, Object> properties = new HashMap<>();
        properties.put("server.port", port);
        properties.put("spring.application.name", instanceName);
        properties.put("spring.jmx.default-domain", "application-" + instanceName);
        properties.put("spring.application.admin.jmx-name", "org.springframework.boot:type=Admin" + port + ",name=SpringApplication-" + instanceName);

        // No need to set JMX properties here since it's already disabled globally

        app.setDefaultProperties(properties);

        // Start the instance
        return app.run();
    }
}