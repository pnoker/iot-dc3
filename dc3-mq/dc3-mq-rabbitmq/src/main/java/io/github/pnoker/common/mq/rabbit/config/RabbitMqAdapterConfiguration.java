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

package io.github.pnoker.common.mq.rabbit.config;

import io.github.pnoker.common.mq.config.BatchConsumerProperties;
import io.github.pnoker.common.mq.rabbit.RabbitMqAdapter;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Activates the RabbitMQ adapter when {@code dc3.mq.type=rabbitmq} (the default). The
 * shared {@link RabbitTemplate} keeps the pre-port configuration — mandatory publishes,
 * returns logging, publisher-confirm callback, persistent delivery — and the Jackson
 * converter is retained for rolling-upgrade and test-harness compatibility.
 *
 * @author pnoker
 * @since 2026.8.19
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "dc3.mq", name = "type", havingValue = "rabbitmq", matchIfMissing = true)
public class RabbitMqAdapterConfiguration {

    /**
     * JSON converter with typed envelope headers.
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter(JsonUtil.getJsonMapper());
    }

    /**
     * Publisher-confirms template with mandatory returns.
     */
    @Bean(name = "rabbitTemplate")
    @ConditionalOnMissingBean(RabbitTemplate.class)
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReturnsCallback(returned -> {
            // Returned messages mean the broker accepted the publish but no queue was
            // bound to the routing key — almost always a deployment misconfiguration.
            // Never render the body: it can contain telemetry, commands or credentials.
            Message returnedMessage = returned.getMessage();
            int bodyLength = returnedMessage != null && returnedMessage.getBody() != null
                    ? returnedMessage.getBody().length : 0;
            log.error("RabbitMQ message returned, exchange={}, routingKey={}, replyCode={}, replyText={}, bodyLength={}",
                    returned.getExchange(), returned.getRoutingKey(), returned.getReplyCode(), returned.getReplyText(),
                    bodyLength);
        });
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("RabbitMQ publisher confirm NACK, correlationId={}, cause={}",
                        correlationData != null ? correlationData.getId() : null, cause);
            }
        });
        return rabbitTemplate;
    }

    /**
     * Declares queues/exchanges/bindings at startup.
     */
    @Bean
    @ConditionalOnMissingBean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * The port adapter bound to the template and admin.
     */
    @Bean
    public RabbitMqAdapter rabbitMqAdapter(RabbitTemplate rabbitTemplate, RabbitAdmin rabbitAdmin,
                                           ConnectionFactory connectionFactory, BatchConsumerProperties batchProperties,
                                           @Value("${dc3.driver.lease.queue-expires-millis:300000}")
                                           int driverQueueExpiresMillis) {
        return new RabbitMqAdapter(rabbitTemplate, rabbitAdmin, connectionFactory, batchProperties,
                driverQueueExpiresMillis);
    }
}
