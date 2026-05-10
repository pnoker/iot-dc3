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

import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * RabbitMQ Configuration Class
 * <p>
 * Configuration class for RabbitMQ messaging in Spring Boot applications. Configures
 * RabbitTemplate, listener container factory, and message converter for reliable message
 * publishing and consumption with proper error handling.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@AutoConfiguration
public class RabbitConfig {

    private final ConnectionFactory connectionFactory;

    public RabbitConfig(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Configure RabbitTemplate for message publishing
     *
     * @param messageConverter Message converter for JSON serialization
     * @return Configured RabbitTemplate bean
     */
    @Bean
    RabbitTemplate rabbitTemplate(MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        rabbitTemplate.setMandatory(true);
        rabbitTemplate
                .setReturnsCallback(message -> log.error("Send message[{}] to exchange[{}], routingKey[{}] failed: {}",
                        message.getMessage(), message.getExchange(), message.getRoutingKey(), message.getReplyText()));
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("CorrelationData[{}] ack failed: {}", correlationData, cause);
            }
        });
        return rabbitTemplate;
    }

    /**
     * Configure Rabbit listener container factory
     *
     * @param messageConverter Message converter for JSON deserialization
     * @return Configured RabbitListenerContainerFactory bean
     */
    @Bean
    public RabbitListenerContainerFactory<SimpleMessageListenerContainer> rabbitListenerContainerFactory(
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(8);
        factory.setPrefetchCount(10);
        return factory;
    }

    /**
     * Configure message converter for JSON serialization/deserialization
     *
     * @return JacksonJsonMessageConverter bean
     */
    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter(JsonUtil.getJsonMapper());
    }

}
