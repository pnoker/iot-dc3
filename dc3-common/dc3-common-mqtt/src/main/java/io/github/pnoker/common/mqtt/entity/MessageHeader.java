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

package io.github.pnoker.common.mqtt.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Getter
@Setter
@Slf4j
public class MessageHeader implements Serializable {

    private UUID id;
    private Integer mqttId;
    private Integer mqttReceivedQos;
    private String mqttReceivedTopic;
    private Boolean mqttDuplicate;
    private Boolean mqttReceivedRetained;
    private Long timestamp;

    public MessageHeader(MessageHeaders messageHeaders) {
        if (Objects.nonNull(messageHeaders)) {
            this.id = messageHeaders.getId();
            this.mqttId = getMessageHeader(messageHeaders, "mqtt_id", Integer.class);
            this.mqttReceivedQos = getMessageHeader(messageHeaders, "mqtt_receivedQos", Integer.class);
            this.mqttReceivedTopic = getMessageHeader(messageHeaders, "mqtt_receivedTopic", String.class);
            this.mqttDuplicate = getMessageHeader(messageHeaders, "mqtt_duplicate", Boolean.class);
            this.mqttReceivedRetained = getMessageHeader(messageHeaders, "mqtt_receivedRetained", Boolean.class);
            this.timestamp = getMessageHeader(messageHeaders, "timestamp", Long.class);
        }
    }

    /**
     * 获取消息头
     *
     * @param messageHeaders MessageHeaders
     * @param key            Header Key
     * @param type           Header Key Type
     * @param <T>            Header Key Type
     * @return Header Value
     */
    private <T> T getMessageHeader(MessageHeaders messageHeaders, String key, Class<T> type) {
        try {
            return messageHeaders.get(key, type);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
