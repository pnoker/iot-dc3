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
package io.github.pnoker.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pnoker.common.mqtt.entity.property.MqttProperties;
import io.github.pnoker.common.utils.MqttUtil;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Locks the device-access plane's vendor neutrality (design §7.1): the MQTT driver
 * stack is a standard Paho client configured through {@link MqttUtil}, so any
 * conformant broker is a drop-in selection — switching EMQX for Mosquitto, HiveMQ,
 * NanoMQ or VerneMQ is a URL change, not a code change. This test proves the property
 * mechanically: the same client code performs a publish/subscribe round trip against
 * two different vendors, differing only in the broker URL.
 *
 * <p>The compose default (dc3-emqx) is a deployment choice; no broker-vendor code is
 * allowed on this plane (guardrail), and this test keeps it honest.
 *
 * <p>Disabled by default; opt in with {@code DC3_E2E=true}.
 */
@Tag("e2e")
@EnabledIfEnvironmentVariable(named = "DC3_E2E", matches = "(?i)true|1|yes|on")
class MqttVendorNeutralityIT {

    private static final GenericContainer<?> HIVEMQ =
            new GenericContainer<>(DockerImageName.parse("hivemq/hivemq-ce:latest")).withExposedPorts(1883);

    // the image ships /mosquitto-no-auth.conf exactly for anonymous local testing
    private static final GenericContainer<?> MOSQUITTO = new GenericContainer<>(
                    DockerImageName.parse("eclipse-mosquitto:2"))
            .withCommand("mosquitto", "-c", "/mosquitto-no-auth.conf")
            .withExposedPorts(1883);

    static Stream<Arguments> brokers() {
        return List.of(Named.of("HiveMQ CE", HIVEMQ), Named.of("Eclipse Mosquitto", MOSQUITTO)).stream()
                .map(Arguments::of);
    }

    @ParameterizedTest(name = "round trip on {0}")
    @MethodSource("brokers")
    void publishSubscribeRoundTripOnAnyConformantBroker(GenericContainer<?> broker) throws Exception {
        broker.start();

        // production code path: the driver's option builder over driver-style properties
        MqttProperties properties = new MqttProperties();
        properties.setUrl("tcp://" + broker.getHost() + ":" + broker.getMappedPort(1883));
        properties.setAuthType(MqttProperties.AuthTypeEnum.NONE);
        properties.setClient("dc3-e2e-vendor-" + UUID.randomUUID().toString().substring(0, 8));

        String topic = "dc3/e2e/vendor/" + UUID.randomUUID();
        CompletableFuture<String> received = new CompletableFuture<>();

        MqttClient client = new MqttClient(properties.getUrl(), properties.getClient(), new MemoryPersistence());
        try {
            client.connect(MqttUtil.getMqttConnectOptions(properties));
            assertThat(client.isConnected()).isTrue();

            client.subscribe(
                    topic,
                    1,
                    (name, message) -> received.complete(new String(message.getPayload(), StandardCharsets.UTF_8)));
            client.publish(topic, "vendor-neutral".getBytes(StandardCharsets.UTF_8), 1, false);

            assertThat(received.get(10, TimeUnit.SECONDS)).isEqualTo("vendor-neutral");
        } finally {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
            broker.stop();
        }
    }
}
