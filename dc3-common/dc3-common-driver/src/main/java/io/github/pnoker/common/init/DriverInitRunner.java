/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
