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

package io.github.pnoker.test.containers;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * Shared PostgreSQL + TimescaleDB container that mirrors the production image
 * defined under dc3-docker (timescale/timescaledb-ha:pg18).
 *
 * <p>Tests should obtain the singleton via {@link #instance()} and register its
 * properties through {@link #register(DynamicPropertyRegistry)} to avoid spawning
 * a fresh container per test class.
 */
public final class PgTimescaleContainer {

    private static final DockerImageName IMAGE = DockerImageName
            .parse("timescale/timescaledb-ha:pg18")
            .asCompatibleSubstituteFor("postgres");

    private static final boolean REUSE_ENABLED = TestcontainersConfiguration.getInstance().environmentSupportsReuse();

    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> INSTANCE = new PostgreSQLContainer<>(IMAGE)
            .withDatabaseName("dc3")
            .withUsername("dc3")
            .withPassword("dc3")
            .withReuse(REUSE_ENABLED);

    static {
        INSTANCE.start();
    }

    private PgTimescaleContainer() {
    }

    public static PostgreSQLContainer<?> instance() {
        return INSTANCE;
    }

    /**
     * Wire the running container into the Spring environment so DataSource auto-config
     * picks it up. Call from a {@code @DynamicPropertySource} method.
     */
    public static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", INSTANCE::getUsername);
        registry.add("spring.datasource.password", INSTANCE::getPassword);
        registry.add("spring.datasource.driver-class-name", INSTANCE::getDriverClassName);
    }
}
