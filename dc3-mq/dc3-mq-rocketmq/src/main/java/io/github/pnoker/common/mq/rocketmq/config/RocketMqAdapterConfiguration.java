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

package io.github.pnoker.common.mq.rocketmq.config;

import io.github.pnoker.common.mq.config.BatchConsumerProperties;
import io.github.pnoker.common.mq.rocketmq.RocketMqAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Activates the RocketMQ adapter when {@code dc3.mq.type=rocketmq}. Name server
 * address comes from {@code dc3.mq.rocketmq.name-server-address}.
 *
 * @author pnoker
 * @since 2026.8.19
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "dc3.mq", name = "type", havingValue = "rocketmq")
public class RocketMqAdapterConfiguration {

    @Bean
    public RocketMqAdapter rocketMqAdapter(
            @Value("${dc3.mq.rocketmq.name-server-address:localhost:9876}") String namesrvAddr,
            BatchConsumerProperties batchProperties) {
        return new RocketMqAdapter(namesrvAddr, batchProperties);
    }
}
