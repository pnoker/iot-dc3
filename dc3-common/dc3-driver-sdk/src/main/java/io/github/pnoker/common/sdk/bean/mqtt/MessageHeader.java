/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.sdk.bean.mqtt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.messaging.MessageHeaders;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MessageHeader implements Serializable {
// TODO 该地方需要重新考虑设计，自己看看MQTT相关文档描述

    private UUID id;
    private Integer mqttId;
    private Integer mqttReceivedQos;
    private String mqttReceivedTopic;
    private Boolean mqttDuplicate;
    private Boolean mqttReceivedRetained;
    private Long timestamp;

    public MessageHeader(MessageHeaders messageHeaders) {
        if (!Objects.isNull(messageHeaders)) {
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
        } catch (Exception ignored) {
            // nothing to do
        }
        return null;
    }
}
