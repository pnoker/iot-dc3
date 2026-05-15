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

import io.github.pnoker.common.mqtt.entity.property.MqttProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.messaging.MessageChannel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MqttConfigTest {

    private MqttProperties properties;
    private MqttConfig config;

    @BeforeEach
    void setUp() {
        properties = new MqttProperties();
        properties.setUrl("tcp://broker.example.com:1883");
        properties.setClient("tenant/app/node");
        properties.setTopicPrefix("dc3/tenant/app/");
        properties.setDefaultSendTopic(new MqttProperties.Topic("command", 1));
        properties.setReceiveTopics(List.of(new MqttProperties.Topic("data", 1),
                new MqttProperties.Topic("dc3/tenant/app/status", 0)));
        config = new MqttConfig(properties);
    }

    @Test
    void inboundUsesProvidedChannelAndDoesNotMutateConfiguredTopics() {
        MessageChannel inboundChannel = config.mqttInboundChannel();

        MqttPahoMessageDrivenChannelAdapter adapter =
                (MqttPahoMessageDrivenChannelAdapter) config.mqttInbound(new DefaultMqttPahoClientFactory(),
                        inboundChannel);

        assertThat(((MessageProducerSupport) adapter).getOutputChannel()).isSameAs(inboundChannel);
        assertThat(properties.getReceiveTopics()).extracting(MqttProperties.Topic::getName)
                .containsExactly("data", "dc3/tenant/app/status");
    }

    @Test
    void outboundDoesNotMutateDefaultSendTopic() {
        config.mqttOutbound(new DefaultMqttPahoClientFactory());

        assertThat(properties.getDefaultSendTopic().getName()).isEqualTo("command");
        assertThat(properties.getDefaultSendTopic().getQos()).isEqualTo(1);
    }

    @Test
    void inboundRequiresTopicsWhenReceiverIsPresent() {
        properties.setReceiveTopics(List.of());

        assertThatThrownBy(() -> config.mqttInbound(new DefaultMqttPahoClientFactory(), config.mqttInboundChannel()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MQTT receive topics must be configured");
    }

}
