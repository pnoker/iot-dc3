/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pnoker.common.sdk.init;

import com.pnoker.common.sdk.quartz.service.QuartzService;
import com.pnoker.common.sdk.service.DriverCustomizersService;
import com.pnoker.common.sdk.service.DriverSdkService;
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
    private DeviceDriver driver;
    @Resource
    private DriverSdkService service;
    @Resource
    private DriverCustomizersService customizers;
    @Resource
    private QuartzService quartzService;

    @Override
    public void run(String... args) throws Exception {
        service.initial(context);
        customizers.initial(driver);
        quartzService.initial();
    }
}
