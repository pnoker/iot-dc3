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

import lombok.RequiredArgsConstructor;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;

/**
 * RabbitMQ Configuration Class
 * <p>
 * Configures the shared {@link RabbitTemplate}, the default
 * {@link RabbitListenerContainerFactory} used by every {@code @RabbitListener}, and a
 * dedicated {@link #highThroughputRabbitListenerContainerFactory} for high-volume
 * streams (point values, mqtt fan-out) that need a wider prefetch / concurrency
 * window than the default factory's metadata-and-command friendly defaults.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
public class RabbitConfig {

    /**
     * Cap how many bytes of a returned message body we log. RabbitMQ returns happen on
     * the connection thread, so a 1 MB unroutable message would otherwise dump 1 MB into
     * the log file every time the routing key changes.
     */
    private static final int RETURN_BODY_LOG_LIMIT = 512;

    private final ConnectionFactory connectionFactory;

    private static String summarizeBody(Message message) {
        if (message == null || message.getBody() == null) {
            return "<empty>";
        }
        byte[] body = message.getBody();
        int len = Math.min(body.length, RETURN_BODY_LOG_LIMIT);
        String prefix = new String(body, 0, len, StandardCharsets.UTF_8);
        return body.length <= RETURN_BODY_LOG_LIMIT ? prefix : prefix + "…(truncated " + body.length + "B)";
    }

    /**
     * Configure RabbitTemplate for message publishing.
     *
     * @param messageConverter Message converter for JSON serialization
     * @return Configured RabbitTemplate bean
     */
    @Bean
    RabbitTemplate rabbitTemplate(MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReturnsCallback(returned -> {
            // Returned messages mean the broker accepted the publish but no queue was
            // bound to the routing key — almost always a deployment misconfiguration.
            // Keep the body excerpt bounded so a flood of returns can't blow up logs.
            log.error("RabbitMQ message returned, exchange={}, routingKey={}, replyCode={}, replyText={}, body={}",
                    returned.getExchange(), returned.getRoutingKey(), returned.getReplyCode(), returned.getReplyText(),
                    summarizeBody(returned.getMessage()));
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
     * Default listener container factory: tuned for command + metadata streams where
     * latency matters more than peak throughput.
     *
     * @param messageConverter Message converter for JSON deserialization
     * @return default RabbitListenerContainerFactory bean
     */
    @Bean
    public RabbitListenerContainerFactory<SimpleMessageListenerContainer> rabbitListenerContainerFactory(
            MessageConverter messageConverter) {
        return buildContainerFactory(messageConverter, 2, 8, 10);
    }

    /**
     * High-throughput listener container factory for point value streams and other
     * fan-in topics where the consumer can comfortably batch tens of messages per
     * trip. Listeners opt in via {@code containerFactory =
     * "highThroughputRabbitListenerContainerFactory"} on the {@code @RabbitListener}.
     *
     * @param messageConverter Message converter for JSON deserialization
     * @return high-throughput RabbitListenerContainerFactory bean
     */
    @Bean
    public RabbitListenerContainerFactory<SimpleMessageListenerContainer> highThroughputRabbitListenerContainerFactory(
            MessageConverter messageConverter) {
        return buildContainerFactory(messageConverter, 4, 32, 100);
    }

    /**
     * Configure message converter for JSON serialization/deserialization.
     *
     * @return JacksonJsonMessageConverter bean
     */
    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter(JsonUtil.getJsonMapper());
    }

    private SimpleRabbitListenerContainerFactory buildContainerFactory(MessageConverter messageConverter,
                                                                       int concurrent, int maxConcurrent, int prefetch) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(concurrent);
        factory.setMaxConcurrentConsumers(maxConcurrent);
        factory.setPrefetchCount(prefetch);
        return factory;
    }

}
