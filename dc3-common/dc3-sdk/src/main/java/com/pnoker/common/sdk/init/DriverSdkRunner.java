package com.pnoker.common.sdk.init;

import com.pnoker.common.sdk.service.DriverCustomizersService;
import com.pnoker.common.sdk.service.DriverSdkService;
import com.pnoker.common.sdk.service.QuartzService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
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
        "com.pnoker.api.center.manager.*"
})
@ComponentScan(basePackages = {
        "com.pnoker.api.center.manager",
        "com.pnoker.common.sdk"
})
public class DriverSdkRunner implements CommandLineRunner {

    @Resource
    private ApplicationContext context;
    @Resource
    private DeviceDriver deviceDriver;
    @Resource
    private DriverSdkService service;
    @Resource
    private DriverCustomizersService customizers;
    @Resource
    private QuartzService quartzService;

    @Override
    public void run(String... args) throws Exception {
        service.initial(context, deviceDriver);
        customizers.initial(deviceDriver);
        quartzService.initial();
    }
}
