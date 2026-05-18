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

package io.github.pnoker.common.mqtt.handler;

import io.github.pnoker.common.mqtt.entity.MessageHeader;
import io.github.pnoker.common.mqtt.entity.MqttMessage;
import io.github.pnoker.common.mqtt.entity.property.MqttProperties;
import io.github.pnoker.common.mqtt.service.MqttReceiveService;
import io.github.pnoker.common.mqtt.service.job.MqttScheduleJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

/**
 * MQTT Receive Handler
 * <p>
 * Handler for processing incoming MQTT messages in IoT DC3 platform. Manages message
 * reception, routing, and batch processing based on message speed and configuration
 * settings.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@ConditionalOnBean(MqttReceiveService.class)
public class MqttReceiveHandler {

    @Resource
    private MqttProperties mqttProperties;

    @Resource
    private MqttReceiveService mqttReceiveService;

    @Resource
    private ExecutorService virtualThreadExecutor;

    /**
     * Configure MQTT inbound message handler bean
     * <p>
     * Receives data from MQTT; subscribed topics are defined in application.yml under
     * mqtt.receive-topics + (plus): matches exactly one word # (hash): matches multiple
     * words (or none)
     *
     * @return Configured MessageHandler for MQTT inbound processing
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public MessageHandler mqttInboundReceive() {
        return message -> {
            try {
                MessageHeader messageHeader = new MessageHeader(message.getHeaders());
                String payload = message.getPayload().toString();
                if (StringUtils.isEmpty(payload)) {
                    log.error("Invalid mqtt inbound, From: {}, Qos: {}, Payload: null",
                            messageHeader.getMqttReceivedTopic(), messageHeader.getMqttReceivedQos());
                    return;
                }
                MqttScheduleJob.MESSAGE_COUNT.getAndIncrement();
                MqttMessage mqttMessage = MqttMessage.builder().header(messageHeader).payload(payload).build();
                log.debug("Mqtt inbound, From: {}, Qos: {}, Payload: {}", messageHeader.getMqttReceivedTopic(),
                        messageHeader.getMqttReceivedQos(), payload);

                // Determine whether to process data in batch based on transmission speed
                Integer batchSpeed = mqttProperties.getBatch().getSpeed();
                if (MqttScheduleJob.MESSAGE_SPEED.get() < batchSpeed) {
                    virtualThreadExecutor.execute(() -> receiveSingle(mqttMessage));
                } else {
                    // Save message to batch schedule for processing
                    MqttScheduleJob.addMqttMessages(mqttMessage);
                }
            } catch (Exception e) {
                log.error("MQTT inbound dispatch failed, headers={}", message.getHeaders(), e);
            }
        };
    }

    private void receiveSingle(MqttMessage mqttMessage) {
        try {
            mqttReceiveService.receiveValue(mqttMessage);
        } catch (Exception e) {
            log.error("MQTT single message handling failed, topic={}, qos={}",
                    mqttMessage.getHeader().getMqttReceivedTopic(), mqttMessage.getHeader().getMqttReceivedQos(), e);
        }
    }

}
