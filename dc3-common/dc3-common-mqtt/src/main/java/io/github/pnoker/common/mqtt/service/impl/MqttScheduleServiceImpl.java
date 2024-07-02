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

package io.github.pnoker.common.mqtt.service.impl;

import io.github.pnoker.common.mqtt.service.MqttScheduleService;
import io.github.pnoker.common.mqtt.service.job.MqttScheduleJob;
import io.github.pnoker.common.quartz.QuartzService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DateBuilder;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class MqttScheduleServiceImpl implements MqttScheduleService {

    @Value("${driver.mqtt.batch.interval}")
    private Integer interval;

    @Resource
    private QuartzService quartzService;

    @Override
    public void initial() {
        try {
            quartzService.createJobWithInterval("ScheduleGroup", "MqttScheduleJob", interval, DateBuilder.IntervalUnit.SECOND, MqttScheduleJob.class);

            quartzService.startScheduler();
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
        }
    }
}
