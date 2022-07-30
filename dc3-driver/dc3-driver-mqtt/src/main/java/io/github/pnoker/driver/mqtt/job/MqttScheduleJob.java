/*
 * Copyright 2022 Pnoker All Rights Reserved
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

package io.github.pnoker.driver.mqtt.job;

import io.github.pnoker.common.sdk.bean.mqtt.MqttMessage;
import io.github.pnoker.driver.mqtt.service.MqttReceiveService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author pnoker
 */
@Slf4j
@Component
public class MqttScheduleJob extends QuartzJobBean {

    public static List<MqttMessage> mqttMessages = new ArrayList<>();
    public static ReentrantReadWriteLock messageLock = new ReentrantReadWriteLock();
    public static AtomicLong messageCount = new AtomicLong(0), messageSpeed = new AtomicLong(0);
    @Value("${driver.mqtt.batch.speed}")
    private Integer batchSpeed;
    @Value("${driver.mqtt.batch.interval}")
    private Integer interval;
    @Resource
    private MqttReceiveService mqttReceiveService;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        // Statistical mqtt message receive rate
        long speed = messageCount.getAndSet(0);
        messageSpeed.set(speed);
        speed /= interval;
        if (speed >= batchSpeed) {
            log.debug("Mqtt message receiver speed: {} /s, value size: {}, interval: {}", speed, mqttMessages.size(), interval);
        }

        // Receive batch mqtt message
        threadPoolExecutor.execute(() -> {
            messageLock.writeLock().lock();
            if (mqttMessages.size() > 0) {
                mqttReceiveService.receiveValues(mqttMessages);
                mqttMessages.clear();
            }
            messageLock.writeLock().unlock();
        });
    }
}
