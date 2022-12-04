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

import io.github.pnoker.common.utils.JsonUtil;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Data
@Accessors(chain = true)
public class MessagePayload {
    private String payload;
    private MessageType messageType;

    public MessagePayload() {
        this.messageType = MessageType.DEFAULT;
    }

    public MessagePayload(Object payload, MessageType messageType) {
        this.payload = JsonUtil.toJsonString(payload);
        this.messageType = messageType;
    }
}