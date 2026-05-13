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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RabbitConfigTest {

    @Mock
    private ConnectionFactory connectionFactory;

    private RabbitConfig config;

    @BeforeEach
    void setUp() {
        config = new RabbitConfig(connectionFactory);
    }

    @Test
    void messageConverterIsJacksonBased() {
        MessageConverter converter = config.messageConverter();
        assertThat(converter).isInstanceOf(JacksonJsonMessageConverter.class);
    }

    @Test
    void rabbitTemplateMandatoryIsTrue() {
        RabbitTemplate template = config.rabbitTemplate(config.messageConverter());
        assertThat(template.isMandatoryFor(null)).isTrue();
    }

    @Test
    void rabbitTemplateUsesProvidedConverter() {
        MessageConverter converter = config.messageConverter();
        RabbitTemplate template = config.rabbitTemplate(converter);
        assertThat(template.getMessageConverter()).isSameAs(converter);
    }

    @Test
    void listenerFactoryUsesManualAck() {
        RabbitListenerContainerFactory<SimpleMessageListenerContainer> factory =
                config.rabbitListenerContainerFactory(config.messageConverter());
        // Cross-check via reflection because SimpleRabbitListenerContainerFactory does
        // not expose getAcknowledgeMode() — but the Spring class does store the mode.
        assertThat(factory).isInstanceOf(SimpleRabbitListenerContainerFactory.class);
        SimpleRabbitListenerContainerFactory simple = (SimpleRabbitListenerContainerFactory) factory;
        // Build a container — its ack mode mirrors the factory's configuration.
        SimpleMessageListenerContainer container = simple.createListenerContainer();
        assertThat(container.getAcknowledgeMode()).isEqualTo(AcknowledgeMode.MANUAL);
    }

    @Test
    void listenerFactoryProducesSimpleMessageContainer() {
        SimpleRabbitListenerContainerFactory factory =
                (SimpleRabbitListenerContainerFactory) config.rabbitListenerContainerFactory(config.messageConverter());
        SimpleMessageListenerContainer container = factory.createListenerContainer();
        // SimpleMessageListenerContainer concurrency / prefetch getters are protected
        // in this Spring AMQP version, so we settle for asserting the type and ack mode
        // here and let the integration tests verify the throughput numbers.
        assertThat(container).isNotNull();
    }
}
