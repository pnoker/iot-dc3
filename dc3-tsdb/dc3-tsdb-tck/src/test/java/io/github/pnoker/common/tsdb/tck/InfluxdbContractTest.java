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

import io.github.pnoker.common.tsdb.influxdb.InfluxdbTsdbStore;
import io.github.pnoker.common.tsdb.spi.TsdbStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * InfluxDB 3 harness for the store-neutral time-series contract suite — runs
 * against {@code influxdb:3.11.2-core}. Self-managed: the server requires
 * INFLUXDB3_NODE_ID/object-store env to serve at all, and the admin token is
 * minted once inside the container (the CLI prints the secret exactly once).
 * <p>
 * Disabled by default; opt in with {@code DC3_TSDB_TCK=true}.
 *
 * @author pnoker
 * @since 2026.8.21
 */
@EnabledIfEnvironmentVariable(named = "DC3_TSDB_TCK", matches = "(?i)true|1|yes|on")
class InfluxdbContractTest extends AbstractTsdbContractTest {

    private static final GenericContainer<?> INFLUX = new GenericContainer<>(
            DockerImageName.parse("influxdb:3.11.2-core"))
            .withExposedPorts(8181)
            .withEnv("INFLUXDB3_NODE_ID", "n0")
            .withEnv("INFLUXDB3_OBJECT_STORE", "file")
            .withEnv("INFLUXDB3_DATA_DIR", "/var/lib/influxdb3");

    private static volatile TsdbStore store;

    @BeforeAll
    static void startContainer() {
        INFLUX.start();
        awaitHttpUp();
    }

    @AfterAll
    static void stopContainer() {
        INFLUX.stop();
    }

    /** Any HTTP answer (even 401) means the node is serving; then mint a token. */
    private static void awaitHttpUp() {
        String url = "http://" + INFLUX.getHost() + ":" + INFLUX.getMappedPort(8181) + "/health";
        long deadline = System.currentTimeMillis() + 5 * 60 * 1000;
        while (System.currentTimeMillis() < deadline) {
            try {
                java.net.HttpURLConnection connection =
                        (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                connection.setConnectTimeout(2000);
                connection.setReadTimeout(2000);
                int code = connection.getResponseCode();
                if (code > 0) {
                    return;
                }
            } catch (Exception ignored) {
                // not up yet
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while waiting for InfluxDB 3", e);
            }
        }
        throw new IllegalStateException("InfluxDB 3 HTTP endpoint never answered");
    }

    @Override
    protected TsdbStore store() {
        if (Objects.isNull(store)) {
            // Each suite run gets its own container, so the freshly minted admin
            // token cannot collide with anything.
            String raw;
            try {
                raw = INFLUX.execInContainer("influxdb3", "create", "token", "--admin").getStdout();
            } catch (Exception e) {
                throw new IllegalStateException("token minting failed", e);
            }
            Matcher matcher = Pattern.compile("[A-Za-z0-9_-]{60,}").matcher(raw.replaceAll("\\x1b\\[[0-9;]*m", ""));
            if (!matcher.find()) {
                throw new IllegalStateException("no token in CLI output: " + raw);
            }
            String token = matcher.group();
            store = new InfluxdbTsdbStore(
                    "http://" + INFLUX.getHost() + ":" + INFLUX.getMappedPort(8181),
                    token, "dc3_tck");
        }
        return store;
    }

    /**
     * Honest non-coverage, same as the other harnesses: retention needs clock
     * manipulation; InfluxDB 3 Core retention is partition-level tooling.
     */
    @Test
    void retentionPlaceholder() {
        // documented non-coverage: retention managed at partition level
    }
}
