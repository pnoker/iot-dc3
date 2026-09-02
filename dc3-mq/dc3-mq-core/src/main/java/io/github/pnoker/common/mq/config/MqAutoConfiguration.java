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
package io.github.pnoker.common.mq.config;

import io.github.pnoker.common.mq.adapter.BrokerAdapter;
import io.github.pnoker.common.mq.core.Dc3ListenerProcessor;
import io.github.pnoker.common.mq.core.MessageSenderImpl;
import io.github.pnoker.common.mq.sender.MessageSender;
import io.github.pnoker.common.mq.sender.ReactiveMessageSender;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Wires the messaging port: requires exactly one {@link BrokerAdapter} on the classpath
 * (selected by {@code dc3.mq.type}, default the rabbitmq adapter) and exposes the
 * business-facing {@link MessageSender} plus the {@code @Dc3Listener} processor. The
 * negotiated capabilities are logged at startup.
 *
 * @author pnoker
 * @since 2026.8.19
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(BatchConsumerProperties.class)
public class MqAutoConfiguration {

    /**
     * Publishing facade over the active adapter.
     */
    @Bean
    @ConditionalOnMissingBean(MessageSender.class)
    public MessageSender messageSender(ObjectProvider<BrokerAdapter> adapterProvider) {
        BrokerAdapter adapter = adapterProvider.getIfAvailable();
        if (Objects.isNull(adapter)) {
            throw new IllegalStateException("No BrokerAdapter bean found: add exactly one dc3-mq-* adapter dependency "
                    + "(selected by dc3.mq.type, default rabbitmq)");
        }
        log.info("MQ port negotiated, broker={}, capabilities={}", adapter.type(), adapter.capabilities());
        if (!adapter.capabilities().delayedMessage()) {
            log.info("Broker has no native delayed delivery: local scheduler fallback active");
        }
        if (!adapter.capabilities().publisherConfirm()) {
            log.info("Broker has no publisher confirmation: durability relies on the driver outbox");
        }
        return new MessageSenderImpl(adapter);
    }

    /**
     * Exposes the same publisher through the non-blocking contract. A custom
     * {@link MessageSender} must explicitly implement {@link ReactiveMessageSender};
     * silently adapting a blocking publisher would violate the reactive boundary.
     */
    @Bean
    @ConditionalOnMissingBean(ReactiveMessageSender.class)
    public ReactiveMessageSender reactiveMessageSender(MessageSender messageSender) {
        if (messageSender instanceof ReactiveMessageSender reactive) {
            return reactive;
        }
        throw new IllegalStateException("MessageSender must implement ReactiveMessageSender");
    }

    /**
     * Registers beans carrying @Dc3Listener methods with the active adapter.
     */
    @Bean
    @ConditionalOnMissingBean
    public Dc3ListenerProcessor dc3ListenerProcessor(ObjectProvider<BrokerAdapter> adapterProvider) {
        BrokerAdapter adapter = adapterProvider.getIfAvailable();
        if (Objects.isNull(adapter)) {
            throw new IllegalStateException("No BrokerAdapter bean found: add exactly one dc3-mq-* adapter dependency");
        }
        return new Dc3ListenerProcessor(adapter);
    }
}
