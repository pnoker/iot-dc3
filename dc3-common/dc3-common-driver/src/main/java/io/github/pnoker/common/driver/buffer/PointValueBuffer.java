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

package io.github.pnoker.common.driver.buffer;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.pnoker.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SQLite-backed DAO for the local point-value buffer. Owns a single-connection HikariCP
 * pool (SQLite is a single-writer database) in WAL mode and exposes the small CRUD
 * surface the buffer service needs.
 *
 * <p>All times are epoch seconds; the schema deliberately avoids date types so the store
 * is free of timezone/format pitfalls.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.6.2
 */
@Slf4j
public class PointValueBuffer {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS point_value_buffer (
                id              TEXT PRIMARY KEY,
                device_id       INTEGER NOT NULL,
                point_id        INTEGER NOT NULL,
                driver_id       INTEGER,
                tenant_id       INTEGER,
                payload_json    TEXT NOT NULL,
                routing_key     TEXT NOT NULL,
                attempt         INTEGER NOT NULL DEFAULT 0,
                next_attempt_at INTEGER NOT NULL,
                created_at      INTEGER NOT NULL
            )
            """;
    private static final String CREATE_INDEX_NEXT_SQL =
            "CREATE INDEX IF NOT EXISTS idx_buffer_next_attempt ON point_value_buffer(next_attempt_at)";
    private static final String CREATE_INDEX_CREATED_SQL =
            "CREATE INDEX IF NOT EXISTS idx_buffer_created ON point_value_buffer(created_at)";

    private static final String UPSERT_SQL = """
            INSERT OR REPLACE INTO point_value_buffer
                (id, device_id, point_id, driver_id, tenant_id, payload_json, routing_key, attempt, next_attempt_at, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String SELECT_PENDING_SQL = """
            SELECT id, device_id, point_id, driver_id, tenant_id, payload_json, routing_key, attempt, next_attempt_at, created_at
            FROM point_value_buffer
            WHERE next_attempt_at <= ?
            ORDER BY next_attempt_at ASC
            LIMIT ?
            """;
    private static final String DELETE_SQL = "DELETE FROM point_value_buffer WHERE id = ?";
    private static final String MARK_RETRY_SQL =
            "UPDATE point_value_buffer SET attempt = ?, next_attempt_at = ? WHERE id = ?";
    private static final String DELETE_OLDEST_SQL = """
            DELETE FROM point_value_buffer WHERE id IN (
                SELECT id FROM point_value_buffer ORDER BY created_at ASC LIMIT ?
            )
            """;
    private static final String COUNT_SQL = "SELECT COUNT(*) FROM point_value_buffer";

    private final String dbPath;
    private HikariDataSource dataSource;

    public PointValueBuffer(String dbPath) {
        this.dbPath = dbPath;
    }

    /**
     * Open the connection pool, apply WAL, and create the buffer table. Idempotent.
     */
    public void initialize() {
        File parent = new File(dbPath).getParentFile();
        if (Objects.nonNull(parent) && !parent.exists() && !parent.mkdirs()) {
            throw new ServiceException("Failed to create buffer db parent directory: " + parent.getAbsolutePath());
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbPath);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.setConnectionInitSql("PRAGMA journal_mode=WAL; PRAGMA synchronous=NORMAL;");
        config.setPoolName("dc3-driver-buffer");
        this.dataSource = new HikariDataSource(config);
        createTableIfNotExists();
        log.info("Point value buffer initialized, dbPath={}", dbPath);
    }

    private void createTableIfNotExists() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_TABLE_SQL);
            stmt.execute(CREATE_INDEX_NEXT_SQL);
            stmt.execute(CREATE_INDEX_CREATED_SQL);
        } catch (SQLException e) {
            throw new ServiceException("Failed to init point-value buffer table", e);
        }
    }

    /**
     * Insert or replace a buffered record keyed by the correlation id.
     */
    public void upsert(BufferedPointValue record) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
            ps.setString(1, record.id());
            setLong(ps, 2, record.deviceId());
            setLong(ps, 3, record.pointId());
            setLong(ps, 4, record.driverId());
            setLong(ps, 5, record.tenantId());
            ps.setString(6, record.payloadJson());
            ps.setString(7, record.routingKey());
            ps.setInt(8, record.attempt());
            ps.setLong(9, record.nextAttemptAt());
            ps.setLong(10, record.createdAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Buffer upsert failed, id={}, attempt={}", record.id(), record.attempt(), e);
        }
    }

    /**
     * Return up to {@code batchSize} records due for republish (next_attempt_at &lt;= now),
     * oldest-first.
     */
    public List<BufferedPointValue> selectPending(int batchSize, long nowEpochSec) {
        List<BufferedPointValue> records = new ArrayList<>(batchSize);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_PENDING_SQL)) {
            ps.setLong(1, nowEpochSec);
            ps.setInt(2, batchSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(new BufferedPointValue(
                            rs.getString("id"),
                            rs.getLong("device_id"),
                            rs.getLong("point_id"),
                            getNullableLong(rs, "driver_id"),
                            getNullableLong(rs, "tenant_id"),
                            rs.getString("payload_json"),
                            rs.getString("routing_key"),
                            rs.getInt("attempt"),
                            rs.getLong("next_attempt_at"),
                            rs.getLong("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Buffer selectPending failed", e);
        }
        return records;
    }

    /**
     * Delete a record after it has left the channel cleanly.
     */
    public void delete(String id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Buffer delete failed, id={}", id, e);
        }
    }

    /**
     * Bump the attempt counter and push back the next retry time after a failed republish.
     */
    public void markRetry(String id, int attempt, long nextAttemptAt) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(MARK_RETRY_SQL)) {
            ps.setInt(1, attempt);
            ps.setLong(2, nextAttemptAt);
            ps.setString(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Buffer markRetry failed, id={}, attempt={}", id, attempt, e);
        }
    }

    /**
     * Delete the {@code evictBatch} oldest records (by created_at) for capacity enforcement.
     *
     * @return number of records deleted
     */
    public int deleteOldest(int evictBatch) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_OLDEST_SQL)) {
            ps.setInt(1, evictBatch);
            return ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Buffer deleteOldest failed", e);
            return 0;
        }
    }

    /**
     * @return current number of buffered records
     */
    public long count() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(COUNT_SQL)) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            log.error("Buffer count failed", e);
            return 0;
        }
    }

    /**
     * @return on-disk size of the SQLite database file in bytes
     */
    public long fileSize() {
        File file = new File(dbPath);
        return file.exists() ? file.length() : 0;
    }

    /**
     * Close the connection pool.
     */
    public void close() {
        if (Objects.nonNull(dataSource) && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private static void setLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (Objects.isNull(value)) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setLong(index, value);
        }
    }

    private static Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
