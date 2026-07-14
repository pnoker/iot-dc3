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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@ExtendWith(MockitoExtension.class)
class FinsDriverCustomServiceImplTest {

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private DriverSenderService driverSenderService;

    private FinsDriverCustomServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FinsDriverCustomServiceImpl(driverMetadata, driverSenderService);
    }

    @Test
    void validateFlagsMissingRequiredDriverAttributes() {
        ValidationReport report = service.validate(new HashMap<>());

        assertThat(report.isPassed()).isFalse();
        assertThat(report.getIssues()).extracting("attributeCode")
                .contains("host", "port", "protocol");
        report.getIssues().forEach(issue -> assertThat(issue.getLevel())
                .isEqualTo(ValidationReport.IssueLevel.ERROR));
    }

    @Test
    void validatePointFlagsMissingRequiredPointAttributes() {
        PointBO point = new PointBO();
        point.setId(1L);

        ValidationReport report = service.validatePoint(new HashMap<>(), point);

        assertThat(report.isPassed()).isFalse();
        assertThat(report.getIssues()).extracting("attributeCode")
                .contains("memoryArea", "address");
    }

    @Test
    void validatePassesWhenRequiredDriverAttributesPresent() {
        Map<String, AttributeBO> config = new HashMap<>();
        config.put("host", AttributeBO.builder().value("127.0.0.1").build());
        config.put("port", AttributeBO.builder().value("9600").build());
        config.put("protocol", AttributeBO.builder().value("FINS").build());

        ValidationReport report = service.validate(config);

        assertThat(report.isPassed()).isTrue();
        assertThat(report.getIssues()).isEmpty();
    }

    @Test
    void scheduleIsNoOp() {
        assertThatNoException().isThrownBy(() -> service.schedule());
    }

    @Test
    void wordCountIsTwoFor32BitTypesAndOneOtherwise() {
        assertThat(service.wordCount("INT32")).isEqualTo(2);
        assertThat(service.wordCount("UINT32")).isEqualTo(2);
        assertThat(service.wordCount("FLOAT")).isEqualTo(2);
        assertThat(service.wordCount("float")).isEqualTo(2);
        assertThat(service.wordCount("INT16")).isEqualTo(1);
        assertThat(service.wordCount("UINT16")).isEqualTo(1);
        assertThat(service.wordCount("STRING")).isEqualTo(1);
    }

    @Test
    void floatEncodeDecodeRoundTrips() {
        byte[] encoded = service.encodeWriteData("3.14", "FLOAT");

        assertThat(encoded).hasSize(4);
        assertThat(Float.parseFloat(service.decodeValue(encoded, "FLOAT"))).isEqualTo(3.14f);
    }

    @Test
    void int32EncodeDecodeRoundTrips() {
        byte[] encoded = service.encodeWriteData("70000", "INT32");

        assertThat(encoded).hasSize(4);
        assertThat(service.decodeValue(encoded, "INT32")).isEqualTo("70000");
    }

}
