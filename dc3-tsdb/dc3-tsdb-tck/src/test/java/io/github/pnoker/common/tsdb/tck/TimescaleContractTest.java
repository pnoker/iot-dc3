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
import io.github.pnoker.common.tsdb.timescale.TimescaleTsdbStore;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Objects;

/**
 * Timescale reference harness for the store-neutral time-series contract suite —
 * runs against {@code timescale/timescaledb-ha:pg18} via Testcontainers.
 *
 * @author pnoker
 * @since 2026.8.20
 */
@Testcontainers(disabledWithoutDocker = true)
class TimescaleContractTest extends AbstractTsdbContractTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("timescale/timescaledb-ha:pg18")
                    .asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("tsdb")
                    .withUsername("tsdb")
                    .withPassword("tsdb");

    private static volatile TsdbStore store;

    @Override
    protected TsdbStore store() {
        if (Objects.isNull(store)) {
            PGSimpleDataSource dataSource = new PGSimpleDataSource();
            dataSource.setURL(POSTGRES.getJdbcUrl());
            dataSource.setUser(POSTGRES.getUsername());
            dataSource.setPassword(POSTGRES.getPassword());
            store = new TimescaleTsdbStore(dataSource);
        }
        return store;
    }

    /**
     * The retention TCK case (design §10.10) needs clock manipulation the container
     * cannot provide honestly; timescale retention is asserted by the app's seed DDL
     * and E2E instead.
     */
    @Test
    void retentionPlaceholder() {
        // documented non-coverage: retention tested at deployment level (seed SQL)
    }
}
