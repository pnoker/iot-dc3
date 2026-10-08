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

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pnoker.common.mqtt.entity.MqttMessage;
import io.github.pnoker.common.mqtt.entity.property.MqttProperties;
import io.github.pnoker.common.mqtt.service.MqttReceiveService;
import io.github.pnoker.test.containers.MqttContainer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * End-to-end inbound flow test against a real Mosquitto broker.
 * <p>
 * Boots the real {@code MqttConfig} auto-configuration against the shared
 * {@link MqttContainer} broker and publishes with an independent raw Paho client, so the
 * whole production path is exercised: broker subscription by
 * {@code MqttPahoMessageDrivenChannelAdapter}, delivery into {@code mqttInboundChannel},
 * subscription by the explicit {@code EventDrivenConsumer} endpoint, and dispatch into the
 * consumer's {@code MqttReceiveService}. Like a driver application, the context has no
 * Spring Integration annotation infrastructure registered, which is the environment the
 * explicit wiring exists for.
 * </p>
 *
 * @author pnoker
 * @since 2026.10.1
 */
class MqttInboundFlowIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues(
                    "dc3.driver.mqtt.url=" + MqttContainer.brokerUrl(),
                    "dc3.driver.mqtt.client=mqtt-inbound-flow-it",
                    "dc3.driver.mqtt.topic-prefix=dc3/it/",
                    "dc3.driver.mqtt.receive-topics[0].name=data",
                    "dc3.driver.mqtt.receive-topics[0].qos=1",
                    "dc3.driver.mqtt.completion-timeout=10000")
            .withBean(ExecutorService.class, Executors::newSingleThreadExecutor)
            .withBean(MqttReceiveService.class, RecordingReceiveService::new)
            .withUserConfiguration(PropertiesConfiguration.class)
            .withConfiguration(AutoConfigurations.of(MqttConfig.class));

    /**
     * A message published to the subscribed topic over MQTT must reach the consumer's
     * receive service with payload and topic intact.
     */
    @Test
    void messagePublishedToBrokerReachesTheReceiveService() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean("mqttInboundReceive")).isNotNull();

            MqttClient publisher = new MqttClient(
                    MqttContainer.brokerUrl(), "mqtt-inbound-flow-it-publisher", new MemoryPersistence());
            publisher.connect();
            publisher.publish("dc3/it/data", "e2e-payload".getBytes(StandardCharsets.UTF_8), 1, false);
            publisher.disconnect();
            publisher.close();

            MqttMessage received =
                    context.getBean(RecordingReceiveService.class).received().get(20, TimeUnit.SECONDS);
            assertThat(received.getPayload()).isEqualTo("e2e-payload");
            assertThat(received.getHeader().getMqttReceivedTopic()).isEqualTo("dc3/it/data");
        });
    }

    /**
     * Property binding plumbing that the driver applications receive from
     * {@code MqttInitRunner}.
     */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MqttProperties.class)
    static class PropertiesConfiguration {}

    /** Receive service recording the single message handed to it. */
    static class RecordingReceiveService implements MqttReceiveService {

        private final CompletableFuture<MqttMessage> received = new CompletableFuture<>();

        CompletableFuture<MqttMessage> received() {
            return received;
        }

        @Override
        public void receiveValue(MqttMessage mqttMessage) {
            received.complete(mqttMessage);
        }

        @Override
        public void receiveValues(List<MqttMessage> mqttMessageList) {
            mqttMessageList.forEach(received::complete);
        }
    }
}
