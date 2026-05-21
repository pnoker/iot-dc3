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
 * Spring auto-configuration for data service RabbitMQ topic exchanges.
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(ExchangeConfig.class)
public class DataTopicConfig {

    private final TopicExchange stateExchange;

    private final TopicExchange alarmExchange;

    private final TopicExchange valueExchange;

    public DataTopicConfig(TopicExchange stateExchange, TopicExchange alarmExchange, TopicExchange valueExchange) {
        this.stateExchange = stateExchange;
        this.alarmExchange = alarmExchange;
        this.valueExchange = valueExchange;
    }

    // ===== Driver state =====================================================

    @Bean
    Queue driverStateQueue() {
        return QueueBuilder.durable(RabbitConstant.QUEUE_DRIVER_STATE)
                .ttl(30000)
                .build();
    }

    @Bean
    Binding driverStateBinding(Queue driverStateQueue) {
        Binding binding = BindingBuilder.bind(driverStateQueue)
                .to(stateExchange)
                .with(RabbitConstant.ROUTING_DRIVER_STATE_PREFIX + SymbolConstant.ASTERISK);
        binding.addArgument(RabbitConstant.AUTO_DELETE, false);
        return binding;
    }

    // ===== Device state =====================================================

    @Bean
    Queue deviceStateQueue() {
        return QueueBuilder.durable(RabbitConstant.QUEUE_DEVICE_STATE)
                .ttl(30000)
                .build();
    }

    @Bean
    Binding deviceStateBinding(Queue deviceStateQueue) {
        Binding binding = BindingBuilder.bind(deviceStateQueue)
                .to(stateExchange)
                .with(RabbitConstant.ROUTING_DEVICE_STATE_PREFIX + SymbolConstant.ASTERISK);
        binding.addArgument(RabbitConstant.AUTO_DELETE, false);
        return binding;
    }

    // ===== Driver alarm =====================================================

    @Bean
    Queue driverAlarmQueue() {
        return QueueBuilder.durable(RabbitConstant.QUEUE_DRIVER_ALARM)
                .ttl(30000)
                .build();
    }

    @Bean
    Binding driverAlarmBinding(Queue driverAlarmQueue) {
        Binding binding = BindingBuilder.bind(driverAlarmQueue)
                .to(alarmExchange)
                .with(RabbitConstant.ROUTING_DRIVER_ALARM_PREFIX + SymbolConstant.ASTERISK);
        binding.addArgument(RabbitConstant.AUTO_DELETE, false);
        return binding;
    }

    // ===== Device alarm =====================================================

    @Bean
    Queue deviceAlarmQueue() {
        return QueueBuilder.durable(RabbitConstant.QUEUE_DEVICE_ALARM)
                .ttl(30000)
                .build();
    }

    @Bean
    Binding deviceAlarmBinding(Queue deviceAlarmQueue) {
        Binding binding = BindingBuilder.bind(deviceAlarmQueue)
                .to(alarmExchange)
                .with(RabbitConstant.ROUTING_DEVICE_ALARM_PREFIX + SymbolConstant.ASTERISK);
        binding.addArgument(RabbitConstant.AUTO_DELETE, false);
        return binding;
    }

    // ===== Point value ======================================================

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

    // ===== Notify task ======================================================

    /**
     * Outbound notification work queue. Each message is one channel send for a
     * fully-rendered payload; the worker that consumes the queue updates the
     * matching {@code dc3_notify_history} row from PENDING to its terminal
     * status. 24-hour TTL on the queue guards against runaway backlog when the
     * outbound channel adapters are stuck.
     */
    @Bean
    Queue notifyTaskQueue() {
        return QueueBuilder.durable(RabbitConstant.QUEUE_NOTIFY_TASK)
                .ttl(86_400_000)
                .build();
    }

    @Bean
    Binding notifyTaskBinding(Queue notifyTaskQueue) {
        Binding binding = BindingBuilder.bind(notifyTaskQueue)
                .to(alarmExchange)
                .with(RabbitConstant.ROUTING_NOTIFY_TASK_PREFIX + SymbolConstant.ASTERISK);
        binding.addArgument(RabbitConstant.AUTO_DELETE, false);
        return binding;
    }

}
