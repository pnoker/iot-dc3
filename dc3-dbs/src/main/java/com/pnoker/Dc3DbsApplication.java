package com.pnoker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * @author: Pnoker
 * @email : pnokers@gmail.com
 */
@EnableEurekaClient
@SpringBootApplication
public class Dc3DbsApplication {

    public static void main(String[] args) {
        SpringApplication.run(Dc3DbsApplication.class, args);
    }
}
