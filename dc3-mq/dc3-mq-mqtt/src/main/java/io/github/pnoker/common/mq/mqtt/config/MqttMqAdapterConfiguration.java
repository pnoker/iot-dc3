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

package io.github.pnoker.common.mq.mqtt.config;

import io.github.pnoker.common.mq.config.BatchConsumerProperties;
import io.github.pnoker.common.mq.mqtt.MqttMqAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Activates the MQTT 5 adapter when {@code dc3.mq.type=mqtt}. Broker host/port come
 * from {@code dc3.mq.mqtt.host} / {@code dc3.mq.mqtt.port} (any MQTT 5 broker with
 * shared subscriptions: EMQX, HiveMQ, NanoMQ, VerneMQ ...).
 *
 * @author pnoker
 * @since 2026.8.19
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "dc3.mq", name = "type", havingValue = "mqtt")
public class MqttMqAdapterConfiguration {

    /**
     * The port adapter over the HiveMQ MQTT5 client.
     */
    @Bean
    public MqttMqAdapter mqttMqAdapter(@Value("${dc3.mq.mqtt.host:localhost}") String host,
                                       @Value("${dc3.mq.mqtt.port:1883}") int port,
                                       BatchConsumerProperties batchProperties) {
        return new MqttMqAdapter(host, port, batchProperties);
    }
}
