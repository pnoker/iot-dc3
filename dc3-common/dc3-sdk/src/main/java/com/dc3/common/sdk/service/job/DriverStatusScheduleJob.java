/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.common.sdk.service.job;

import com.dc3.common.constant.Common;
import com.dc3.common.model.DriverEvent;
import com.dc3.common.sdk.bean.DriverContext;
import com.dc3.common.sdk.service.DriverService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 自定义调度任务
 *
 * @author pnoker
 */
@Slf4j
@Component
public class DriverStatusScheduleJob extends QuartzJobBean {

    @Value("${spring.application.name}")
    private String serviceName;
    @Resource
    private DriverContext driverContext;
    @Resource
    private DriverService driverService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        DriverEvent driverEvent = new DriverEvent(serviceName, Common.Driver.Event.HEARTBEAT, driverContext.getDriverStatus(), 10, TimeUnit.SECONDS);
        driverService.driverEventSender(driverEvent);
    }
}