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

package io.github.pnoker.common.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.pnoker.common.driver.entity.bean.DeviceHealthState;
import io.github.pnoker.common.driver.entity.bean.DriverHealthState;
import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bean.ValidationReport;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.enums.RwTypeEnum;
import io.github.pnoker.common.exception.ConnectorException;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.WritePointException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base class for JDBC-based database driver implementations.
 * <p>
 * Provides shared connection pool management (HikariCP), SQL execution,
 * and the standard driver lifecycle for MySQL, PostgreSQL, Oracle, and SQL Server drivers.
 * Subclasses only need to provide JDBC URL construction and driver class name.
 * </p>
 * <p>
 * Configuration contract (driver attributes):
 * </p>
 * <ul>
 * <li>{@code host} — database host, default {@code localhost}</li>
 * <li>{@code port} — database port, default is dialect-specific</li>
 * <li>{@code username} — login name, default {@code root}</li>
 * <li>{@code password} — login password, default empty</li>
 * <li>{@code queryTimeout} — SQL statement timeout in seconds, default {@code 30},
 * applied via {@link PreparedStatement#setQueryTimeout(int)} to every read and write</li>
 * </ul>
 * <p>
 * Point configuration contract:
 * </p>
 * <ul>
 * <li>{@code readQuery} — SELECT executed for reads; must not contain {@code ?} placeholders
 * because the driver binds no read parameters</li>
 * <li>{@code writeQuery} — UPDATE/INSERT/DELETE executed for writes; must contain exactly one
 * {@code ?} placeholder for the written value, which is bound via
 * {@link PreparedStatement#setString} so a malicious point value cannot alter the statement</li>
 * </ul>
 * <p>
 * {@link #validatePoint} derives the required queries from the point's {@link RwTypeEnum} flag:
 * read-only points need {@code readQuery}, write-only points need {@code writeQuery}, and
 * read-write points need both. A point with an unknown flag is treated as read-only.
 * </p>
 *
 * @author pnoker
 * @since 2026.6.2
 */
@Slf4j
public abstract class AbstractJdbcDriverCustomService implements DriverCustomService {

    @Value("${dc3.driver.code}")
    protected String driverCode;

    /**
     * Cache of device ID to HikariDataSource connection pools.
     */
    protected Map<Long, HikariDataSource> connectMap;

    /**
     * Base constructors take no dependencies: connection management and query execution
     * are self-contained, and subclasses only contribute dialect specifics.
     */
    protected AbstractJdbcDriverCustomService() {
    }

    /**
     * Build the JDBC URL from driver configuration attributes.
     *
     * @param driverConfig driver attribute configuration map
     * @return JDBC connection URL
     */
    protected abstract String buildJdbcUrl(Map<String, AttributeBO> driverConfig);

    /**
     * Return the JDBC driver class name.
     *
     * @return fully qualified driver class name
     */
    protected abstract String getDriverClassName();

    /**
     * Return the default port for this database type.
     *
     * @return default port number
     */
    protected abstract int getDefaultPort();

    @Override
    public void initial() {
        if (Objects.isNull(connectMap)) {
            connectMap = new ConcurrentHashMap<>(16);
        }
    }

    @Override
    public void schedule() {
        // Database drivers do not need custom scheduled tasks.
    }

    @Override
    public DriverHealthState health() {
        return DriverHealthState.online();
    }

    @Override
    public DeviceHealthState health(Map<String, AttributeBO> driverConfig, DeviceBO device) {
        if (Objects.isNull(device) || Objects.isNull(device.getId())) {
            return DeviceHealthState.offline();
        }
        try {
            HikariDataSource ds = getConnector(device.getId(), driverConfig);
            try (Connection conn = ds.getConnection()) {
                return conn.isValid(5) ? DeviceHealthState.online() : DeviceHealthState.offline();
            }
        } catch (Exception e) {
            log.warn("Driver health check failed, protocol={}, deviceId={}", driverCode, device.getId(), e);
            return DeviceHealthState.offline();
        }
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}, deviceId={}",
                    driverCode, metadataType, operateType, metadataEvent.getId());

            if (MetadataOperateTypeEnum.DELETE.equals(operateType)
                    || MetadataOperateTypeEnum.UPDATE.equals(operateType)) {
                HikariDataSource removed = connectMap.remove(metadataEvent.getId());
                if (Objects.nonNull(removed)) {
                    closeQuietly(metadataEvent.getId(), removed);
                    log.info("Driver connection pool destroyed, protocol={}, deviceId={}, operateType={}",
                            driverCode, metadataEvent.getId(), operateType);
                }
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}, pointId={}",
                    driverCode, metadataType, operateType, metadataEvent.getId());
        } else if (MetadataTypeEnum.DRIVER.equals(metadataType)) {
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}",
                    driverCode, metadataType, operateType);

            if (MetadataOperateTypeEnum.DELETE.equals(operateType)
                    || MetadataOperateTypeEnum.UPDATE.equals(operateType)) {
                int count = invalidateAllConnectors();
                log.info("Driver connection pools destroyed, protocol={}, count={}, operateType={}",
                        driverCode, count, operateType);
            }
        }
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                               DeviceBO device, PointBO point) {
        String readQuery = getRequiredConfig(pointConfig, "readQuery");
        if (countPlaceholders(readQuery) != 0) {
            throw new ReadPointException(
                    "Driver SQL read query must not contain '?' placeholders, protocol={}, query={}",
                    driverCode, readQuery);
        }
        int queryTimeout = getConfigIntValue(driverConfig, "queryTimeout", 30);
        HikariDataSource ds = getConnector(device.getId(), driverConfig);
        try {
            String value = executeReadQuery(ds, readQuery, queryTimeout);
            return new ReadPointValue(device, point, value);
        } catch (ReadPointException e) {
            invalidateConnector(device.getId(), ds);
            throw e;
        }
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                         DeviceBO device, PointBO point, WritePointValue writePointValue) {
        String writeQuery = getRequiredConfig(pointConfig, "writeQuery");
        if (countPlaceholders(writeQuery) != 1) {
            throw new WritePointException(
                    "Driver SQL write query must contain exactly one '?' placeholder, protocol={}, query={}",
                    driverCode, writeQuery);
        }
        String value = writePointValue.getValue(String.class);
        int queryTimeout = getConfigIntValue(driverConfig, "queryTimeout", 30);
        HikariDataSource ds = getConnector(device.getId(), driverConfig);
        try {
            return executeWriteQuery(ds, writeQuery, value, queryTimeout);
        } catch (WritePointException e) {
            invalidateConnector(device.getId(), ds);
            throw e;
        }
    }

    /**
     * Shared point configuration validation for all JDBC dialects.
     * <p>
     * Derives the required queries from the point's read/write flag, then verifies the
     * placeholder shape of every supplied query against what the execution path supports:
     * reads bind nothing (no {@code ?} allowed), writes bind exactly one value.
     * </p>
     *
     * @param pointConfig point attribute configuration map
     * @param point       point business object, may be {@code null}
     * @return validation report, never {@code null}
     */
    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();

        RwTypeEnum rwFlag = Objects.isNull(point) ? null : point.getRwFlag();
        boolean readRequired = Objects.isNull(rwFlag) || RwTypeEnum.READ_ONLY.equals(rwFlag)
                || RwTypeEnum.READ_WRITE.equals(rwFlag);
        boolean writeRequired = RwTypeEnum.WRITE_ONLY.equals(rwFlag) || RwTypeEnum.READ_WRITE.equals(rwFlag);

        if (readRequired) {
            checkRequired(pointConfig, "readQuery", issues);
        }
        if (writeRequired) {
            checkRequired(pointConfig, "writeQuery", issues);
        }

        String readQuery = getRawValue(pointConfig, "readQuery");
        if (Objects.nonNull(readQuery) && countPlaceholders(readQuery) != 0) {
            issues.add(issue("readQuery",
                    "Read query must not contain '?' placeholders because the driver binds no read parameters"));
        }
        String writeQuery = getRawValue(pointConfig, "writeQuery");
        if (Objects.nonNull(writeQuery) && countPlaceholders(writeQuery) != 1) {
            issues.add(issue("writeQuery",
                    "Write query must contain exactly one '?' placeholder for the written value"));
        }

        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues)
                .build();
    }

    /**
     * Get or create a HikariDataSource connection pool for the given device.
     *
     * @param deviceId     unique device identifier
     * @param driverConfig driver configuration containing database connection parameters
     * @return cached or newly created HikariDataSource
     * @throws ConnectorException if connection pool creation fails
     */
    protected HikariDataSource getConnector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        return connectMap.computeIfAbsent(deviceId, id -> {
            String username = getConfigValue(driverConfig, "username", "root");
            String password = getConfigValue(driverConfig, "password", "");

            String jdbcUrl = buildJdbcUrl(driverConfig);
            log.debug("Driver connection pool creating, protocol={}, deviceId={}, jdbcUrl={}",
                    driverCode, deviceId, jdbcUrl);

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setDriverClassName(getDriverClassName());
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(5);
            config.setMinimumIdle(1);
            config.setMaxLifetime(1800000);
            config.setKeepaliveTime(300000);
            config.setPoolName("dc3-" + driverCode + "-" + deviceId);

            try {
                HikariDataSource ds = new HikariDataSource(config);
                log.info("Driver connection pool established, protocol={}, deviceId={}, jdbcUrl={}",
                        driverCode, deviceId, jdbcUrl);
                return ds;
            } catch (Exception e) {
                log.error("Driver connection pool failed, protocol={}, deviceId={}, jdbcUrl={}",
                        driverCode, deviceId, jdbcUrl, e);
                throw new ConnectorException("Driver connection pool failed, protocol={}, deviceId={}, message={}",
                        driverCode, deviceId, e.getMessage(), e);
            }
        });
    }

    /**
     * Execute a read SQL query and return the first column of the first row as a string.
     * <p>
     * Returns {@code null} when the query yields no row or the value is SQL {@code NULL}.
     * </p>
     *
     * @param ds           active HikariDataSource
     * @param readQuery    SQL SELECT query without {@code ?} placeholders
     * @param queryTimeout statement timeout in seconds
     * @return query result as string, or {@code null}
     * @throws ReadPointException if query execution fails
     */
    protected String executeReadQuery(HikariDataSource ds, String readQuery, int queryTimeout) {
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(readQuery)) {
            ps.setQueryTimeout(queryTimeout);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Object value = rs.getObject(1);
                    return Objects.nonNull(value) ? value.toString() : null;
                }
                return null;
            }
        } catch (SQLException e) {
            log.error("Driver SQL read failed, protocol={}, query={}", driverCode, readQuery, e);
            throw new ReadPointException("Driver SQL read failed, protocol={}, query={}, message={}",
                    driverCode, readQuery, e.getMessage(), e);
        }
    }

    /**
     * Execute a write SQL query (UPDATE/INSERT/DELETE), binding the point value as a parameter.
     * <p>
     * The {@code writeQuery} must use a single {@code ?} placeholder for the written value; the
     * value is bound via {@link PreparedStatement#setString} rather than concatenated into the SQL,
     * so a malicious point value cannot alter the statement (no SQL injection).
     * </p>
     *
     * @param ds           active HikariDataSource
     * @param writeQuery   SQL write query containing exactly one {@code ?} value placeholder
     * @param value        point value to bind to the placeholder
     * @param queryTimeout statement timeout in seconds
     * @return true if at least one row was affected
     * @throws WritePointException if query execution fails
     */
    protected boolean executeWriteQuery(HikariDataSource ds, String writeQuery, String value, int queryTimeout) {
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(writeQuery)) {
            ps.setQueryTimeout(queryTimeout);
            ps.setString(1, value);
            int rows = ps.executeUpdate();
            log.debug("Driver SQL write executed, protocol={}, rows={}", driverCode, rows);
            return rows > 0;
        } catch (SQLException e) {
            log.error("Driver SQL write failed, protocol={}, query={}", driverCode, writeQuery, e);
            throw new WritePointException("Driver SQL write failed, protocol={}, query={}, message={}",
                    driverCode, writeQuery, e.getMessage(), e);
        }
    }

    /**
     * Remove and close a HikariDataSource from the connection cache.
     *
     * @param deviceId device identifier
     * @param ds       the data source to invalidate
     */
    protected void invalidateConnector(Long deviceId, HikariDataSource ds) {
        connectMap.remove(deviceId, ds);
        closeQuietly(deviceId, ds);
    }

    /**
     * Remove and close every cached connection pool.
     *
     * @return number of destroyed pools
     */
    protected int invalidateAllConnectors() {
        int count = 0;
        for (Long deviceId : connectMap.keySet()) {
            HikariDataSource removed = connectMap.remove(deviceId);
            if (Objects.nonNull(removed)) {
                closeQuietly(deviceId, removed);
                count++;
            }
        }
        return count;
    }

    /**
     * Close a data source, never throwing to the caller.
     *
     * @param deviceId device identifier, for logging
     * @param ds       the data source to close, may be {@code null}
     */
    protected void closeQuietly(Long deviceId, HikariDataSource ds) {
        try {
            if (Objects.nonNull(ds) && !ds.isClosed()) {
                ds.close();
            }
        } catch (Exception e) {
            log.warn("Driver connection pool destroy failed, protocol={}, deviceId={}", driverCode, deviceId, e);
        }
    }

    /**
     * Get a required configuration value, throwing an exception if missing.
     *
     * @param config attribute configuration map
     * @param code   attribute code
     * @return configuration value
     * @throws ConnectorException if the attribute is missing or empty
     */
    protected String getRequiredConfig(Map<String, AttributeBO> config, String code) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue()) || attr.getValue().isEmpty()) {
            throw new ConnectorException("Required driver attribute '{}' is missing", code);
        }
        return attr.getValue(String.class);
    }

    /**
     * Get an optional configuration value with a default.
     *
     * @param config       attribute configuration map
     * @param code         attribute code
     * @param defaultValue default value if attribute is missing or empty
     * @return configuration value or default
     */
    protected String getConfigValue(Map<String, AttributeBO> config, String code, String defaultValue) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue()) || attr.getValue().isEmpty()) {
            return defaultValue;
        }
        return attr.getValue(String.class);
    }

    /**
     * Get an optional integer configuration value with a default.
     *
     * @param config       attribute configuration map
     * @param code         attribute code
     * @param defaultValue default value if attribute is missing or empty
     * @return configuration value or default
     * @throws ConnectorException if the attribute value cannot be parsed as an integer
     */
    protected int getConfigIntValue(Map<String, AttributeBO> config, String code, int defaultValue) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue()) || attr.getValue().isEmpty()) {
            return defaultValue;
        }
        try {
            return attr.getValue(Integer.class);
        } catch (RuntimeException e) {
            throw new ConnectorException("Invalid integer driver attribute '{}', value={}",
                    code, attr.getValue(), e);
        }
    }

    /**
     * Record a required-attribute error in the shared validation report shape.
     *
     * @param config attribute configuration map
     * @param code   attribute code to require
     * @param issues mutable issue list to append to
     */
    protected static void checkRequired(Map<String, AttributeBO> config, String code,
                                        List<ValidationReport.AttributeIssue> issues) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue()) || attr.getValue().isEmpty()) {
            issues.add(issue(code, "Missing required attribute: " + code));
        }
    }

    /**
     * Build an error-level attribute issue.
     *
     * @param code    attribute code
     * @param message human-readable description
     * @return error issue
     */
    protected static ValidationReport.AttributeIssue issue(String code, String message) {
        return ValidationReport.AttributeIssue.builder()
                .attributeCode(code)
                .level(ValidationReport.IssueLevel.ERROR)
                .message(message)
                .build();
    }

    /**
     * Count {@code ?} placeholder occurrences in SQL text.
     * <p>
     * The count is textual: a {@code ?} inside a string literal or comment is still counted,
     * which is acceptable for configuration validation.
     * </p>
     *
     * @param sql SQL text
     * @return placeholder count
     */
    protected static int countPlaceholders(String sql) {
        int count = 0;
        for (int index = sql.indexOf('?'); index >= 0; index = sql.indexOf('?', index + 1)) {
            count++;
        }
        return count;
    }

    /**
     * Return the raw string value of an optional attribute, or {@code null} when absent or empty.
     *
     * @param config attribute configuration map
     * @param code   attribute code
     * @return raw value or {@code null}
     */
    protected static String getRawValue(Map<String, AttributeBO> config, String code) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue()) || attr.getValue().isEmpty()) {
            return null;
        }
        return attr.getValue(String.class);
    }
}
