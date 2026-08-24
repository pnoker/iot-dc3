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

package io.github.pnoker.common.mq.activemq.config;

import io.github.pnoker.common.mq.activemq.ActiveMqAdapter;
import io.github.pnoker.common.mq.config.BatchConsumerProperties;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Activates the ActiveMQ (Artemis client, JMS 2.0) adapter when
 * {@code dc3.mq.type=activemq}. Connection URL comes from
 * {@code dc3.mq.activemq.url} (also covers ActiveMQ Classic brokers).
 *
 * @author pnoker
 * @since 2026.8.19
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "dc3.mq", name = "type", havingValue = "activemq")
public class ActiveMqAdapterConfiguration {

    /** Connection factory from the adapter url/credentials; overridable by a user bean. */
    @Bean
    @ConditionalOnMissingBean(jakarta.jms.ConnectionFactory.class)
    public ActiveMQConnectionFactory activeMqConnectionFactory(
            @Value("${dc3.mq.activemq.url:tcp://localhost:61616}") String url,
            @Value("${dc3.mq.activemq.user:}") String user,
            @Value("${dc3.mq.activemq.password:}") String password) {
        return new ActiveMQConnectionFactory(url, user, password);
    }

    /** The port adapter bound to this broker's connection factory. */
    @Bean
    public ActiveMqAdapter activeMqAdapter(jakarta.jms.ConnectionFactory connectionFactory,
                                           BatchConsumerProperties batchProperties) {
        return new ActiveMqAdapter(connectionFactory, batchProperties);
    }
}
