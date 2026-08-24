package io.github.pnoker.driver.service.impl;

import io.github.pnoker.common.driver.entity.bean.ValidationReport;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.sql.AbstractJdbcDriverCustomService;
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
 * @since 2026.5.22
 */
@Service
public class OracleDriverCustomServiceImpl extends AbstractJdbcDriverCustomService {

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
        checkRequired(driverConfig, "username", issues);
        checkRequired(driverConfig, "password", issues);

        String connectionType = getConfigValue(driverConfig, "connectionType", "SID");
        if ("ServiceName".equalsIgnoreCase(connectionType)) {
            checkRequired(driverConfig, "serviceName", issues);
        }

        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues)
                .build();
    }

}
