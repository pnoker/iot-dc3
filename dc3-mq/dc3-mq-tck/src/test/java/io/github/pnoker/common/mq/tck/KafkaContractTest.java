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
import io.github.pnoker.common.mq.kafka.KafkaMqAdapter;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Objects;

/**
 * Kafka harness for the broker-neutral contract suite. By default a disposable KRaft
 * container; setting {@code TCK_KAFKA_BOOTSTRAP} points it at an externally managed
 * broker (useful where the managed container misbehaves under a given runtime).
 *
 * @author pnoker
 * @since 2026.8.19
 */
@Testcontainers(disabledWithoutDocker = true)
class KafkaContractTest extends AbstractMqContractTest {

    private static final String EXTERNAL_BOOTSTRAP = System.getenv("TCK_KAFKA_BOOTSTRAP");

    // started manually (not via the extension) so TCK_KAFKA_BOOTSTRAP fully bypasses it:
    // testcontainers 2.0.5 configures apache/kafka 3.9.0 with a nonroutable advertised
    // listener the broker rejects, so some runtimes need an externally managed broker.
    private static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:3.9.0"));
    private KafkaMqAdapter kafkaAdapter;

    private static String bootstrap() {
        if (Objects.nonNull(EXTERNAL_BOOTSTRAP)) {
            return EXTERNAL_BOOTSTRAP;
        }
        if (!KAFKA.isRunning()) {
            KAFKA.start();
        }
        return KAFKA.getBootstrapServers();
    }

    @Override
    protected BrokerAdapter adapter() {
        if (Objects.isNull(kafkaAdapter)) {
            String bootstrap = bootstrap();
            BatchConsumerProperties properties = new BatchConsumerProperties();
            properties.setBatchSize(10);
            properties.setMaxRetries(2);
            properties.setRetryInitialIntervalMillis(100);
            properties.setRetryMultiplier(2);
            properties.setRetryMaxIntervalMillis(200);
            kafkaAdapter = new KafkaMqAdapter(KafkaMqAdapter.template(bootstrap),
                    KafkaMqAdapter.consumerConfig(bootstrap), properties);
        }
        return kafkaAdapter;
    }

    @Override
    protected void shutdownAdapter() {
        if (Objects.nonNull(kafkaAdapter)) {
            kafkaAdapter.stop();
        }
    }

    /**
     * Kafka offsets persist; there is no per-instance subscription expiry (documented
     * cleanup policy, capability false).
     */
    @Test
    @Override
    public void perInstanceSubscriptionExpiresAfterInstanceStops() {
        adapter();
        Assumptions.assumeTrue(kafkaAdapter.capabilities().subscriptionExpiry(),
                "kafka declares subscriptionExpiry=false; documented cleanup policy applies");
    }
}
