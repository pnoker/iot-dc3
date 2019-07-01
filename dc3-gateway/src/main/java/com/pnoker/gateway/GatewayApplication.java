package com.pnoker.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: IOT DC3 平台网关,用于限流、权限验证、服务转发、方向代理 等。
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
