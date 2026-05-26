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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(MqttReceiveService.class)
@DisallowConcurrentExecution
public class MqttScheduleJob extends QuartzJobBean {

    private static final ReentrantReadWriteLock MESSAGE_LOCK = new ReentrantReadWriteLock();

    private static final AtomicLong MESSAGE_COUNT = new AtomicLong(0);

    private static final AtomicLong MESSAGE_SPEED = new AtomicLong(0);

    private static final List<MqttMessage> MQTT_MESSAGES = new ArrayList<>();

    private final MqttProperties mqttProperties;

    private final MqttReceiveService mqttReceiveService;

    private final ExecutorService virtualThreadExecutor;

    /**
     * Get MqttMessage list size
     *
     * @return message size
     */
    public static int getMqttMessagesSize() {
        MESSAGE_LOCK.readLock().lock();
        try {
            return MQTT_MESSAGES.size();
        } finally {
            MESSAGE_LOCK.readLock().unlock();
        }
    }

    /**
     * Clear MqttMessage list
     */
    public static void clearMqttMessages() {
        MESSAGE_LOCK.writeLock().lock();
        try {
            MQTT_MESSAGES.clear();
        } finally {
            MESSAGE_LOCK.writeLock().unlock();
        }
    }

    /**
     * Add MqttMessage to list
     *
     * @param mqttMessage MqttMessage
     */
    public static void addMqttMessages(MqttMessage mqttMessage) {
        MESSAGE_LOCK.writeLock().lock();
        try {
            MQTT_MESSAGES.add(mqttMessage);
        } finally {
            MESSAGE_LOCK.writeLock().unlock();
        }
    }

    public static void recordMessage() {
        MESSAGE_COUNT.getAndIncrement();
    }

    public static long getMessageCount() {
        return MESSAGE_COUNT.get();
    }

    public static long getMessageSpeed() {
        return MESSAGE_SPEED.get();
    }

    public static void resetMetrics() {
        MESSAGE_COUNT.set(0);
        MESSAGE_SPEED.set(0);
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
        long speed = MESSAGE_COUNT.getAndSet(0) / interval;
        MESSAGE_SPEED.set(speed);
        if (speed >= batchSpeed) {
            log.debug("Mqtt message receiver speed: {} /s, value size: {}, interval: {}", speed, getMqttMessagesSize(),
                    interval);
        }

        // Process a private snapshot outside the lock so inbound MQTT threads are not
        // blocked by downstream parsing or publishing.
        List<MqttMessage> snapshot;
        MESSAGE_LOCK.writeLock().lock();
        try {
            if (MQTT_MESSAGES.isEmpty()) {
                return;
            }
            snapshot = new ArrayList<>(MQTT_MESSAGES);
            MQTT_MESSAGES.clear();
        } finally {
            MESSAGE_LOCK.writeLock().unlock();
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
