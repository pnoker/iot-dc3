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

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

/**
 * PostgreSQL harness — postgres:18 with the canonical seed mounted at the
 * entrypoint's initdb directory. The connection selects the dc3_data schema so
 * cross-schema references (dc3_manager.*) resolve exactly as the services see
 * them.
 *
 * @author pnoker
 * @since 2026.8.24
 */
@Testcontainers(disabledWithoutDocker = true)
class PostgresDialectContractIT extends AbstractDbDialectContractTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(org.testcontainers.utility.DockerImageName
                    .parse("timescale/timescaledb-ha:pg18")
                    .asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("dc3")
                    .withUsername("dc3")
                    .withPassword("dc3")
                    // 00-iot-dc3-extensions (age, pgvector) is absent from this
                    // image — only the production base image carries those; the
                    // dialect contract does not touch them.
                    .withCopyFileToContainer(
                            MountableFile.forHostPath("../../dc3/dependencies/postgres/initdb/02-iot-dc3-auth.sql"),
                            "/docker-entrypoint-initdb.d/02-iot-dc3-auth.sql")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath("../../dc3/dependencies/postgres/initdb/03-iot-dc3-data.sql"),
                            "/docker-entrypoint-initdb.d/03-iot-dc3-data.sql")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath("../../dc3/dependencies/postgres/initdb/04-iot-dc3-manager.sql"),
                            "/docker-entrypoint-initdb.d/04-iot-dc3-manager.sql")
                    .withCopyFileToContainer(MountableFile.forHostPath(latestOnlySeed()),
                            "/docker-entrypoint-initdb.d/05-iot-dc3-history.sql")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath("../../dc3/dependencies/postgres/initdb/06-iot-dc3-agentic.sql"),
                            "/docker-entrypoint-initdb.d/06-iot-dc3-agentic.sql")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath("../../dc3/dependencies/postgres/initdb/08-iot-dc3-runtime.sql"),
                            "/docker-entrypoint-initdb.d/08-iot-dc3-runtime.sql")
                    // log-following is unreliable against podman; the listening
                    // TCP port (5432) flips when the postmaster is ready
                    .waitingFor(org.testcontainers.containers.wait.strategy.Wait.forListeningPort()
                            .withStartupTimeout(java.time.Duration.ofMinutes(5)));

    /**
     * The hypertable/cagg half of 05 needs the production base image's
     * TimescaleDB function set; the dialect contract only exercises the
     * relational {@code dc3_point_latest} projection — extract exactly that.
     */
    private static String latestOnlySeed() {
        try {
            String seed = java.nio.file.Files.readString(java.nio.file.Path.of(
                    "../../dc3/dependencies/postgres/initdb/05-iot-dc3-history.sql"));
            int start = seed.indexOf("CREATE TABLE dc3_point_latest");
            int end = seed.indexOf(";", seed.indexOf("idx_point_latest_driver")) + 1;
            java.nio.file.Path trimmed = java.nio.file.Files.createTempFile("dc3-tck-05", ".sql");
            java.nio.file.Files.writeString(trimmed, "CREATE SCHEMA IF NOT EXISTS dc3_history;\nSET search_path TO dc3_history, public;\n"
                    + seed.substring(start, end) + "\n");
            // createTempFile defaults to 0600 — the container's psql user must read it
            java.nio.file.Files.setPosixFilePermissions(trimmed, java.nio.file.attribute.PosixFilePermissions.fromString("rw-r--r--"));
            return trimmed.toAbsolutePath().toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected String r2dbcUrl() {
        return "r2dbc:postgresql://dc3:dc3@" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432) + "/dc3";
    }

    @Override
    protected String dialectName() {
        return "postgres";
    }

    @Override
    protected String fingerprintTable() {
        return "public.dc3_schema_fingerprint";
    }

    @Override
    protected String operationTable() {
        return "public.dc3_point_value_ingest_outbox";
    }

    @Override
    protected String alarmTable() {
        return "dc3_data.dc3_entity_alarm";
    }

    @Override
    protected String notifyHistoryTable() {
        return "dc3_data.dc3_notify_history";
    }
}
