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
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.sql.AbstractJdbcDriverCustomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Custom driver service implementation for the Oracle driver.
 * <p>
 * Extends the abstract JDBC driver service to provide Oracle-specific
 * connection URL construction and driver class configuration. Supports both
 * SID and Service Name connection types.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.6.2
 */
@Slf4j
@Service
public class OracleDriverCustomServiceImpl extends AbstractJdbcDriverCustomService {

    /**
     * Construct the service with the driver sender service.
     *
     * @param driverSenderService the driver sender service for SDK communication
     */
    public OracleDriverCustomServiceImpl(DriverSenderService driverSenderService) {
        super(driverSenderService);
    }

    private static void checkRequired(Map<String, AttributeBO> config, String code,
                                      List<ValidationReport.AttributeIssue> issues) {
        AttributeBO attr = config.get(code);
        if (attr == null || attr.getValue() == null) {
            issues.add(ValidationReport.AttributeIssue.builder()
                    .attributeCode(code).level(ValidationReport.IssueLevel.ERROR)
                    .message("Missing required attribute: " + code).build());
        }
    }

    @Override
    protected String buildJdbcUrl(Map<String, AttributeBO> driverConfig) {
        String host = getConfigValue(driverConfig, "host", "localhost");
        int port = getConfigIntValue(driverConfig, "port", getDefaultPort());
        String connectionType = getConfigValue(driverConfig, "connectionType", "SID");

        if ("ServiceName".equalsIgnoreCase(connectionType)) {
            String serviceName = getRequiredConfig(driverConfig, "serviceName");
            return String.format("jdbc:oracle:thin:@//%s:%d/%s", host, port, serviceName);
        } else {
            String sid = getConfigValue(driverConfig, "sid", "ORCL");
            return String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, sid);
        }
    }

    @Override
    protected String getDriverClassName() {
        return "oracle.jdbc.OracleDriver";
    }

    @Override
    protected int getDefaultPort() {
        return 1521;
    }

    @Override
    public ValidationReport validate(Map<String, AttributeBO> driverConfig) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(driverConfig, "host", issues);
        checkRequired(driverConfig, "port", issues);
        checkRequired(driverConfig, "database", issues);
        checkRequired(driverConfig, "username", issues);
        checkRequired(driverConfig, "password", issues);
        checkRequired(driverConfig, "connectionType", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(pointConfig, "readQuery", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

}