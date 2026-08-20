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

package io.github.pnoker.common.mq.message;

import io.github.pnoker.common.constant.mq.MqTopic;

import java.time.Duration;
import java.util.Map;

/**
 * Serialized broker-agnostic envelope handed to adapters: JSON body plus standardized
 * string headers. Adapters never see payload objects, so serialization stays in the
 * API layer and the wire format is identical across brokers.
 *
 * @author pnoker
 * @since 2026.8.19
 */
public record WireMqMessage(MqTopic topic, String partitionKey, byte[] body, Map<String, String> headers,
                            Duration delay) {
}
