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

package io.github.pnoker.driver.mqtt.service.impl;

import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.mqtt.entity.MessageHeader;
import io.github.pnoker.common.mqtt.entity.MqttMessage;
import io.github.pnoker.common.mqtt.service.MqttReceiveService;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.LocalDateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * MQTT message receive service implementation.
 * <p>
 * This service handles incoming MQTT messages by converting them to PointValue objects
 * and forwarding them to the DC3 platform. It supports both single message and batch
 * message processing.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqttReceiveServiceImpl implements MqttReceiveService {

    private final DriverSenderService driverSenderService;

    /**
     * Processes a single MQTT message received from the broker.
     * <p>
     * This method parses the MQTT message payload into a PointValue object, sets the
     * creation time, and forwards it to the platform.
     * </p>
     *
     * @param mqttMessage the MQTT message containing topic, payload, and metadata
     */
    @Override
    public void receiveValue(MqttMessage mqttMessage) {
        // do something to process your mqtt messages
        log.debug("MQTT message received, topic={}, qos={}, payloadLength={}", topicOf(mqttMessage), qosOf(mqttMessage),
                payloadLengthOf(mqttMessage));
        PointValue pointValue = JsonUtil.parseObject(mqttMessage.getPayload(), PointValue.class);
        pointValue.setCreateTime(LocalDateTimeUtil.now());
        driverSenderService.pointValueSender(pointValue);
        log.debug("MQTT point value forwarded, topic={}, deviceId={}, pointId={}", topicOf(mqttMessage),
                pointValue.getDeviceId(), pointValue.getPointId());
    }

    /**
     * Processes a batch of MQTT messages received from the broker.
     * <p>
     * This method parses multiple MQTT messages into PointValue objects, sets their
     * creation times, and forwards them as a batch to the platform.
     * </p>
     *
     * @param mqttMessageList list of MQTT messages to process
     */
    @Override
    public void receiveValues(List<MqttMessage> mqttMessageList) {
        // do something to process your mqtt messages
        log.debug("MQTT message batch received, count={}", mqttMessageList.size());
        List<PointValue> pointValues = mqttMessageList.stream().map(mqttMessage -> {
            PointValue pointValue = JsonUtil.parseObject(mqttMessage.getPayload(), PointValue.class);
            pointValue.setCreateTime(LocalDateTimeUtil.now());
            return pointValue;
        }).toList();
        driverSenderService.pointValueSender(pointValues);
        log.debug("MQTT point value batch forwarded, count={}", pointValues.size());
    }

    private String topicOf(MqttMessage mqttMessage) {
        MessageHeader header = mqttMessage.getHeader();
        return Objects.isNull(header) ? null : header.getMqttReceivedTopic();
    }

    private Integer qosOf(MqttMessage mqttMessage) {
        MessageHeader header = mqttMessage.getHeader();
        return Objects.isNull(header) ? null : header.getMqttReceivedQos();
    }

    private int payloadLengthOf(MqttMessage mqttMessage) {
        String payload = mqttMessage.getPayload();
        return Objects.isNull(payload) ? 0 : payload.length();
    }

}
