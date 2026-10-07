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
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.MessageHandler;

/**
 * MQTT Receive Handler
 * <p>
 * Handler for processing incoming MQTT messages in IoT DC3 platform. Manages message
 * reception, routing, and batch processing based on message speed and configuration
 * settings.
 * </p>
 * <p>
 * Not a component-scanned bean: it is registered by {@code MqttConfig} as an
 * auto-configuration {@code @Bean} guarded by {@code @ConditionalOnBean} so the
 * condition is evaluated only after consumer bean definitions (the driver's
 * {@link MqttReceiveService} implementation) have been registered.
 * </p>
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@RequiredArgsConstructor
public class MqttReceiveHandler {

    private final MqttProperties mqttProperties;

    private final MqttReceiveService mqttReceiveService;

    private final ExecutorService virtualThreadExecutor;

    /**
     * Build the MQTT inbound message handler
     * <p>
     * Receives data from MQTT; subscribed topics are defined in application.yml under
     * mqtt.receive-topics + (plus): matches exactly one word # (hash): matches multiple
     * words (or none)
     *
     * @return Configured MessageHandler for MQTT inbound processing
     */
    public MessageHandler mqttInboundReceive() {
        return message -> {
            try {
                MessageHeader messageHeader = new MessageHeader(message.getHeaders());
                String payload = message.getPayload().toString();
                if (StringUtils.isEmpty(payload)) {
                    log.error(
                            "MQTT inbound rejected, reason=emptyPayload, topic={}, qos={}",
                            messageHeader.getMqttReceivedTopic(),
                            messageHeader.getMqttReceivedQos());
                    return;
                }
                MqttScheduleJob.recordMessage();
                MqttMessage mqttMessage = MqttMessage.builder()
                        .header(messageHeader)
                        .payload(payload)
                        .build();
                log.debug(
                        "MQTT inbound received, topic={}, qos={}, payloadLength={}",
                        messageHeader.getMqttReceivedTopic(),
                        messageHeader.getMqttReceivedQos(),
                        payload.length());

                // Determine whether to process data in batch based on transmission speed
                Integer batchSpeed = mqttProperties.getBatch().getSpeed();
                if (MqttScheduleJob.getMessageSpeed() < batchSpeed) {
                    virtualThreadExecutor.execute(() -> receiveSingle(mqttMessage));
                } else {
                    // Save message to batch schedule for processing
                    MqttScheduleJob.addMqttMessages(mqttMessage);
                }
            } catch (Exception e) {
                log.error(
                        "MQTT inbound dispatch failed, payloadType={}",
                        message.getPayload().getClass().getSimpleName(),
                        e);
            }
        };
    }

    private void receiveSingle(MqttMessage mqttMessage) {
        try {
            mqttReceiveService.receiveValue(mqttMessage);
        } catch (Exception e) {
            log.error(
                    "MQTT single message handling failed, topic={}, qos={}",
                    mqttMessage.getHeader().getMqttReceivedTopic(),
                    mqttMessage.getHeader().getMqttReceivedQos(),
                    e);
        }
    }
}
