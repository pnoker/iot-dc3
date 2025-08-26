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

package io.github.pnoker.common.driver.job;

import io.github.pnoker.common.driver.service.DriverCustomService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

/**
 * 自定义调度任务
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Component
public class DriverCustomScheduleJob extends QuartzJobBean {

    private final DriverCustomService driverCustomService;

    public DriverCustomScheduleJob(DriverCustomService driverCustomService) {
        this.driverCustomService = driverCustomService;
    }

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        try {
            driverCustomService.schedule();
        } catch (Exception e) {
            log.error("Failed to execute custom schedule job: {}", e.getMessage(), e);
        }
    }
}