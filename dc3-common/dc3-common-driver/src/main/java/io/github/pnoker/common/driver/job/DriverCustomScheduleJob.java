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

package io.github.pnoker.common.driver.job;

import io.github.pnoker.common.driver.service.DriverCustomService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

/**
 * 自定义调度任务
 *
 * @author pnoker
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
    protected void executeInternal(@NotNull JobExecutionContext jobExecutionContext) {
        try {
            driverCustomService.schedule();
        } catch (Exception e) {
            log.error("Failed to execute custom schedule job: {}", e.getMessage(), e);
        }
    }
}