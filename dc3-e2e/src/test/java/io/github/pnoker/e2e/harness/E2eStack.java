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

package io.github.pnoker.e2e.harness;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.time.Duration;

/**
 * Singleton container stack used by E2E tests.
 * <p>
 * Boots the production-pinned PostgreSQL+TimescaleDB and RabbitMQ images (the same
 * versions deployed in {@code dc3/docker-compose-db.yml}) on a shared docker network so
 * platform services can talk to them by hostname. Containers are started lazily on
 * first {@link #start()} call and reused across the JVM, keeping E2E feedback loops
 * under three minutes of warm-up.
 *
 * <p>The stack is intentionally <strong>infrastructure only</strong>. Service smoke /
 * REST-assured tests bring up the platform JVMs in-process via Spring Boot test
 * harnesses, which is faster and more deterministic than wrapping the full
 * {@code docker-compose.yml} (deferred until the platform publishes immutable image
 * tags suitable for CI).
 */
public final class E2eStack {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName
            .parse("timescale/timescaledb-ha:pg18")
            .asCompatibleSubstituteFor("postgres");

    private static final DockerImageName RABBIT_IMAGE = DockerImageName
            .parse("rabbitmq:3.13-management");

    private static final Network NETWORK = Network.newNetwork();

    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withNetwork(NETWORK)
            .withNetworkAliases("dc3-postgres")
            .withDatabaseName("dc3")
            .withUsername("dc3")
            .withPassword("dc3")
            .withReuse(reuseEnabled())
            .withStartupTimeout(Duration.ofMinutes(2));

    @SuppressWarnings("resource")
    private static final RabbitMQContainer RABBIT = new RabbitMQContainer(RABBIT_IMAGE)
            .withNetwork(NETWORK)
            .withNetworkAliases("dc3-rabbitmq")
            .withReuse(reuseEnabled())
            .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1))
            .withStartupTimeout(Duration.ofMinutes(2));

    private static volatile boolean started;

    private E2eStack() {
    }

    /**
     * Idempotent boot. The first invocation starts the containers; subsequent calls
     * are no-ops that return the already-running stack. Cross-run container reuse
     * is enabled only when the local Testcontainers environment supports it.
     */
    public static synchronized void start() {
        if (started) {
            return;
        }
        POSTGRES.start();
        RABBIT.start();
        started = true;
    }

    public static String postgresJdbcUrl() {
        ensureStarted();
        return POSTGRES.getJdbcUrl();
    }

    public static String postgresUsername() {
        ensureStarted();
        return POSTGRES.getUsername();
    }

    public static String postgresPassword() {
        ensureStarted();
        return POSTGRES.getPassword();
    }

    public static String rabbitHost() {
        ensureStarted();
        return RABBIT.getHost();
    }

    public static int rabbitAmqpPort() {
        ensureStarted();
        return RABBIT.getAmqpPort();
    }

    public static String rabbitUsername() {
        ensureStarted();
        return RABBIT.getAdminUsername();
    }

    public static String rabbitPassword() {
        ensureStarted();
        return RABBIT.getAdminPassword();
    }

    public static GenericContainer<?> rabbitContainer() {
        ensureStarted();
        return RABBIT;
    }

    public static PostgreSQLContainer<?> postgresContainer() {
        ensureStarted();
        return POSTGRES;
    }

    private static void ensureStarted() {
        if (!started) {
            throw new IllegalStateException(
                    "E2eStack has not been started. Call E2eStack.start() from a "
                            + "@BeforeAll hook or extend BaseE2eIT.");
        }
    }

    private static boolean reuseEnabled() {
        return TestcontainersConfiguration.getInstance().environmentSupportsReuse();
    }
}
