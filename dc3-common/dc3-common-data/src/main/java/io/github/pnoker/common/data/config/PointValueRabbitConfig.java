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

package io.github.pnoker.common.data.config;

import io.github.pnoker.common.config.MdcRequestIdListenerAdvice;
import io.github.pnoker.common.data.entity.property.PointBatchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.MessageBatchRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Point-value consumer configuration. RabbitMQ itself is the durable buffer; the
 * consumer creates bounded batches and acknowledges only after the PostgreSQL
 * transaction commits.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class PointValueRabbitConfig {

    private final ConnectionFactory connectionFactory;
    private final PointBatchProperties properties;

    @Bean("pointValueRabbitListenerContainerFactory")
    SimpleRabbitListenerContainerFactory pointValueRabbitListenerContainerFactory(
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(properties.getConcurrentConsumers());
        factory.setMaxConcurrentConsumers(properties.getMaxConcurrentConsumers());
        factory.setPrefetchCount(Math.max(properties.getPrefetchCount(), properties.getBatchSize()));
        factory.setBatchListener(true);
        factory.setConsumerBatchEnabled(true);
        factory.setBatchSize(properties.getBatchSize());
        factory.setBatchReceiveTimeout(properties.getReceiveTimeoutMillis());

        MessageBatchRecoverer recoverer = (messages, cause) -> {
            log.error("Point-value batch exhausted retries, rejecting to dead-letter exchange, size={}",
                    messages.size(), cause);
            throw new AmqpRejectAndDontRequeueException(
                    "Point-value batch exhausted retries", true, cause);
        };
        Advice retryAdvice = RetryInterceptorBuilder.stateless()
                .maxRetries(properties.getMaxRetries())
                .backOffOptions(properties.getRetryInitialIntervalMillis(),
                        properties.getRetryMultiplier(), properties.getRetryMaxIntervalMillis())
                .recoverer(recoverer)
                .build();
        factory.setAdviceChain(new MdcRequestIdListenerAdvice(), retryAdvice);
        return factory;
    }
}
