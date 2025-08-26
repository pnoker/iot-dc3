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

package io.github.pnoker.common.mqtt.service.job;

import io.github.pnoker.common.mqtt.entity.MqttMessage;
import io.github.pnoker.common.mqtt.service.MqttReceiveService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Component
public class MqttScheduleJob extends QuartzJobBean {

    public static final ReentrantReadWriteLock messageLock = new ReentrantReadWriteLock();
    public static final AtomicLong messageCount = new AtomicLong(0);
    public static final AtomicLong messageSpeed = new AtomicLong(0);
    private static final List<MqttMessage> mqttMessages = new ArrayList<>();
    @Value("${driver.mqtt.batch.speed}")
    private Integer batchSpeed;
    @Value("${driver.mqtt.batch.interval}")
    private Integer interval;
    @Resource
    private MqttReceiveService mqttReceiveService;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    /**
     * 获取 MqttMessage 长度
     *
     * @return 消息长度
     */
    public static int getMqttMessagesSize() {
        return mqttMessages.size();
    }

    /**
     * 清空 MqttMessage
     */
    public static void clearMqttMessages() {
        mqttMessages.clear();
    }

    /**
     * 添加 MqttMessage
     *
     * @param mqttMessage MqttMessage
     */
    public static void addMqttMessages(MqttMessage mqttMessage) {
        mqttMessages.add(mqttMessage);
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        // Statistical mqtt message receive rate
        long speed = messageCount.getAndSet(0);
        messageSpeed.set(speed);
        speed /= interval;
        if (speed >= batchSpeed) {
            log.debug("Mqtt message receiver speed: {} /s, value size: {}, interval: {}", speed, getMqttMessagesSize(), interval);
        }

        // Receive batch mqtt message
        threadPoolExecutor.execute(() -> {
            messageLock.writeLock().lock();
            if (!mqttMessages.isEmpty()) {
                mqttReceiveService.receiveValues(mqttMessages);
                clearMqttMessages();
            }
            messageLock.writeLock().unlock();
        });
    }
}
