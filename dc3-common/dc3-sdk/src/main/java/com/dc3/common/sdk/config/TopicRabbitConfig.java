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

package com.dc3.common.sdk.config;

import com.dc3.common.constant.Common;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author pnoker
 */
@Slf4j
@Configuration
public class TopicRabbitConfig {
    @Value("${spring.application.name}")
    private String serviceName;

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
        return new TopicExchange(Common.Rabbit.TOPIC_EXCHANGE_EVENT, true, false);
    }

    @Bean
    Queue deviceEventQueue() {
        return new Queue(Common.Rabbit.QUEUE_DEVICE_EVENT, true, false, false);
    }

    @Bean
    TopicExchange notifyExchange() {
        return new TopicExchange(Common.Rabbit.TOPIC_EXCHANGE_NOTIFY, true, false);
    }

    @Bean
    Queue driverNotifyQueue() {
        return new Queue(Common.Rabbit.QUEUE_DRIVER_NOTIFY_PREFIX + this.serviceName, false, false, true);
    }

    @Bean
    Binding driverNotifyBinding() {
        return BindingBuilder
                .bind(driverNotifyQueue())
                .to(notifyExchange())
                .with(Common.Rabbit.ROUTING_DEVICE_NOTIFY_PREFIX + this.serviceName);
    }

    @Bean
    TopicExchange valueExchange() {
        return new TopicExchange(Common.Rabbit.TOPIC_EXCHANGE_VALUE, true, false);
    }

    @Bean
    Queue pointValueQueue() {
        return new Queue(Common.Rabbit.QUEUE_POINT_VALUE, true, false, false);
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
