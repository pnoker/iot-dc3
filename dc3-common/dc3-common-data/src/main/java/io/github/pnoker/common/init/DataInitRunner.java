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

import io.github.pnoker.common.data.biz.ScheduleForDataService;
import io.github.pnoker.common.data.entity.property.AlarmCacheProperties;
import io.github.pnoker.common.data.entity.property.AlarmWindowProperties;
import io.github.pnoker.common.data.entity.property.NotifyCredentialProperties;
import io.github.pnoker.common.data.entity.property.PointBatchProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * Data Initialization Runner for DC3 IoT Platform. This class handles data initialization
 * tasks during application startup, configuring component scanning for data-related
 * classes and MyBatis mappers.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@AutoConfiguration
@ComponentScan(basePackages = {"io.github.pnoker.common.data"})
@MapperScan(basePackages = {"io.github.pnoker.common.data.mapper"})
@EnableConfigurationProperties({PointBatchProperties.class, NotifyCredentialProperties.class,
        AlarmCacheProperties.class, AlarmWindowProperties.class})
public class DataInitRunner implements ApplicationRunner {

    private final ScheduleForDataService scheduleForDataService;

    /**
     * Constructor for DataInitRunner
     *
     * @param scheduleForDataService Service for handling data scheduling operations
     */
    public DataInitRunner(ScheduleForDataService scheduleForDataService) {
        this.scheduleForDataService = scheduleForDataService;
    }

    /**
     * Executes the data initialization process when the application starts
     *
     * @param args Application arguments passed during startup
     * @throws Exception If an error occurs during initialization
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        scheduleForDataService.initial();
    }

}
