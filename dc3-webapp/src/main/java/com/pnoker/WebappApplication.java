package com.pnoker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @author: Pnoker
 * @email : pnokers@gmail.com
 */
@EnableAsync
@EnableEurekaClient
@SpringBootApplication
@ComponentScan("com.pnoker.config")
@EnableFeignClients(basePackages = {"com.pnoker.service"})
public class WebappApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebappApplication.class, args);
    }
}
