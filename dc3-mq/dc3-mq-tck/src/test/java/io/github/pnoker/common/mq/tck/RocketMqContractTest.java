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
import io.github.pnoker.common.mq.rocketmq.RocketMqAdapter;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.Objects;

/**
 * RocketMQ harness for the broker-neutral contract suite. Requires a running
 * name server + broker pair (auto topic creation enabled); point it at them via
 * {@code TCK_ROCKETMQ_NAMESRV} (default localhost:9876). The pair is not managed
 * by testcontainers because RocketMQ needs two cooperating containers with an
 * addressable broker IP — the dc3 compose files carry a ready-made pair.
 *
 * @author pnoker
 * @since 2026.8.19
 */
class RocketMqContractTest extends AbstractMqContractTest {

    // opt-in: RocketMQ needs a name server + broker pair that testcontainers cannot
    // manage (addressable broker IP), so the suite only runs when pointed at one
    private static final String NAMESRV = System.getenv("TCK_ROCKETMQ_NAMESRV");

    private RocketMqAdapter rocketAdapter;

    @org.junit.jupiter.api.BeforeEach
    void requireNameServer() {
        org.junit.jupiter.api.Assumptions.assumeTrue(Objects.nonNull(NAMESRV),
                "set TCK_ROCKETMQ_NAMESRV to run the rocketmq contract suite "
                        + "(fresh-group offset isolation is under investigation; the adapter "
                        + "is not yet certified)");
    }

    @Override
    protected BrokerAdapter adapter() {
        if (Objects.isNull(rocketAdapter)) {
            BatchConsumerProperties properties = new BatchConsumerProperties();
            properties.setBatchSize(10);
            properties.setMaxRetries(2);
            properties.setRetryInitialIntervalMillis(100);
            properties.setRetryMultiplier(2);
            properties.setRetryMaxIntervalMillis(200);
            rocketAdapter = new RocketMqAdapter(NAMESRV, properties);
        }
        return rocketAdapter;
    }

    @Override
    protected void shutdownAdapter() {
        if (Objects.nonNull(rocketAdapter)) {
            rocketAdapter.stop();
        }
    }

    /**
     * RocketMQ has no per-instance subscription expiry (capability false).
     */
    @Test
    @Override
    public void perInstanceSubscriptionExpiresAfterInstanceStops() {
        adapter();
        Assumptions.assumeTrue(rocketAdapter.capabilities().subscriptionExpiry(),
                "rocketmq declares subscriptionExpiry=false");
    }
}
