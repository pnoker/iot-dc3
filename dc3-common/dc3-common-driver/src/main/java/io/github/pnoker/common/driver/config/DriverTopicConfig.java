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

package io.github.pnoker.common.driver.config;

import io.github.pnoker.common.config.ExchangeConfig;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import lombok.RequiredArgsConstructor;
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
 * RabbitMQ configuration that declares the driver-specific metadata and command queues
 * together with their exchange bindings.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(ExchangeConfig.class)
@RequiredArgsConstructor
public class DriverTopicConfig {

    private final DriverProperties driverProperties;

    private final TopicExchange metadataExchange;

    private final TopicExchange pointCommandExchange;

    private final TopicExchange commandExchange;

    /**
     * Creates the metadata queue used to receive driver metadata synchronization events.
     *
     * @return metadata queue
     */
    @Bean
    Queue metadataQueue() {
        return QueueBuilder.durable(RabbitConstant.QUEUE_DRIVER_METADATA_PREFIX + driverProperties.getClient())
                .autoDelete()
                .ttl(30000)
                .build();
    }

    /**
     * Binds the metadata queue to the metadata exchange.
     *
     * @param metadataQueue metadata queue
     * @return queue binding
     */
    @Bean
    Binding metadataBinding(Queue metadataQueue) {
        Binding binding = BindingBuilder.bind(metadataQueue)
                .to(metadataExchange)
                .with(RabbitConstant.ROUTING_DRIVER_METADATA_PREFIX + driverProperties.getService());
        binding.addArgument(RabbitConstant.AUTO_DELETE, false);
        return binding;
    }

    /**
     * Creates the point command queue used to receive point read and write commands.
     *
     * @return point command queue
     */
    @Bean
    Queue pointCommandQueue() {
        return QueueBuilder.durable(RabbitConstant.QUEUE_POINT_COMMAND_PREFIX + driverProperties.getService())
                .ttl(30000)
                .deadLetterExchange(RabbitConstant.TOPIC_EXCHANGE_POINT_COMMAND_DEAD)
                .deadLetterRoutingKey(SymbolConstant.HASHTAG)
                .build();
    }

    /**
     * Binds the point command queue to the command exchange.
     *
     * @param pointCommandQueue point command queue
     * @return queue binding
     */
    @Bean
    Binding pointCommandBinding(Queue pointCommandQueue) {
        Binding binding = BindingBuilder.bind(pointCommandQueue)
                .to(pointCommandExchange)
                .with(RabbitConstant.ROUTING_POINT_COMMAND_PREFIX + driverProperties.getService());
        binding.addArgument(RabbitConstant.AUTO_DELETE, false);
        return binding;
    }

    /**
     * Creates the custom command queue used to receive custom command calls.
     *
     * @return command queue
     */
    @Bean
    Queue commandQueue() {
        return QueueBuilder.durable(RabbitConstant.QUEUE_COMMAND_PREFIX + driverProperties.getService())
                .ttl(30000)
                .deadLetterExchange(RabbitConstant.TOPIC_EXCHANGE_COMMAND_DEAD)
                .deadLetterRoutingKey(SymbolConstant.HASHTAG)
                .build();
    }

    /**
     * Binds the custom command queue to the command exchange.
     *
     * @param commandQueue command queue
     * @return queue binding
     */
    @Bean
    Binding commandBinding(Queue commandQueue) {
        Binding binding = BindingBuilder.bind(commandQueue)
                .to(commandExchange)
                .with(RabbitConstant.ROUTING_COMMAND_PREFIX + driverProperties.getService());
        binding.addArgument(RabbitConstant.AUTO_DELETE, false);
        return binding;
    }

}
