package com.pnoker.common.sdk.init;

import com.pnoker.common.sdk.service.DriverService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 初始化
 *
 * @author pnoker
 */
@Component
@EnableFeignClients(basePackages = {
        "com.pnoker.api.center.device.*"
})
@ComponentScan(basePackages = {
        "com.pnoker.api.center.device",
        "com.pnoker.common.sdk"
})
public class DriverRunner implements CommandLineRunner {

    @Value("${driver.name}")
    private String name;
    @Value("${spring.application.name}")
    private String serviceName;
    @Value("${driver.description}")
    private String description;

    @Resource
    private ApplicationContext context;
    @Resource
    private DriverService driverService;

    @Override
    public void run(String... args) throws Exception {
        boolean register = driverService.register(name, serviceName, description);
        if (!register) {
            ((ConfigurableApplicationContext) context).close();
        }
    }
}
