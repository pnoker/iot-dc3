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

package io.github.pnoker.common.mq.tck;

import io.github.pnoker.common.mq.adapter.BrokerAdapter;
import io.github.pnoker.common.mq.config.BatchConsumerProperties;
import io.github.pnoker.common.mq.mqtt.MqttMqAdapter;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Objects;

/**
 * MQTT 5 harness (HiveMQ CE) for the broker-neutral contract suite.
 * {@code TCK_MQTT_HOST} / {@code TCK_MQTT_PORT} point it at an externally managed
 * broker instead of the disposable container.
 *
 * <p>The no-consumer durability case is disabled: MQTT 5 is silent on retention for a
 * shared subscription while no member is online and HiveMQ CE drops those messages —
 * the documented variance in the design's §13.8 (deployers needing group durability
 * must pick a broker that retains).
 *
 * @author pnoker
 * @since 2026.8.19
 */
class MqttContractTest extends AbstractMqContractTest {

    private static final String EXTERNAL_HOST = System.getenv("TCK_MQTT_HOST");
    private static final String EXTERNAL_PORT = System.getenv("TCK_MQTT_PORT");

    // started manually (not via the extension) so the env override fully bypasses it
    private static final GenericContainer<?> HIVEMQ =
            new GenericContainer<>(DockerImageName.parse("hivemq/hivemq-ce:latest"))
                    .withExposedPorts(1883);

    private static String host() {
        if (Objects.nonNull(EXTERNAL_HOST)) {
            return EXTERNAL_HOST;
        }
        if (!HIVEMQ.isRunning()) {
            HIVEMQ.start();
        }
        return HIVEMQ.getHost();
    }

    private static int port() {
        if (Objects.nonNull(EXTERNAL_PORT)) {
            return Integer.parseInt(EXTERNAL_PORT);
        }
        return HIVEMQ.getMappedPort(1883);
    }

    private MqttMqAdapter mqttAdapter;

    @Override
    protected BrokerAdapter adapter() {
        if (Objects.isNull(mqttAdapter)) {
            BatchConsumerProperties properties = new BatchConsumerProperties();
            properties.setBatchSize(10);
            properties.setReceiveTimeoutMillis(100);
            properties.setMaxRetries(2);
            properties.setRetryInitialIntervalMillis(100);
            properties.setRetryMultiplier(2);
            properties.setRetryMaxIntervalMillis(200);
            mqttAdapter = new MqttMqAdapter(host(), port(), properties);
        }
        return mqttAdapter;
    }

    @Override
    protected void shutdownAdapter() {
        if (Objects.nonNull(mqttAdapter)) {
            mqttAdapter.stop();
        }
    }

    /**
     * MQTT 5 has no per-instance subscription expiry (capability false).
     */
    @Test
    @Override
    public void perInstanceSubscriptionExpiresAfterInstanceStops() {
        adapter();
        Assumptions.assumeTrue(mqttAdapter.capabilities().subscriptionExpiry(),
                "mqtt declares subscriptionExpiry=false");
    }

    @Test
    @Override
    void messagesSurviveWhileNoConsumerIsRunning() {
        adapter();
        org.junit.jupiter.api.Assumptions.assumeTrue(false,
                "MQTT 5 leaves retention for an offline shared subscription to the broker "
                        + "(design §13.8); HiveMQ CE drops such messages, so this case is "
                        + "a documented non-compliance rather than an adapter defect");
    }
}
