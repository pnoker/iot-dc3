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

package com.pnoker.common.sdk.service.impl;

import com.pnoker.common.sdk.bean.ScheduleProperty;
import com.pnoker.common.sdk.service.job.CustomScheduleJob;
import com.pnoker.common.sdk.service.job.ReadScheduleJob;
import com.pnoker.common.sdk.service.DriverScheduleService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class DriverScheduleServiceImpl implements DriverScheduleService {
    @Resource
    private Scheduler scheduler;

    @Override
    @SneakyThrows
    public void initial(ScheduleProperty scheduleProperty) {
        if (scheduleProperty.getRead().getEnable()) {
            createJob("ReadGroup", "ReadScheduleJob", scheduleProperty.getRead().getCorn(), ReadScheduleJob.class);
        }
        if (scheduleProperty.getCustom().getEnable()) {
            createJob("CustomGroup", "CustomScheduleJob", scheduleProperty.getCustom().getCorn(), CustomScheduleJob.class);
        }
        if (!scheduler.isShutdown()) {
            scheduler.start();
        }
    }

    @SneakyThrows
    public void createJob(String group, String name, String corn, Class<? extends Job> jobClass) {
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(name, group).build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(name, group)
                .startAt(DateBuilder.futureDate(1, DateBuilder.IntervalUnit.SECOND))
                .withSchedule(CronScheduleBuilder.cronSchedule(corn))
                .startNow().build();
        scheduler.scheduleJob(jobDetail, trigger);
    }

}
