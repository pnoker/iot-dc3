/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.init;

import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverRegisterService;
import io.github.pnoker.common.driver.service.DriverScheduleService;
import jakarta.annotation.Resource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

/**
 * DriverInitRunner is a Spring Boot application runner component responsible for
 * initializing and configuring driver-related services during application startup.
 * It performs the following tasks:
 * <p>
 * - Registers the driver information with the platform through the DriverRegisterService.
 * - Executes custom initialization logic for the driver via the DriverCustomService.
 * - Sets up driver scheduling tasks, including driver status, read tasks, and custom tasks
 * using the DriverScheduleService.
 * <p>
 * This class ensures that driver-related functionalities are properly initialized
 * and ready to use as part of the application lifecycle.
 */
@Component
@ComponentScan(basePackages = {
        "io.github.pnoker.common.driver.*"
})
@EnableConfigurationProperties({DriverProperties.class})
public class DriverInitRunner implements ApplicationRunner {

    @Resource
    private DriverRegisterService driverRegisterService;
    @Resource
    private DriverCustomService driverCustomService;
    @Resource
    private DriverScheduleService driverScheduleService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 驱动注册, 包括基本的信息同步
        driverRegisterService.initial();

        // 执行驱动模块的自定义初始化函数
        driverCustomService.initial();

        // 初始化驱动任务, 包括驱动状态, 读和自定义任务
        driverScheduleService.initial();
    }
}
