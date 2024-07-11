/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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