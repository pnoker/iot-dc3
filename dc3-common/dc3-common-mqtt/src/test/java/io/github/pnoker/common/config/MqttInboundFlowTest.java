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
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Message-flow tests for the MQTT inbound wiring.
 * <p>
 * Guards the subscription itself, not just bean existence: when the receive handler was
 * registered via {@code @ServiceActivator} on a {@code @Bean} method, the bean existed and
 * bean-existence tests passed, while every inbound message still failed with
 * "Dispatcher has no subscribers" because annotation-driven endpoint registration never ran.
 * That registration requires Spring Integration infrastructure registered through
 * {@code @EnableIntegration}, which no consumer of this module has on its classpath, so this
 * test context deliberately boots {@code MqttConfig} without it, exactly like a driver
 * application.
 * </p>
 *
 * @author pnoker
 * @since 2026.10.1
 */
class MqttInboundFlowTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues(
                    "dc3.driver.mqtt.url=tcp://127.0.0.1:1",
                    "dc3.driver.mqtt.client=flow-test",
                    "dc3.driver.mqtt.topic-prefix=dc3/test/",
                    "dc3.driver.mqtt.receive-topics[0].name=data",
                    "dc3.driver.mqtt.receive-topics[0].qos=1",
                    "dc3.driver.mqtt.completion-timeout=1000")
            .withBean(ExecutorService.class, MqttInboundFlowTest::directExecutorService)
            .withUserConfiguration(PropertiesConfiguration.class)
            .withConfiguration(AutoConfigurations.of(MqttConfig.class));

    /**
     * The inbound channel must have a live subscriber when the consumer's receive service
     * exists, otherwise every message the MQTT adapter delivers to the channel fails with
     * "Dispatcher has no subscribers".
     */
    @Test
    void inboundChannelHasASubscriberWhenReceiveServiceIsPresent() {
        contextRunner
                .withBean(MqttReceiveService.class, RecordingReceiveService::new)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean("mqttInboundChannel", DirectChannel.class)
                                    .getSubscriberCount())
                            .isPositive();
                    assertThat(context.getBean("mqttInboundReceive", EventDrivenConsumer.class)
                                    .isRunning())
                            .isTrue();
                });
    }

    /**
     * A message sent through the inbound channel must reach the consumer's receive service
     * with payload and topic intact. This is the flow that was broken while bean-existence
     * tests stayed green.
     */
    @Test
    void messageSentToInboundChannelReachesTheReceiveService() throws Exception {
        contextRunner
                .withBean(MqttReceiveService.class, RecordingReceiveService::new)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    DirectChannel inboundChannel = context.getBean("mqttInboundChannel", DirectChannel.class);
                    RecordingReceiveService receiveService = context.getBean(RecordingReceiveService.class);

                    inboundChannel.send(MessageBuilder.withPayload("value-from-test")
                            .setHeader("mqtt_receivedTopic", "dc3/test/data")
                            .setHeader("mqtt_receivedQos", 1)
                            .build());

                    MqttMessage received = receiveService.received().get(5, TimeUnit.SECONDS);
                    assertThat(received.getPayload()).isEqualTo("value-from-test");
                    assertThat(received.getHeader().getMqttReceivedTopic()).isEqualTo("dc3/test/data");
                });
    }

    /**
     * Without a receive service the receive side is conditional off: no endpoint must be
     * left subscribed to the inbound channel.
     */
    @Test
    void inboundChannelHasNoSubscriberWhenNoReceiveServiceIsPresent() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean("mqttInboundChannel", DirectChannel.class)
                            .getSubscriberCount())
                    .isZero();
        });
    }

    /**
     * The outbound endpoint is unconditional and must always subscribe its handler, so the
     * outbound channel stays usable for consumers that publish through it.
     */
    @Test
    void outboundChannelAlwaysHasASubscriber() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean("mqttOutboundChannel", DirectChannel.class)
                            .getSubscriberCount())
                    .isPositive();
            assertThat(context.getBean("mqttOutbound", EventDrivenConsumer.class)
                            .isRunning())
                    .isTrue();
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

    /**
     * Same-thread executor so the handler's dispatch hop onto the executor completes
     * within {@code channel.send} and flow failures surface directly at the send call.
     */
    private static ExecutorService directExecutorService() {
        return new AbstractExecutorService() {

            @Override
            public void execute(Runnable command) {
                command.run();
            }

            @Override
            public void shutdown() {
                // no threads are managed, nothing to shut down
            }

            @Override
            public List<Runnable> shutdownNow() {
                return List.of();
            }

            @Override
            public boolean isShutdown() {
                return false;
            }

            @Override
            public boolean isTerminated() {
                return false;
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) {
                return true;
            }
        };
    }
}
