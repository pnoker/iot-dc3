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

package io.github.pnoker.common.config;

import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.constant.driver.RabbitConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Topic Config
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(ExchangeConfig.class)
public class DataTopicConfig {

    private final TopicExchange eventExchange;

    private final TopicExchange valueExchange;

    public DataTopicConfig(TopicExchange eventExchange, TopicExchange valueExchange) {
        this.eventExchange = eventExchange;
        this.valueExchange = valueExchange;
    }

    @Bean
    Queue driverEventQueue() {
        return QueueBuilder.durable(RabbitConstant.QUEUE_DRIVER_EVENT)
                .ttl(30000)
                .build();
    }

    @Bean
    Binding driverEventBinding(Queue driverEventQueue) {
        Binding binding = BindingBuilder.bind(driverEventQueue)
                .to(eventExchange)
                .with(RabbitConstant.ROUTING_DRIVER_EVENT_PREFIX + SymbolConstant.ASTERISK);
        binding.addArgument(RabbitConstant.AUTO_DELETE, false);
        return binding;
    }

    @Bean
    Queue deviceEventQueue() {
        return QueueBuilder.durable(RabbitConstant.QUEUE_DEVICE_EVENT)
                .ttl(30000)
                .build();
    }

    @Bean
    Binding deviceEventBinding(Queue deviceEventQueue) {
        Binding binding = BindingBuilder.bind(deviceEventQueue)
                .to(eventExchange)
                .with(RabbitConstant.ROUTING_DEVICE_EVENT_PREFIX + SymbolConstant.ASTERISK);
        binding.addArgument(RabbitConstant.AUTO_DELETE, false);
        return binding;
    }

    @Bean
    Queue pointValueQueue() {
        return QueueBuilder.durable(RabbitConstant.QUEUE_POINT_VALUE)
                .ttl(604800000)
                .build();
    }

    @Bean
    Binding pointValueBinding(Queue pointValueQueue) {
        Binding binding = BindingBuilder.bind(pointValueQueue)
                .to(valueExchange)
                .with(RabbitConstant.ROUTING_POINT_VALUE_PREFIX + SymbolConstant.ASTERISK);
        binding.addArgument(RabbitConstant.AUTO_DELETE, false);
        return binding;
    }

}
