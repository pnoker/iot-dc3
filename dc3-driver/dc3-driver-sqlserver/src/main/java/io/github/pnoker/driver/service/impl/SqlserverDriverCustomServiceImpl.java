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

package io.github.pnoker.driver.service.impl;

import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.sql.AbstractJdbcDriverCustomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Custom driver service implementation for the SQL Server driver.
 * <p>
 * Extends the abstract JDBC driver service to provide SQL Server-specific
 * connection URL construction and driver class configuration.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.6.2
 */
@Slf4j
@Service
public class SqlserverDriverCustomServiceImpl extends AbstractJdbcDriverCustomService {

    /**
     * Construct the service with the driver sender service.
     *
     * @param driverSenderService the driver sender service for SDK communication
     */
    public SqlserverDriverCustomServiceImpl(DriverSenderService driverSenderService) {
        super(driverSenderService);
    }

    @Override
    protected String buildJdbcUrl(Map<String, AttributeBO> driverConfig) {
        String host = getConfigValue(driverConfig, "host", "localhost");
        int port = getConfigIntValue(driverConfig, "port", getDefaultPort());
        String database = getRequiredConfig(driverConfig, "database");
        String encrypt = getConfigValue(driverConfig, "encrypt", "false");
        String trustServerCertificate = getConfigValue(driverConfig, "trustServerCertificate", "true");
        return String.format("jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=%s;trustServerCertificate=%s;",
                host, port, database, encrypt, trustServerCertificate);
    }

    @Override
    protected String getDriverClassName() {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

    @Override
    protected int getDefaultPort() {
        return 1433;
    }

}
