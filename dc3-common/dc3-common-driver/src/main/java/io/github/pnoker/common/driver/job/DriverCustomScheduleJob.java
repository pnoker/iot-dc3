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

import lombok.RequiredArgsConstructor;
import io.github.pnoker.common.driver.service.DriverCustomService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

/**
 * Quartz job that invokes the custom scheduled task implemented by the driver.
 * Marked {@link DisallowConcurrentExecution} so a slow custom job cannot stack
 * overlapping invocations against the same driver state.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class DriverCustomScheduleJob extends QuartzJobBean {

    private final DriverCustomService driverCustomService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        try {
            driverCustomService.schedule();
        } catch (Exception e) {
            log.error("Failed to execute custom schedule job", e);
        }
    }

}
