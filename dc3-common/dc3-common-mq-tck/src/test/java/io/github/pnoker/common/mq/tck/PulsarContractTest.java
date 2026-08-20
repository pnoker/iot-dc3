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
import io.github.pnoker.common.mq.pulsar.PulsarMqAdapter;
import org.apache.pulsar.client.api.PulsarClient;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.Objects;

/**
 * Pulsar harness for the broker-neutral contract suite. By default a disposable
 * standalone container; {@code TCK_PULSAR_URL} points it at an externally managed
 * broker instead.
 *
 * @author pnoker
 * @since 2026.8.20
 */
class PulsarContractTest extends AbstractMqContractTest {

    private static final String EXTERNAL_URL = System.getenv("TCK_PULSAR_URL");

    // started manually (not via the extension) so TCK_PULSAR_URL fully bypasses it
    private static final org.testcontainers.containers.GenericContainer<?> PULSAR =
            new org.testcontainers.containers.GenericContainer<>(
                    org.testcontainers.utility.DockerImageName.parse("apachepulsar/pulsar:4.1.3"))
                    .withCommand("bin/pulsar", "standalone", "-nfw")
                    .withExposedPorts(6650);

    private static String serviceUrl() {
        if (Objects.nonNull(EXTERNAL_URL)) {
            return EXTERNAL_URL;
        }
        if (!PULSAR.isRunning()) {
            PULSAR.start();
        }
        return "pulsar://" + PULSAR.getHost() + ":" + PULSAR.getMappedPort(6650);
    }

    private PulsarMqAdapter pulsarAdapter;

    @Override
    protected BrokerAdapter adapter() {
        if (Objects.isNull(pulsarAdapter)) {
            try {
                BatchConsumerProperties properties = new BatchConsumerProperties();
                properties.setBatchSize(10);
                properties.setMaxRetries(2);
                properties.setRetryInitialIntervalMillis(100);
                properties.setRetryMultiplier(2);
                properties.setRetryMaxIntervalMillis(200);
                pulsarAdapter = new PulsarMqAdapter(
                        PulsarClient.builder().serviceUrl(serviceUrl()).build(), properties);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return pulsarAdapter;
    }

    @Override
    protected void shutdownAdapter() {
        if (Objects.nonNull(pulsarAdapter)) {
            pulsarAdapter.stop();
        }
    }

    /**
     * Pulsar has no per-instance subscription expiry (capability false).
     */
    @Test
    @Override
    public void perInstanceSubscriptionExpiresAfterInstanceStops() {
        adapter();
        Assumptions.assumeTrue(pulsarAdapter.capabilities().subscriptionExpiry(),
                "pulsar declares subscriptionExpiry=false");
    }
}
