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

import lombok.RequiredArgsConstructor;
import io.github.pnoker.common.manager.biz.ScheduleForManagerService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Manager Initialization Runner for DC3 IoT Platform. This class is responsible for
 * initializing manager-related components and services during application startup. It
 * configures component scanning for manager packages and sets up MyBatis mapper scanning.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@AutoConfiguration
@ComponentScan(basePackages = {"io.github.pnoker.common.manager"})
@MapperScan(basePackages = {"io.github.pnoker.common.manager.mapper"})
@RequiredArgsConstructor
public class ManagerInitRunner implements ApplicationRunner {

    private final ScheduleForManagerService scheduleForManagerService;

    /**
     * Constructs a new ManagerInitRunner with the required service dependency.
     *
     * @param scheduleForManagerService The service responsible for manager scheduling
     *                                  operations
     */
    /**
     * Executes the initialization process when the application starts. This method
     * initializes the schedule manager service to set up necessary scheduling
     * configurations and tasks.
     *
     * @param args Application arguments passed during startup
     * @throws Exception If an error occurs during initialization
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        scheduleForManagerService.initial();
    }

}
