/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.center.data.config;

import com.dc3.common.constant.Common;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author pnoker
 */
@Slf4j
@Configuration
public class TopicRabbitConfig {

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            log.error("Send message({}) to exchange({}), routingKey({}) failed: {}", message, exchange, routingKey, replyText);
        });
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("CorrelationData({}) ack failed: {}", correlationData, cause);
            }
        });
        return rabbitTemplate;
    }

    @Bean
    TopicExchange eventExchange() {
        return new TopicExchange(Common.Rabbit.TOPIC_EXCHANGE_EVENT, true, true);
    }

    @Bean
    Queue eventQueue() {
        return new Queue(Common.Rabbit.QUEUE_EVENT, true, false, true);
    }

    @Bean
    Binding eventBinding() {
        return BindingBuilder
                .bind(eventQueue())
                .to(eventExchange())
                .with(Common.Rabbit.ROUTING_EVENT_PREFIX + "*");
    }

    @Bean
    Queue deviceStatusQueue() {
        return new Queue(Common.Rabbit.QUEUE_DEVICE_STATUS, true, false, true);
    }

    @Bean
    Binding deviceStatusBinding() {
        return BindingBuilder
                .bind(deviceStatusQueue())
                .to(eventExchange())
                .with(Common.Rabbit.ROUTING_DEVICE_STATUS_PREFIX + "*");
    }

    @Bean
    TopicExchange valueExchange() {
        return new TopicExchange(Common.Rabbit.TOPIC_EXCHANGE_VALUE, true, true);
    }

    @Bean
    Queue singlePointValueQueue() {
        return new Queue(Common.Rabbit.QUEUE_POINT_SINGLE_VALUE, true, false, true);
    }

    @Bean
    Binding singlePointValueBinding() {
        return BindingBuilder
                .bind(singlePointValueQueue())
                .to(valueExchange())
                .with(Common.Rabbit.ROUTING_SINGLE_VALUE_PREFIX + "*");
    }

    @Bean
    Queue multiPointValueQueue() {
        return new Queue(Common.Rabbit.QUEUE_POINT_MULTI_VALUE, true, false, true);
    }

    @Bean
    Binding multiPointValueBinding() {
        return BindingBuilder
                .bind(multiPointValueQueue())
                .to(valueExchange())
                .with(Common.Rabbit.ROUTING_MULTI_VALUE_PREFIX + "*");
    }

    @Bean
    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }

}
