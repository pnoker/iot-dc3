package com.pnoker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * @author: Pnoker
 * @email : pnokers@gmail.com
 */
@EnableEurekaServer
@SpringBootApplication
public class Dc3ServiceCenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(Dc3ServiceCenterApplication.class, args);
    }
}
