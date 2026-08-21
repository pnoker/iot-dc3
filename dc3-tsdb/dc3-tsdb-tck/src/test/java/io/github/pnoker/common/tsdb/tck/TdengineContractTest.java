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

import io.github.pnoker.common.tsdb.spi.TsdbStore;
import io.github.pnoker.common.tsdb.tdengine.TdengineTsdbStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Base64;
import java.util.Objects;

/**
 * TDengine harness for the store-neutral time-series contract suite — runs against
 * {@code tdengine/tdengine:3.3.6.13} via the REST (TAOS-RS) JDBC driver. The
 * container is self-managed because the REST endpoint answers readiness only with
 * basic auth, which the built-in HTTP wait strategy cannot send.
 * <p>
 * Disabled by default; opt in with {@code DC3_TSDB_TCK=true} (image pull is heavy).
 *
 * @author pnoker
 * @since 2026.8.21
 */
@EnabledIfEnvironmentVariable(named = "DC3_TSDB_TCK", matches = "(?i)true|1|yes|on")
class TdengineContractTest extends AbstractTsdbContractTest {

    private static final String DATABASE = "dc3";

    private static final GenericContainer<?> TDENGINE = new GenericContainer<>(
            DockerImageName.parse("tdengine/tdengine:3.3.6.13"))
            .withExposedPorts(6041);

    private static volatile TsdbStore store;

    @BeforeAll
    static void startContainer() {
        TDENGINE.start();
        awaitRestReady();
    }

    @AfterAll
    static void stopContainer() {
        TDENGINE.stop();
    }

    /**
     * Poll the REST endpoint with a real statement over POST — the adapter only
     * serves the body-style route, GET with path SQL answers 404 even when healthy.
     */
    private static void awaitRestReady() {
        String token = Base64.getEncoder().encodeToString("root:taosdata".getBytes());
        String url = "http://" + TDENGINE.getHost() + ":" + TDENGINE.getMappedPort(6041) + "/rest/sql";
        long deadline = System.currentTimeMillis() + 10 * 60 * 1000;
        while (System.currentTimeMillis() < deadline) {
            try {
                java.net.HttpURLConnection connection =
                        (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Authorization", "Basic " + token);
                connection.getOutputStream().write("server_version".getBytes());
                if (connection.getResponseCode() == 200) {
                    return;
                }
            } catch (Exception ignored) {
                // not up yet
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while waiting for TDengine REST", e);
            }
        }
        throw new IllegalStateException("TDengine REST endpoint never became ready");
    }

    @Override
    protected TsdbStore store() {
        if (Objects.isNull(store)) {
            SimpleDriverDataSource dataSource = new SimpleDriverDataSource(
                    new com.taosdata.jdbc.rs.RestfulDriver(),
                    "jdbc:TAOS-RS://" + TDENGINE.getHost() + ":" + TDENGINE.getMappedPort(6041) + "/",
                    "root", "taosdata");
            store = new TdengineTsdbStore(dataSource, DATABASE);
        }
        return store;
    }

    /**
     * Same honest non-coverage as the timescale harness: retention needs clock
     * manipulation the container cannot provide; TDengine KEEP is asserted by the
     * adapter's bootstrap DDL.
     */
    @Test
    void retentionPlaceholder() {
        // documented non-coverage: retention tested at deployment level (KEEP 180)
    }
}
