/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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

package io.github.pnoker.common.config;

import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.constant.driver.RabbitConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Topic Config
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Configuration
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
        Map<String, Object> arguments = new HashMap<>();
        // 30秒: 30 * 1000 = 30000L
        arguments.put(RabbitConstant.MESSAGE_TTL, 30000L);
        return new Queue(RabbitConstant.QUEUE_DRIVER_EVENT, true, false, false, arguments);
    }

    @Bean
    Binding driverEventBinding(Queue driverEventQueue) {
        Binding binding = BindingBuilder
                .bind(driverEventQueue)
                .to(eventExchange)
                .with(RabbitConstant.ROUTING_DRIVER_EVENT_PREFIX + SymbolConstant.ASTERISK);
        binding.addArgument(RabbitConstant.AUTO_DELETE, true);
        return binding;
    }

    @Bean
    Queue deviceEventQueue() {
        Map<String, Object> arguments = new HashMap<>();
        // 30秒: 30 * 1000 = 30000L
        arguments.put(RabbitConstant.MESSAGE_TTL, 30000L);
        return new Queue(RabbitConstant.QUEUE_DEVICE_EVENT, true, false, false, arguments);
    }

    @Bean
    Binding deviceEventBinding(Queue deviceEventQueue) {
        Binding binding = BindingBuilder
                .bind(deviceEventQueue)
                .to(eventExchange)
                .with(RabbitConstant.ROUTING_DEVICE_EVENT_PREFIX + SymbolConstant.ASTERISK);
        binding.addArgument(RabbitConstant.AUTO_DELETE, true);
        return binding;
    }

    @Bean
    Queue pointValueQueue() {
        Map<String, Object> arguments = new HashMap<>();
        // 7天:  7 * 24 * 60 * 60 * 1000 = 604800000L
        arguments.put(RabbitConstant.MESSAGE_TTL, 604800000L);
        return new Queue(RabbitConstant.QUEUE_POINT_VALUE, true, false, false, arguments);
    }

    @Bean
    Binding pointValueBinding(Queue pointValueQueue) {
        Binding binding = BindingBuilder
                .bind(pointValueQueue)
                .to(valueExchange)
                .with(RabbitConstant.ROUTING_POINT_VALUE_PREFIX + SymbolConstant.ASTERISK);
        binding.addArgument(RabbitConstant.AUTO_DELETE, true);
        return binding;
    }

}
