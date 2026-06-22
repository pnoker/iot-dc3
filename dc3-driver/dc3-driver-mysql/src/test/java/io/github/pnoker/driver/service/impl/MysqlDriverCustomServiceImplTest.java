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
import io.github.pnoker.common.enums.AttributeTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MysqlDriverCustomServiceImplTest {

    @Mock
    private DriverSenderService driverSenderService;

    private MysqlDriverCustomServiceImpl service;

    private static AttributeBO string(String value) {
        return AttributeBO.builder().value(value).type(AttributeTypeEnum.STRING).build();
    }

    @BeforeEach
    void setUp() {
        service = new MysqlDriverCustomServiceImpl(driverSenderService);
    }

    @Test
    void validateFlagsMissingRequiredAttributes() {
        ValidationReport report = service.validate(new HashMap<>());
        assertThat(report.isPassed()).isFalse();
        assertThat(report.getIssues()).isNotEmpty();
    }

    @Test
    void validatePassesWhenAllRequiredAttributesPresent() {
        Map<String, AttributeBO> config = new HashMap<>();
        config.put("host", string("localhost"));
        config.put("port", AttributeBO.builder().value("3306").type(AttributeTypeEnum.INT).build());
        config.put("database", string("dc3"));
        config.put("username", string("root"));
        config.put("password", string("secret"));
        ValidationReport report = service.validate(config);
        assertThat(report.isPassed()).isTrue();
        assertThat(report.getIssues()).isEmpty();
    }

    @Test
    void validatePointFlagsMissingReadQuery() {
        ValidationReport report = service.validatePoint(new HashMap<>(), new PointBO());
        assertThat(report.isPassed()).isFalse();
        assertThat(report.getIssues()).isNotEmpty();
    }

    @Test
    void validatePointPassesWhenReadQueryPresent() {
        Map<String, AttributeBO> pointConfig = new HashMap<>();
        pointConfig.put("readQuery", string("SELECT 1"));
        ValidationReport report = service.validatePoint(pointConfig, new PointBO());
        assertThat(report.isPassed()).isTrue();
        assertThat(report.getIssues()).isEmpty();
    }
}
