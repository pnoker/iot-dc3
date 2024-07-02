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

import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
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
public class DriverTopicConfig {

    private final DriverProperties driverProperties;
    private final TopicExchange metadataExchange;
    private final TopicExchange commandExchange;

    public DriverTopicConfig(DriverProperties driverProperties, TopicExchange metadataExchange, TopicExchange commandExchange) {
        this.driverProperties = driverProperties;
        this.metadataExchange = metadataExchange;
        this.commandExchange = commandExchange;
    }

    @Bean
    Queue metadataQueue() {
        Map<String, Object> arguments = new HashMap<>();
        // 30秒: 30 * 1000 = 30000L
        arguments.put(RabbitConstant.MESSAGE_TTL, 30000L);
        return new Queue(RabbitConstant.QUEUE_DRIVER_METADATA_PREFIX + driverProperties.getClient(), false, false, true, arguments);
    }

    @Bean
    Binding metadataBinding(Queue metadataQueue) {
        Binding binding = BindingBuilder
                .bind(metadataQueue)
                .to(metadataExchange)
                .with(RabbitConstant.ROUTING_DRIVER_METADATA_PREFIX + driverProperties.getService());
        binding.addArgument(RabbitConstant.AUTO_DELETE, true);
        return binding;
    }

    @Bean
    Queue driverCommandQueue() {
        Map<String, Object> arguments = new HashMap<>();
        // 30秒: 30 * 1000 = 30000L
        arguments.put(RabbitConstant.MESSAGE_TTL, 30000L);
        return new Queue(RabbitConstant.QUEUE_DRIVER_COMMAND_PREFIX + driverProperties.getService(), false, false, false, arguments);
    }

    @Bean
    Binding driverCommandBinding(Queue driverCommandQueue) {
        Binding binding = BindingBuilder
                .bind(driverCommandQueue)
                .to(commandExchange)
                .with(RabbitConstant.ROUTING_DRIVER_COMMAND_PREFIX + driverProperties.getService());
        binding.addArgument(RabbitConstant.AUTO_DELETE, true);
        return binding;
    }

    @Bean
    Queue deviceCommandQueue() {
        Map<String, Object> arguments = new HashMap<>();
        // 30秒: 30 * 1000 = 30000L
        arguments.put(RabbitConstant.MESSAGE_TTL, 30000L);
        return new Queue(RabbitConstant.QUEUE_DEVICE_COMMAND_PREFIX + driverProperties.getService(), false, false, false, arguments);
    }

    @Bean
    Binding deviceCommandBinding(Queue deviceCommandQueue) {
        Binding binding = BindingBuilder
                .bind(deviceCommandQueue)
                .to(commandExchange)
                .with(RabbitConstant.ROUTING_DEVICE_COMMAND_PREFIX + driverProperties.getService());
        binding.addArgument(RabbitConstant.AUTO_DELETE, true);
        return binding;
    }

}
