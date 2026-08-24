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
import io.github.pnoker.common.enums.AttributeTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OracleDriverCustomServiceImplTest {

    private OracleDriverCustomServiceImpl service;

    private static AttributeBO str(String value) {
        return AttributeBO.builder().value(value).type(AttributeTypeEnum.STRING).build();
    }

    private static Map<String, AttributeBO> baseConfig() {
        Map<String, AttributeBO> config = new HashMap<>();
        config.put("host", str("localhost"));
        config.put("port", AttributeBO.builder().value("1521").type(AttributeTypeEnum.INT).build());
        config.put("username", str("system"));
        config.put("password", str("secret"));
        return config;
    }

    private static PointBO point(Long id) {
        PointBO point = new PointBO();
        point.setId(id);
        return point;
    }

    @BeforeEach
    void setUp() {
        service = new OracleDriverCustomServiceImpl();
    }

    @Test
    void validateFlagsMissingRequiredAttributes() {
        ValidationReport report = service.validate(new HashMap<>());

        assertThat(report.isPassed()).isFalse();
        assertThat(report.getIssues()).isNotEmpty();
    }

    @Test
    void validatePointFlagsMissingReadQuery() {
        ValidationReport report = service.validatePoint(new HashMap<>(), point(1L));

        assertThat(report.isPassed()).isFalse();
        assertThat(report.getIssues()).isNotEmpty();
    }

    @Test
    void validatePassesForSidWithoutDatabaseOrServiceName() {
        ValidationReport report = service.validate(baseConfig());

        assertThat(report.isPassed()).isTrue();
        assertThat(report.getIssues()).isEmpty();
    }

    @Test
    void validateRequiresServiceNameForServiceNameConnectionType() {
        Map<String, AttributeBO> config = baseConfig();
        config.put("connectionType", str("ServiceName"));

        ValidationReport report = service.validate(config);

        assertThat(report.isPassed()).isFalse();
        assertThat(report.getIssues()).isNotEmpty();
    }

    @Test
    void validatePassesForServiceNameWithServiceNamePresent() {
        Map<String, AttributeBO> config = baseConfig();
        config.put("connectionType", str("ServiceName"));
        config.put("serviceName", str("ORCLPDB1"));

        ValidationReport report = service.validate(config);

        assertThat(report.isPassed()).isTrue();
    }

}
