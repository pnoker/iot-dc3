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
import io.github.pnoker.common.mqtt.entity.property.MqttProperties;
import io.github.pnoker.common.mqtt.service.MqttReceiveService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * MQTT Schedule Job
 * <p>
 * Quartz job for processing MQTT messages in batch mode. Manages message counting, speed
 * calculation, and batch processing of MQTT messages with thread-safe operations.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Component
@ConditionalOnBean(MqttReceiveService.class)
public class MqttScheduleJob extends QuartzJobBean {

    public static final ReentrantReadWriteLock messageLock = new ReentrantReadWriteLock();

    public static final AtomicLong messageCount = new AtomicLong(0);

    public static final AtomicLong messageSpeed = new AtomicLong(0);

    private static final List<MqttMessage> mqttMessages = new ArrayList<>();

    @Resource
    private MqttProperties mqttProperties;

    @Resource
    private MqttReceiveService mqttReceiveService;

    @Resource
    private ExecutorService virtualThreadExecutor;

    /**
     * Get MqttMessage list size
     *
     * @return message size
     */
    public static int getMqttMessagesSize() {
        messageLock.readLock().lock();
        try {
            return mqttMessages.size();
        } finally {
            messageLock.readLock().unlock();
        }
    }

    /**
     * Clear MqttMessage list
     */
    public static void clearMqttMessages() {
        messageLock.writeLock().lock();
        try {
            mqttMessages.clear();
        } finally {
            messageLock.writeLock().unlock();
        }
    }

    /**
     * Add MqttMessage to list
     *
     * @param mqttMessage MqttMessage
     */
    public static void addMqttMessages(MqttMessage mqttMessage) {
        messageLock.writeLock().lock();
        try {
            mqttMessages.add(mqttMessage);
        } finally {
            messageLock.writeLock().unlock();
        }
    }

    /**
     * Execute scheduled job for batch MQTT message processing
     *
     * @param context Job execution context
     * @throws JobExecutionException if job execution fails
     */
    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        // Calculate MQTT message receive rate
        Integer interval = mqttProperties.getBatch().getInterval();
        Integer batchSpeed = mqttProperties.getBatch().getSpeed();
        long speed = messageCount.getAndSet(0) / interval;
        messageSpeed.set(speed);
        if (speed >= batchSpeed) {
            log.debug("Mqtt message receiver speed: {} /s, value size: {}, interval: {}", speed, getMqttMessagesSize(),
                    interval);
        }

        // Process a private snapshot outside the lock so inbound MQTT threads are not
        // blocked by downstream parsing or publishing.
        List<MqttMessage> snapshot;
        messageLock.writeLock().lock();
        try {
            if (mqttMessages.isEmpty()) {
                return;
            }
            snapshot = new ArrayList<>(mqttMessages);
            mqttMessages.clear();
        } finally {
            messageLock.writeLock().unlock();
        }

        virtualThreadExecutor.execute(() -> receiveBatch(snapshot));
    }

    private void receiveBatch(List<MqttMessage> mqttMessages) {
        try {
            mqttReceiveService.receiveValues(mqttMessages);
        } catch (Exception e) {
            log.error("MQTT batch message handling failed, size={}", mqttMessages.size(), e);
        }
    }

}
