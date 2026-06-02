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
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.ConnectorException;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.WritePointException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.6.2
 */
@Slf4j
public abstract class AbstractJdbcDriverCustomService implements DriverCustomService {

    protected final DriverSenderService driverSenderService;

    @Value("${dc3.driver.code}")
    protected String driverCode;

    /**
     * Cache of device ID to HikariDataSource connections.
     */
    protected Map<Long, HikariDataSource> connectMap;

    protected AbstractJdbcDriverCustomService(DriverSenderService driverSenderService) {
        this.driverSenderService = driverSenderService;
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

    @Override
    public void initial() {
        connectMap = new ConcurrentHashMap<>(16);
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
                    removed.close();
                    log.info("Driver connection pool destroyed, protocol={}, deviceId={}, operateType={}",
                            driverCode, metadataEvent.getId(), operateType);
                }
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}, pointId={}",
                    driverCode, metadataType, operateType, metadataEvent.getId());
        }
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                               DeviceBO device, PointBO point) {
        String readQuery = getRequiredConfig(pointConfig, "readQuery");
        HikariDataSource ds = getConnector(device.getId(), driverConfig);
        try {
            String value = executeReadQuery(ds, readQuery);
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
        String resolvedQuery = writeQuery.replace("${value}", writePointValue.getValue(String.class));
        HikariDataSource ds = getConnector(device.getId(), driverConfig);
        try {
            return executeWriteQuery(ds, resolvedQuery);
        } catch (WritePointException e) {
            invalidateConnector(device.getId(), ds);
            throw e;
        }
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
            String host = getConfigValue(driverConfig, "host", "localhost");
            int port = getConfigIntValue(driverConfig, "port", getDefaultPort());
            String database = getRequiredConfig(driverConfig, "database");
            String username = getConfigValue(driverConfig, "username", "root");
            String password = getConfigValue(driverConfig, "password", "");
            int queryTimeout = getConfigIntValue(driverConfig, "queryTimeout", 30);

            String jdbcUrl = buildJdbcUrl(driverConfig);
            log.debug("Driver connection pool creating, protocol={}, deviceId={}, jdbcUrl={}", driverCode, deviceId, jdbcUrl);

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setDriverClassName(getDriverClassName());
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(5);
            config.setMinimumIdle(1);
            config.setConnectionTimeout(queryTimeout * 1000L);
            config.setMaxLifetime(1800000);
            config.setKeepaliveTime(300000);
            config.setPoolName("dc3-" + driverCode + "-" + deviceId);

            try {
                HikariDataSource ds = new HikariDataSource(config);
                log.info("Driver connection pool established, protocol={}, deviceId={}, jdbcUrl={}", driverCode, deviceId, jdbcUrl);
                return ds;
            } catch (Exception e) {
                log.error("Driver connection pool failed, protocol={}, deviceId={}, jdbcUrl={}", driverCode, deviceId, jdbcUrl, e);
                throw new ConnectorException("Driver connection pool failed, protocol={}, deviceId={}, message={}",
                        driverCode, deviceId, e.getMessage(), e);
            }
        });
    }

    /**
     * Execute a read SQL query and return the first column of the first row as a string.
     *
     * @param ds        active HikariDataSource
     * @param readQuery SQL SELECT query
     * @return query result as string
     * @throws ReadPointException if query execution fails
     */
    protected String executeReadQuery(HikariDataSource ds, String readQuery) {
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(readQuery);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                Object value = rs.getObject(1);
                return Objects.nonNull(value) ? value.toString() : null;
            }
            return null;
        } catch (SQLException e) {
            log.error("Driver SQL read failed, protocol={}, query={}", driverCode, readQuery, e);
            throw new ReadPointException("Driver SQL read failed, protocol={}, query={}, message={}",
                    driverCode, readQuery, e.getMessage(), e);
        }
    }

    /**
     * Execute a write SQL query (UPDATE/INSERT/DELETE).
     *
     * @param ds         active HikariDataSource
     * @param writeQuery SQL write query
     * @return true if at least one row was affected
     * @throws WritePointException if query execution fails
     */
    protected boolean executeWriteQuery(HikariDataSource ds, String writeQuery) {
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(writeQuery)) {
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
        try {
            if (Objects.nonNull(ds) && !ds.isClosed()) {
                ds.close();
            }
        } catch (Exception e) {
            log.warn("Driver connection pool destroy failed, protocol={}, deviceId={}", driverCode, deviceId, e);
        }
    }

    /**
     * Return the default port for this database type.
     *
     * @return default port number
     */
    protected abstract int getDefaultPort();

    /**
     * Get a required configuration value, throwing an exception if missing.
     *
     * @param config attribute configuration map
     * @param code   attribute code
     * @return configuration value
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
     * @param defaultValue default value if attribute is missing
     * @return configuration value or default
     */
    protected int getConfigIntValue(Map<String, AttributeBO> config, String code, int defaultValue) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue())) {
            return defaultValue;
        }
        return attr.getValue(Integer.class);
    }
}
