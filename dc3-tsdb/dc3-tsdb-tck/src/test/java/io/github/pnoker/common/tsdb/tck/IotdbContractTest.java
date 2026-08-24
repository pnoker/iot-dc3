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

package io.github.pnoker.common.tsdb.tck;

import io.github.pnoker.common.tsdb.iotdb.IotdbTsdbStore;
import io.github.pnoker.common.tsdb.spi.TsdbStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.Objects;

/**
 * Apache IoTDB harness for the store-neutral time-series contract suite — runs
 * against {@code apache/iotdb:2.0.10-standalone} with a one-line
 * iotdb-system.properties override mounting {@code timestamp_precision=us}; the
 * adapter's microsecond contract depends on it (defaults to milliseconds).
 * <p>
 * Disabled by default; opt in with {@code DC3_TSDB_TCK=true}.
 *
 * @author pnoker
 * @since 2026.8.21
 */
@EnabledIfEnvironmentVariable(named = "DC3_TSDB_TCK", matches = "(?i)true|1|yes|on")
class IotdbContractTest extends AbstractTsdbContractTest {

    /** The minimal properties override: Java properties merge over code defaults. */
    private static final GenericContainer<?> IOTDB = new GenericContainer<>(
            DockerImageName.parse("apache/iotdb:2.0.10-standalone"))
            .withExposedPorts(6667)
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("iotdb/iotdb-system.properties"),
                    "/iotdb/conf/iotdb-system.properties");

    private static volatile TsdbStore store;

    @BeforeAll
    static void startContainer() {
        IOTDB.start();
        awaitRpc();
    }

    @AfterAll
    static void stopContainer() {
        IOTDB.stop();
    }

    /**
     * Poll with real session handshakes — the RPC port accepts TCP well before
     * the data node finishes initializing and can serve thrift requests.
     */
    private static void awaitRpc() {
        long deadline = System.currentTimeMillis() + 5 * 60 * 1000;
        while (System.currentTimeMillis() < deadline) {
            org.apache.iotdb.session.Session probe =
                    new org.apache.iotdb.session.Session.Builder()
                            .host(IOTDB.getHost()).port(IOTDB.getMappedPort(6667))
                            .username("root").password("root")
                            .enableRedirection(false)
                            .build();
            try {
                probe.open();
                probe.close();
                return;
            } catch (Exception ignored) {
                // not up yet
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while waiting for IoTDB RPC", e);
            }
        }
        throw new IllegalStateException("IoTDB session never completed a handshake");
    }

    @Override
    protected TsdbStore store() {
        if (Objects.isNull(store)) {
            store = new IotdbTsdbStore(IOTDB.getHost(), IOTDB.getMappedPort(6667), "root", "root");
        }
        return store;
    }

    /**
     * Honest non-coverage: TTL retention needs clock manipulation the container
     * cannot provide; IoTDB retention is asserted by the adapter's SET TTL.
     */
    @Test
    void retentionPlaceholder() {
        // documented non-coverage: retention via TTL on root.dc3
    }
}
