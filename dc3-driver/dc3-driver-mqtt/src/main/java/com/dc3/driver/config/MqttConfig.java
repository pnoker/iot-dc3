/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.driver.config;

import com.dc3.driver.bean.MqttProperty;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
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

import javax.annotation.Resource;

/**
 * @author pnoker
 */
@Slf4j
@Configuration
@IntegrationComponentScan
@EnableConfigurationProperties({MqttProperty.class})
public class MqttConfig {
    @Value("${driver.mqtt.client.id}")
    private String clientId;

    @Value("${driver.mqtt.default.topic}")
    private String defaultTopic;

    @Value("${driver.mqtt.default.qos}")
    private Integer defaultQos;

    @Resource
    private MqttProperty mqttProperty;

    @Bean
    public MqttConnectOptions getMqttConnectOptions() {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(mqttProperty.getUsername());
        mqttConnectOptions.setPassword(mqttProperty.getPassword().toCharArray());
        mqttConnectOptions.setServerURIs(new String[]{mqttProperty.getUrl()});
        mqttConnectOptions.setKeepAliveInterval(mqttProperty.getKeepAlive());
        return mqttConnectOptions;
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(getMqttConnectOptions());
        return factory;
    }

    @Bean
    public MessageChannel defaultMqttInputChannel() {
        return new DirectChannel();
    }

    /**
     * 说明：
     * ConditionalOnProperty(value = "driver.mqtt.default.receive.enable")
     * 根据配置属性driver.mqtt.default.receive.enable选择是否开启 Default Topic 主题的数据接收逻辑
     *
     * @return
     */
    @Bean
    @ConditionalOnProperty(value = "driver.mqtt.default.receive.enable")
    public MessageProducer defaultInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                clientId + "_default_inbound_",
                mqttClientFactory(),
                defaultTopic
        );
        adapter.setCompletionTimeout(mqttProperty.getCompletionTimeout());
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(defaultQos);
        adapter.setOutputChannel(defaultMqttInputChannel());
        return adapter;
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer inbound() {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                clientId + "_inbound",
                mqttClientFactory(),
                mqttProperty.getTopics().toArray(new String[0])
        );
        adapter.setCompletionTimeout(mqttProperty.getCompletionTimeout());
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(mqttProperty.getQos().stream().mapToInt(Integer::valueOf).toArray());
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    @Bean
    public MessageChannel mqttOutChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutChannel")
    public MessageHandler outbound() {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(clientId, mqttClientFactory());
        messageHandler.setAsync(true);
        messageHandler.setDefaultQos(defaultQos);
        messageHandler.setDefaultTopic(defaultTopic);
        return messageHandler;
    }

}