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
import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * Application startup runner that completes the standard driver bootstrap sequence:
 * registration, custom initialization, and schedule initialization.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@AutoConfiguration
@ComponentScan(basePackages = {"io.github.pnoker.common.driver"})
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
        // Initialize driver registration and synchronize basic information with the
        // platform
        driverRegisterService.initial();

        // Execute custom initialization functions specific to this driver module
        driverCustomService.initial();

        // Initialize driver tasks including status monitoring, reading operations and
        // custom tasks
        driverScheduleService.initial();
    }

}
