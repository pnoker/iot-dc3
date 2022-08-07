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

package io.github.pnoker.common.sdk.bean.mqtt;

import cn.hutool.core.util.ObjectUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.messaging.MessageHeaders;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author pnoker
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MessageHeader implements Serializable {

    private String id;
    private Integer mqttId;
    private Integer mqttReceivedQos;
    private String mqttReceivedTopic;
    private Boolean mqttDuplicate;
    private Boolean mqttReceivedRetained;
    private Long timestamp;

    public MessageHeader(MessageHeaders messageHeaders) {
        if (ObjectUtil.isNotNull(messageHeaders)) {
            try {
                UUID id = messageHeaders.get("id", UUID.class);
                if (ObjectUtil.isNotNull(id)) {
                    this.id = id.toString();
                }
            } catch (Exception ignored) {
            }
            try {
                this.mqttId = messageHeaders.get("mqtt_id", Integer.class);
            } catch (Exception ignored) {
            }
            try {
                this.mqttReceivedQos = messageHeaders.get("mqtt_receivedQos", Integer.class);
            } catch (Exception ignored) {
            }
            try {
                this.mqttReceivedTopic = messageHeaders.get("mqtt_receivedTopic", String.class);
            } catch (Exception ignored) {
            }
            try {
                this.mqttDuplicate = messageHeaders.get("mqtt_duplicate", Boolean.class);
            } catch (Exception ignored) {
            }
            try {
                this.mqttReceivedRetained = messageHeaders.get("mqtt_receivedRetained", Boolean.class);
            } catch (Exception ignored) {
            }
            try {
                this.timestamp = messageHeaders.get("timestamp", Long.class);
            } catch (Exception ignored) {
            }
        }
    }
}
