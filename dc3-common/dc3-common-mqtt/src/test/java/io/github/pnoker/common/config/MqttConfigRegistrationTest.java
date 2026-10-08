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

import io.github.pnoker.common.mqtt.entity.property.MqttProperties;
import io.github.pnoker.common.mqtt.handler.MqttReceiveHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.endpoint.EventDrivenConsumer;

/**
 * Registration tests for the MQTT auto-configuration receive side.
 * <p>
 * Guards the bean-registration timing of {@code MqttReceiveHandler} and
 * {@code MqttScheduleJob}: their {@code @ConditionalOnBean(MqttReceiveService.class)}
 * conditions must be evaluated in the auto-configuration phase, after consumer bean
 * definitions are registered. When they were component-scanned classes the condition was
 * evaluated at scan time against an incomplete registry, a consumer service scanned
 * after the handler package was not yet visible, and the inbound channel ended up with a
 * producer but no subscriber ("Dispatcher has no subscribers for channel ...
 * mqttInboundChannel").
 * </p>
 * <p>
 * Bean existence alone does not prove the channel is subscribed; the message-flow level
 * is guarded by {@link MqttInboundFlowTest}.
 * </p>
 *
 * @author pnoker
 * @since 2026.10.1
 */
class MqttConfigRegistrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues(
                    "dc3.driver.mqtt.url=tcp://127.0.0.1:1",
                    "dc3.driver.mqtt.client=registration-test",
                    "dc3.driver.mqtt.topic-prefix=dc3/test/",
                    "dc3.driver.mqtt.receive-topics[0].name=data",
                    "dc3.driver.mqtt.receive-topics[0].qos=1",
                    "dc3.driver.mqtt.completion-timeout=1000")
            .withBean(ExecutorService.class, () -> Executors.newSingleThreadExecutor())
            .withUserConfiguration(PropertiesConfiguration.class)
            .withConfiguration(AutoConfigurations.of(MqttConfig.class));

    /**
     * The receive side must be registered when the consumer's receive service exists,
     * even when that service is component-scanned after the handler's own package.
     * <p>
     * The consumer configuration scans the handler package first on purpose: with the
     * old component-scanned handler, its {@code @ConditionalOnBean} condition ran at
     * scan time, before the scanned service definition existed, so the handler (and its
     * inbound subscriber) silently disappeared.
     * </p>
     */
    @Test
    void receiveSideBeansAreRegisteredWhenReceiveServiceIsScannedAfterHandlerPackage() {
        contextRunner
                .withUserConfiguration(ScanOrderConsumerConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("mqttReceiveHandler");
                    assertThat(context.getBean("mqttReceiveHandler")).isInstanceOf(MqttReceiveHandler.class);
                    assertThat(context).hasBean("mqttInboundReceive");
                    assertThat(context.getBean("mqttInboundReceive")).isInstanceOf(EventDrivenConsumer.class);
                    assertThat(context).hasBean("mqttScheduleJob");
                    assertThat(context).hasBean("mqttInbound");
                });
    }

    @Test
    void receiveSideBeansAreAbsentWhenNoReceiveServiceBeanIsPresent() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean("mqttReceiveHandler");
            assertThat(context).doesNotHaveBean("mqttInboundReceive");
            assertThat(context).doesNotHaveBean("mqttScheduleJob");
            assertThat(context).doesNotHaveBean("mqttInbound");
            assertThat(context).hasBean("mqttOutboundHandler");
            assertThat(context.getBean("mqttOutbound")).isInstanceOf(EventDrivenConsumer.class);
        });
    }

    /**
     * Property binding plumbing that the driver applications receive from
     * {@code MqttInitRunner}.
     */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MqttProperties.class)
    static class PropertiesConfiguration {}

    /**
     * Consumer configuration mirroring a driver module: the receive service is
     * component-scanned, and the handler package is scanned before the consumer's own
     * package, as happens in any application whose component scan root covers
     * {@code io.github.pnoker}.
     */
    @Configuration(proxyBeanMethods = false)
    @ComponentScan(basePackages = {"io.github.pnoker.common.mqtt.handler", "io.github.pnoker.consumerscan"})
    static class ScanOrderConsumerConfiguration {}
}
