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
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.WritePointException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class DlmsDriverCustomServiceImplTest {

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private DriverSenderService driverSenderService;

    private DlmsDriverCustomServiceImpl service;

    private static AttributeBO attr(String value, AttributeTypeEnum type) {
        return AttributeBO.builder().value(value).type(type).build();
    }

    private static Map<String, AttributeBO> pointConfig() {
        Map<String, AttributeBO> m = new HashMap<>();
        m.put("objectType", attr("REGISTER", AttributeTypeEnum.STRING));
        m.put("logicalName", attr("1.0.1.8.0.255", AttributeTypeEnum.STRING));
        m.put("attributeId", attr("2", AttributeTypeEnum.INT));
        return m;
    }

    private static DeviceBO device(Long id) {
        DeviceBO device = new DeviceBO();
        device.setId(id);
        return device;
    }

    private static PointBO point(Long id) {
        PointBO point = new PointBO();
        point.setId(id);
        return point;
    }

    @BeforeEach
    void setUp() {
        service = new DlmsDriverCustomServiceImpl(driverMetadata, driverSenderService);
        service.initial();
    }

    @Test
    void readFailsSafeWhenTransportNotImplemented() {
        assertThatThrownBy(() -> service.read(new HashMap<>(), pointConfig(), device(1L), point(1L)))
                .isInstanceOf(ReadPointException.class);
    }

    @Test
    void writeFailsSafeWhenTransportNotImplemented() {
        WritePointValue value = WritePointValue.builder().value("123").build();
        assertThatThrownBy(() -> service.write(new HashMap<>(), pointConfig(), device(1L), point(1L), value))
                .isInstanceOf(WritePointException.class);
    }

    @Test
    void validateFlagsMissingRequiredAttributes() {
        ValidationReport report = service.validate(new HashMap<>());
        assertThat(report.isPassed()).isFalse();
        assertThat(report.getIssues()).isNotEmpty();
    }

}
