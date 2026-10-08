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
import io.github.pnoker.common.mqtt.handler.MqttReceiveHandler;
import io.github.pnoker.common.mqtt.service.MqttReceiveService;
import io.github.pnoker.common.mqtt.service.job.MqttScheduleJob;
import io.github.pnoker.common.utils.MqttUtil;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

/**
 * MQTT Configuration Class
 * <p>
 * Configuration class for MQTT integration in IoT DC3 platform. Configures MQTT client
 * factory, inbound/outbound channels, message handlers, and topic subscriptions.
 * </p>
 * <p>
 * Channel endpoints are wired explicitly with {@link EventDrivenConsumer} beans instead of
 * {@code @ServiceActivator} annotations (or {@code IntegrationFlow} beans): both
 * annotation-driven and DSL endpoint registration are performed by Spring Integration
 * infrastructure that is only registered through {@code @EnableIntegration}, and consumers
 * of this module do not have that infrastructure on their classpath. An annotated handler
 * bean is then a silent no-op: the handler exists but never subscribes, and every inbound
 * message fails with "Dispatcher has no subscribers" on its channel. An
 * {@link EventDrivenConsumer} is a plain {@code SmartLifecycle} bean, so the core container
 * itself starts it and performs the channel subscription; it is also the exact endpoint
 * type the annotation machinery would build for a {@link DirectChannel} input.
 * </p>
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
public class MqttConfig {

    private final MqttProperties mqttProperties;

    /**
     * MQTT inbound message channel bean
     *
     * @return DirectChannel for inbound MQTT messages
     */
    @Bean
    public DirectChannel mqttInboundChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT outbound message channel bean
     *
     * @return DirectChannel for outbound MQTT messages
     */
    @Bean
    public DirectChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT client factory bean configuration
     *
     * @return Configured MqttPahoClientFactory with connection options
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(MqttUtil.getMqttConnectOptions(mqttProperties));
        return factory;
    }

    /**
     * MQTT inbound message producer bean configuration
     *
     * @param mqttClientFactory MQTT client factory
     * @return Configured MessageProducer for MQTT inbound processing
     */
    @Bean
    @ConditionalOnBean(MqttReceiveService.class)
    public MessageProducer mqttInbound(MqttPahoClientFactory mqttClientFactory, MessageChannel mqttInboundChannel) {
        List<MqttProperties.Topic> receiveTopics = mqttProperties.getReceiveTopics();
        if (Objects.isNull(receiveTopics) || receiveTopics.isEmpty()) {
            throw new IllegalStateException(
                    "MQTT receive topics must be configured when MqttReceiveService is present");
        }

        List<MqttProperties.Topic> prefixedTopics = receiveTopics.stream()
                .map(topic -> new MqttProperties.Topic(prefixedTopicName(topic.getName()), topic.getQos()))
                .toList();
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                mqttProperties.getClient() + "_in",
                mqttClientFactory,
                prefixedTopics.stream().map(MqttProperties.Topic::getName).toArray(String[]::new));
        adapter.setQos(
                prefixedTopics.stream().mapToInt(MqttProperties.Topic::getQos).toArray());
        adapter.setOutputChannel(mqttInboundChannel);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setCompletionTimeout(mqttProperties.getCompletionTimeout());
        log.info(
                "MQTT inbound configured, clientId={}, topicCount={}",
                mqttProperties.getClient() + "_in",
                prefixedTopics.size());
        return adapter;
    }

    /**
     * MQTT receive handler bean configuration
     *
     * @param mqttReceiveService consumer-provided receive service
     * @param virtualThreadExecutor shared virtual thread executor
     * @return MqttReceiveHandler bound to the consumer's receive logic
     */
    @Bean
    @ConditionalOnBean(MqttReceiveService.class)
    public MqttReceiveHandler mqttReceiveHandler(
            MqttReceiveService mqttReceiveService, ExecutorService virtualThreadExecutor) {
        return new MqttReceiveHandler(mqttProperties, mqttReceiveService, virtualThreadExecutor);
    }

    /**
     * MQTT inbound endpoint bean configuration
     * <p>
     * Explicitly subscribes the receive handler to the inbound channel. On context start
     * the container starts this endpoint and {@link EventDrivenConsumer#doStart()} performs
     * the subscription, so the wiring does not depend on any annotation-driven endpoint
     * infrastructure being present.
     *
     * @param mqttReceiveHandler MQTT receive handler
     * @param mqttInboundChannel inbound channel the MQTT adapter delivers to
     * @return EventDrivenConsumer subscribed to the inbound MQTT channel
     */
    @Bean
    @ConditionalOnBean(MqttReceiveService.class)
    public EventDrivenConsumer mqttInboundReceive(
            MqttReceiveHandler mqttReceiveHandler, SubscribableChannel mqttInboundChannel) {
        return new EventDrivenConsumer(mqttInboundChannel, mqttReceiveHandler.mqttInboundReceive());
    }

    /**
     * MQTT batch schedule job bean configuration
     *
     * @param mqttReceiveService consumer-provided receive service
     * @param virtualThreadExecutor shared virtual thread executor
     * @return MqttScheduleJob draining the batch buffer to the consumer's receive logic
     */
    @Bean
    @ConditionalOnBean(MqttReceiveService.class)
    public MqttScheduleJob mqttScheduleJob(
            MqttReceiveService mqttReceiveService, ExecutorService virtualThreadExecutor) {
        return new MqttScheduleJob(mqttProperties, mqttReceiveService, virtualThreadExecutor);
    }

    /**
     * MQTT outbound message handler bean configuration
     *
     * @param mqttClientFactory MQTT client factory
     * @return Configured MqttPahoMessageHandler for MQTT outbound processing
     */
    @Bean
    public MqttPahoMessageHandler mqttOutboundHandler(MqttPahoClientFactory mqttClientFactory) {
        MqttProperties.Topic defaultSendTopic = mqttProperties.getDefaultSendTopic();
        MqttProperties.Topic prefixedDefaultSendTopic =
                new MqttProperties.Topic(prefixedTopicName(defaultSendTopic.getName()), defaultSendTopic.getQos());
        MqttPahoMessageHandler messageHandler =
                new MqttPahoMessageHandler(mqttProperties.getClient() + "_out", mqttClientFactory);
        messageHandler.setAsync(true);
        messageHandler.setDefaultQos(prefixedDefaultSendTopic.getQos());
        messageHandler.setDefaultTopic(prefixedDefaultSendTopic.getName());
        log.info(
                "MQTT outbound configured, clientId={}, defaultQos={}",
                mqttProperties.getClient() + "_out",
                prefixedDefaultSendTopic.getQos());
        return messageHandler;
    }

    /**
     * MQTT outbound endpoint bean configuration
     * <p>
     * Explicitly subscribes the outbound handler to the outbound channel, symmetric to the
     * inbound endpoint. Starting the endpoint also starts the handler, which opens the
     * publisher connection to the broker on demand.
     *
     * @param mqttOutboundHandler MQTT outbound message handler
     * @param mqttOutboundChannel outbound channel publishers send to
     * @return EventDrivenConsumer subscribed to the outbound MQTT channel
     */
    @Bean
    public EventDrivenConsumer mqttOutbound(
            MqttPahoMessageHandler mqttOutboundHandler, SubscribableChannel mqttOutboundChannel) {
        return new EventDrivenConsumer(mqttOutboundChannel, mqttOutboundHandler);
    }

    private String prefixedTopicName(String topicName) {
        String topicPrefix = mqttProperties.getTopicPrefix();
        if (StringUtils.isBlank(topicPrefix) || Strings.CS.startsWith(topicName, topicPrefix)) {
            return topicName;
        }
        return topicPrefix + topicName;
    }
}
