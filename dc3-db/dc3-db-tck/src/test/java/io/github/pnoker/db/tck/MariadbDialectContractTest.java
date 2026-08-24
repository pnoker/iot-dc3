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

package io.github.pnoker.db.tck;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

/**
 * MariaDB harness — mariadb:10.11 with the derived MySQL seed (the engines
 * share the DDL surface: utf8mb4, DATETIME(6), expression JSON defaults, the
 * row-level revision triggers). The mapper forks differ in exactly one
 * dimension: MariaDB never adopted the AS-new ODKU row alias, so its twins
 * reference VALUES(col) — the statement routing itself (databaseId=mariadb)
 * is part of what this suite certifies. 10.6+ is the floor (SKIP LOCKED,
 * expression defaults).
 *
 * @author pnoker
 * @since 2026.8.24
 */
@Testcontainers(disabledWithoutDocker = true)
class MariadbDialectContractTest extends AbstractDbDialectContractTest {

    @Container
    private static final GenericContainer<?> MARIADB =
            new GenericContainer<>("mariadb:10.11")
                    .withEnv("MARIADB_ROOT_PASSWORD", "dc3")
                    .withEnv("MARIADB_DATABASE", "dc3_data")
                    .withExposedPorts(3306)
                    .withCopyFileToContainer(
                            MountableFile.forHostPath("../../dc3/dependencies/mysql/initdb"),
                            "/docker-entrypoint-initdb.d")
                    // TCP listening flips when mariadbd is ready (log-following
                    // is unreliable against podman)
                    .waitingFor(Wait.forListeningPort()
                            .withStartupTimeout(java.time.Duration.ofMinutes(5)));

    @Override
    protected String jdbcUrl(String database) {
        return "jdbc:mariadb://" + MARIADB.getHost() + ":" + MARIADB.getMappedPort(3306)
                + "/" + database + "?useSSL=false&serverTimezone=UTC";
    }

    @Override
    protected String username() {
        // the seeded dc3 user only holds dc3_data; the TCK spans four databases
        return "root";
    }

    @Override
    protected String password() {
        return "dc3";
    }

    @BeforeAll
    void awaitMariadbd() throws Exception {
        // the TCP port opens during the entrypoint's local-only initialization
        // and closes again for the restart into the seeded runtime; poll a real
        // JDBC handshake instead of trusting port readiness
        long deadline = System.currentTimeMillis() + 5 * 60 * 1000;
        while (System.currentTimeMillis() < deadline) {
            try {
                java.sql.DriverManager.getConnection(jdbcUrl("dc3_data"), username(), password()).close();
                return;
            } catch (Exception ignored) {
                // still initializing or restarting
            }
            Thread.sleep(2000);
        }
        throw new IllegalStateException("mariadbd never accepted JDBC connections");
    }

    @Override
    protected String databaseId() {
        return "mariadb";
    }

    @Override
    protected String driverClass() {
        return "org.mariadb.jdbc.Driver";
    }
}
