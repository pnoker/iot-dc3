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

import io.github.pnoker.common.driver.entity.bean.ValidationReport;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.sql.AbstractJdbcDriverCustomService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Custom driver service implementation for the Postgresql driver.
 * <p>
 * Extends the abstract JDBC driver service to provide PostgreSQL-specific
 * connection URL construction and driver class configuration.
 * </p>
 *
 * @author pnoker
 * @since 2026.5.22
 */
@Service
public class PostgresqlDriverCustomServiceImpl extends AbstractJdbcDriverCustomService {

    @Override
    protected String buildJdbcUrl(Map<String, AttributeBO> driverConfig) {
        String host = getConfigValue(driverConfig, "host", "localhost");
        int port = getConfigIntValue(driverConfig, "port", getDefaultPort());
        String database = getRequiredConfig(driverConfig, "database");
        return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
    }

    @Override
    protected String getDriverClassName() {
        return "org.postgresql.Driver";
    }

    @Override
    protected int getDefaultPort() {
        return 5432;
    }

    @Override
    public ValidationReport validate(Map<String, AttributeBO> driverConfig) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(driverConfig, "host", issues);
        checkRequired(driverConfig, "port", issues);
        checkRequired(driverConfig, "database", issues);
        checkRequired(driverConfig, "username", issues);
        checkRequired(driverConfig, "password", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues)
                .build();
    }

}
