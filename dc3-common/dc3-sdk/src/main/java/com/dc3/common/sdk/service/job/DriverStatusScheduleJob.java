/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
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

package com.dc3.common.sdk.service.job;

import com.dc3.common.bean.driver.DriverEvent;
import com.dc3.common.constant.Common;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    private RabbitTemplate rabbitTemplate;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        DriverEvent driverEvent = new DriverEvent(serviceName, Common.Driver.Event.HEARTBEAT, Common.Driver.Status.ONLINE, 10, TimeUnit.SECONDS);
        rabbitTemplate.convertAndSend(Common.Rabbit.TOPIC_EXCHANGE_EVENT, Common.Rabbit.ROUTING_DRIVER_EVENT_PREFIX + serviceName, driverEvent);
    }
}