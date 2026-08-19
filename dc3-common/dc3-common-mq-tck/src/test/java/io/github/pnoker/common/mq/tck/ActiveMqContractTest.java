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

import io.github.pnoker.common.mq.activemq.ActiveMqAdapter;
import io.github.pnoker.common.mq.adapter.BrokerAdapter;
import io.github.pnoker.common.mq.config.BatchConsumerProperties;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Objects;

/**
 * ActiveMQ (Artemis) harness for the broker-neutral contract suite. By default a
 * disposable Artemis container; {@code TCK_ARTEMIS_URL} points it at an externally
 * managed broker instead.
 *
 * @author pnoker
 * @since 2026.8.19
 */
class ActiveMqContractTest extends AbstractMqContractTest {

    private static final String EXTERNAL_URL = System.getenv("TCK_ARTEMIS_URL");

    // started manually (not via the extension) so TCK_ARTEMIS_URL fully bypasses it
    private static final GenericContainer<?> ARTEMIS =
            new GenericContainer<>(DockerImageName.parse("apache/activemq-artemis:2.38.0-alpine"))
                    .withExposedPorts(61616)
                    .withEnv("ARTEMIS_USER", "artemis")
                    .withEnv("ARTEMIS_PASSWORD", "artemis");

    private static String brokerUrl() {
        if (Objects.nonNull(EXTERNAL_URL)) {
            return EXTERNAL_URL;
        }
        if (!ARTEMIS.isRunning()) {
            ARTEMIS.start();
        }
        return "tcp://" + ARTEMIS.getHost() + ":" + ARTEMIS.getMappedPort(61616);
    }

    private ActiveMqAdapter activeMqAdapter;

    @Override
    protected BrokerAdapter adapter() {
        if (Objects.isNull(activeMqAdapter)) {
            BatchConsumerProperties properties = new BatchConsumerProperties();
            properties.setBatchSize(10);
            properties.setReceiveTimeoutMillis(100);
            properties.setMaxRetries(2);
            properties.setRetryInitialIntervalMillis(100);
            properties.setRetryMultiplier(2);
            properties.setRetryMaxIntervalMillis(200);
            activeMqAdapter = new ActiveMqAdapter(new ActiveMQConnectionFactory(brokerUrl(), "artemis", "artemis"), properties);
        }
        return activeMqAdapter;
    }

    @Override
    protected void shutdownAdapter() {
        if (Objects.nonNull(activeMqAdapter)) {
            activeMqAdapter.stop();
        }
    }

    /**
     * JMS has no per-instance subscription expiry (capability false).
     */
    @Test
    @Override
    public void perInstanceSubscriptionExpiresAfterInstanceStops() {
        adapter();
        Assumptions.assumeTrue(activeMqAdapter.capabilities().subscriptionExpiry(),
                "activemq declares subscriptionExpiry=false");
    }
}
