/*
 * Copyright 2022 Pnoker All Rights Reserved
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

package io.github.pnoker.center.manager.config;

import io.github.pnoker.common.constant.CommonConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author pnoker
 */
@Slf4j
@Configuration
public class TopicRabbitConfig {

    @Resource
    private ConnectionFactory connectionFactory;

    @Bean
    RabbitTemplate rabbitTemplate() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReturnsCallback((message) -> {
            log.error("Send message({}) to exchange({}), routingKey({}) failed: {}", message.getMessage(), message.getExchange(), message.getRoutingKey(), message.getReplyText());
        });
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("CorrelationData({}) ack failed: {}", correlationData, cause);
            }
        });
        return rabbitTemplate;
    }

    @Bean
    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }

    @Bean
    TopicExchange eventExchange() {
        return new TopicExchange(CommonConstant.Rabbit.TOPIC_EXCHANGE_EVENT, true, false);
    }

    @Bean
    Queue driverEventQueue() {
        Map<String, Object> arguments = new HashMap<>();
        // 15秒：15 * 1000 = 15000L
        arguments.put(CommonConstant.Rabbit.MESSAGE_TTL, 15000L);
        return new Queue(CommonConstant.Rabbit.QUEUE_DRIVER_EVENT, true, false, false, arguments);
    }

    @Bean
    Binding driverEventBinding(TopicExchange eventExchange, Queue driverEventQueue) {
        return BindingBuilder
                .bind(driverEventQueue)
                .to(eventExchange)
                .with(CommonConstant.Rabbit.ROUTING_DRIVER_EVENT_PREFIX + CommonConstant.Symbol.ASTERISK);
    }

    @Bean
    Queue deviceEventQueue() {
        Map<String, Object> arguments = new HashMap<>();
        // 15秒：15 * 1000 = 15000L
        arguments.put(CommonConstant.Rabbit.MESSAGE_TTL, 15000L);
        return new Queue(CommonConstant.Rabbit.QUEUE_DEVICE_EVENT, true, false, false, arguments);
    }

    @Bean
    Binding deviceEventBinding(TopicExchange eventExchange, Queue deviceEventQueue) {
        return BindingBuilder
                .bind(deviceEventQueue)
                .to(eventExchange)
                .with(CommonConstant.Rabbit.ROUTING_DEVICE_EVENT_PREFIX + CommonConstant.Symbol.ASTERISK);
    }

}
