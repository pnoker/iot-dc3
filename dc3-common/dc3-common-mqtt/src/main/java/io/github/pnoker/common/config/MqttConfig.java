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
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.MqttUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.util.ArrayList;
import java.util.Objects;


/**
 * Mqtt 配置
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Configuration
public class MqttConfig {

    private final MqttProperties mqttProperties;

    public MqttConfig(MqttProperties mqttProperties) {
        this.mqttProperties = mqttProperties;
    }

    @Bean
    public MessageChannel mqttInboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(MqttUtil.getMqttConnectOptions(mqttProperties));
        return factory;
    }

    @Bean
    public MessageProducer mqttInbound(MqttPahoClientFactory mqttClientFactory) {
        if (Objects.isNull(mqttProperties.getReceiveTopics())) {
            mqttProperties.setReceiveTopics(new ArrayList<>());
        }

        mqttProperties.getReceiveTopics().forEach(topic -> topic.setName(mqttProperties.getTopicPrefix() + topic.getName()));
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                mqttProperties.getClient() + "_in",
                mqttClientFactory,
                mqttProperties.getReceiveTopics().stream().map(MqttProperties.Topic::getName).toArray(String[]::new)
        );
        adapter.setQos(mqttProperties.getReceiveTopics().stream().mapToInt(MqttProperties.Topic::getQos).toArray());
        adapter.setOutputChannel(mqttInboundChannel());
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setCompletionTimeout(mqttProperties.getCompletionTimeout());
        log.info("Set receive topics: {}", JsonUtil.toJsonString(mqttProperties.getReceiveTopics()));
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound(MqttPahoClientFactory mqttClientFactory) {
        mqttProperties.getDefaultSendTopic().setName(mqttProperties.getTopicPrefix() + mqttProperties.getDefaultSendTopic().getName());
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(
                mqttProperties.getClient() + "_out",
                mqttClientFactory
        );
        messageHandler.setAsync(true);
        messageHandler.setDefaultQos(mqttProperties.getDefaultSendTopic().getQos());
        messageHandler.setDefaultTopic(mqttProperties.getDefaultSendTopic().getName());
        log.info("Set default send topic: {}", JsonUtil.toJsonString(mqttProperties.getDefaultSendTopic()));
        return messageHandler;
    }

}