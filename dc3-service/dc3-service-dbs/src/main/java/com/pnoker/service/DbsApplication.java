package com.pnoker.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author: Pnoker
 * @email: pnokers@gmail.com
 * @project: iot-dc3
 * @copyright: Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>
 */
@EnableEurekaClient
@SpringBootApplication
@EnableTransactionManagement
public class DbsApplication {
    public static void main(String[] args) {
        SpringApplication.run(DbsApplication.class, args);
    }
}
