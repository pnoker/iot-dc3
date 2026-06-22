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
import io.github.pnoker.common.driver.metadata.DriverMetadata;
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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class HttpDriverCustomServiceImplTest {

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private DriverSenderService driverSenderService;

    private HttpDriverCustomServiceImpl service;

    private static PointBO point(Long id) {
        PointBO point = new PointBO();
        point.setId(id);
        return point;
    }

    @BeforeEach
    void setUp() {
        service = new HttpDriverCustomServiceImpl(driverMetadata, driverSenderService);
    }

    @Test
    void validateFlagsMissingBaseUrl() {
        ValidationReport report = service.validate(new HashMap<>());

        assertThat(report.isPassed()).isFalse();
        assertThat(report.getIssues()).isNotEmpty();
        verifyNoInteractions(driverSenderService);
    }

    @Test
    void validatePassesWhenBaseUrlPresent() {
        Map<String, AttributeBO> driverConfig = new HashMap<>();
        driverConfig.put("baseUrl",
                AttributeBO.builder().value("http://localhost:8080").type(AttributeTypeEnum.STRING).build());

        ValidationReport report = service.validate(driverConfig);

        assertThat(report.isPassed()).isTrue();
        assertThat(report.getIssues()).isEmpty();
    }

    @Test
    void validatePointFlagsMissingPathAndMethod() {
        ValidationReport report = service.validatePoint(new HashMap<>(), point(1L));

        assertThat(report.isPassed()).isFalse();
        assertThat(report.getIssues()).hasSize(2);
    }

    @Test
    void scheduleIsNoOp() {
        assertThatNoException().isThrownBy(() -> service.schedule());
        verifyNoInteractions(driverSenderService);
    }

}
