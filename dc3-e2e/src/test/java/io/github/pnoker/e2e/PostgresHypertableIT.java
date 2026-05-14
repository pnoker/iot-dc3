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

package io.github.pnoker.e2e;

import io.github.pnoker.e2e.harness.BaseE2eIT;
import io.github.pnoker.e2e.harness.E2eStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the assumptions production point-value persistence relies on:
 * - the dc3 schema can mount the timescaledb extension
 * - the dc3 schema can mount the pgvector extension
 * - a hypertable on (time, device_id, point_id) accepts batched inserts and
 * retains them across queries
 * - a continuous-aggregate-style time_bucket query returns expected aggregates
 * <p>
 * Disabled by default; opt in with {@code DC3_E2E=true}.
 */
@EnabledIfEnvironmentVariable(named = "DC3_E2E", matches = "(?i)true|1|yes|on")
class PostgresHypertableIT extends BaseE2eIT {

    @Test
    void timescaleHypertableAcceptsAndAggregatesPointValues() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                E2eStack.postgresJdbcUrl(), E2eStack.postgresUsername(), E2eStack.postgresPassword())) {
            try (Statement ddl = conn.createStatement()) {
                ddl.execute("CREATE EXTENSION IF NOT EXISTS timescaledb");
                ddl.execute("DROP TABLE IF EXISTS dc3_point_value_e2e");
                ddl.execute("""
                        CREATE TABLE dc3_point_value_e2e (
                            time        TIMESTAMPTZ NOT NULL,
                            device_id   BIGINT      NOT NULL,
                            point_id    BIGINT      NOT NULL,
                            raw_value   TEXT,
                            cal_value   DOUBLE PRECISION
                        )""");
                ddl.execute("SELECT create_hypertable('dc3_point_value_e2e', 'time')");
            }

            Instant base = Instant.now().truncatedTo(ChronoUnit.MINUTES).minusSeconds(60);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO dc3_point_value_e2e(time, device_id, point_id, raw_value, cal_value)"
                            + " VALUES (?, ?, ?, ?, ?)")) {
                for (int i = 0; i < 10; i++) {
                    ps.setTimestamp(1, Timestamp.from(base.plusSeconds(i * 5L)));
                    ps.setLong(2, 1L);
                    ps.setLong(3, 100L);
                    ps.setString(4, String.valueOf(20.0 + i));
                    ps.setDouble(5, 20.0 + i);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT time_bucket('1 minute', time), count(*), avg(cal_value), min(cal_value),"
                                 + " max(cal_value) FROM dc3_point_value_e2e"
                                 + " WHERE device_id = 1 AND point_id = 100 GROUP BY 1")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(2)).isEqualTo(10);
                assertThat(rs.getDouble(3)).isEqualTo(24.5, org.assertj.core.data.Offset.offset(0.0001));
                assertThat(rs.getDouble(4)).isEqualTo(20.0);
                assertThat(rs.getDouble(5)).isEqualTo(29.0);
                assertThat(rs.next()).isFalse();
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT count(*) FROM timescaledb_information.hypertables"
                                 + " WHERE hypertable_name = 'dc3_point_value_e2e'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(1);
            }
        }
    }

    @Test
    void pgvectorStoresIndexesAndRanksEmbeddings() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                E2eStack.postgresJdbcUrl(), E2eStack.postgresUsername(), E2eStack.postgresPassword())) {
            try (Statement ddl = conn.createStatement()) {
                ddl.execute("CREATE EXTENSION IF NOT EXISTS timescaledb");
                ddl.execute("CREATE EXTENSION IF NOT EXISTS vector");
                ddl.execute("DROP TABLE IF EXISTS dc3_embedding_e2e");
                ddl.execute("""
                        CREATE TABLE dc3_embedding_e2e (
                            id        BIGINT PRIMARY KEY,
                            name      TEXT      NOT NULL,
                            embedding VECTOR(3) NOT NULL
                        )""");
                ddl.execute("""
                        CREATE INDEX dc3_embedding_e2e_hnsw_idx
                        ON dc3_embedding_e2e USING hnsw (embedding vector_l2_ops)
                        """);
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO dc3_embedding_e2e(id, name, embedding) VALUES (?, ?, ?::vector)")) {
                ps.setLong(1, 1L);
                ps.setString(2, "temperature");
                ps.setString(3, "[0.10,0.20,0.30]");
                ps.addBatch();

                ps.setLong(1, 2L);
                ps.setString(2, "humidity");
                ps.setString(3, "[0.90,0.10,0.10]");
                ps.addBatch();

                ps.setLong(1, 3L);
                ps.setString(2, "pressure");
                ps.setString(3, "[0.20,0.80,0.90]");
                ps.addBatch();

                ps.executeBatch();
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT count(*) FROM pg_extension WHERE extname IN ('timescaledb', 'vector')")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(2);
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT vector_dims(embedding) FROM dc3_embedding_e2e WHERE id = 1")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(3);
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT name, embedding <-> '[0.11,0.19,0.31]'::vector AS distance"
                                 + " FROM dc3_embedding_e2e ORDER BY embedding <-> '[0.11,0.19,0.31]'::vector"
                                 + " LIMIT 1")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("temperature");
                assertThat(rs.getDouble(2)).isLessThan(0.03);
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT indexdef FROM pg_indexes"
                                 + " WHERE tablename = 'dc3_embedding_e2e'"
                                 + " AND indexname = 'dc3_embedding_e2e_hnsw_idx'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).contains("USING hnsw", "vector_l2_ops");
            }
        }
    }

    @Test
    void hypertableEnforcesNotNullOnTimeColumn() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                E2eStack.postgresJdbcUrl(), E2eStack.postgresUsername(), E2eStack.postgresPassword())) {
            try (Statement ddl = conn.createStatement()) {
                ddl.execute("CREATE EXTENSION IF NOT EXISTS timescaledb");
                ddl.execute("DROP TABLE IF EXISTS dc3_point_value_e2e_strict");
                ddl.execute("""
                        CREATE TABLE dc3_point_value_e2e_strict (
                            time      TIMESTAMPTZ NOT NULL,
                            device_id BIGINT      NOT NULL,
                            value     DOUBLE PRECISION
                        )""");
                ddl.execute("SELECT create_hypertable('dc3_point_value_e2e_strict', 'time')");
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO dc3_point_value_e2e_strict(time, device_id, value) VALUES (?, ?, ?)")) {
                ps.setTimestamp(1, null);
                ps.setLong(2, 1L);
                ps.setDouble(3, 1.0);
                java.sql.SQLException caught = org.junit.jupiter.api.Assertions.assertThrows(
                        java.sql.SQLException.class, ps::executeUpdate);
                assertThat(caught.getMessage().toLowerCase()).contains("null");
            }
        }
    }
}
