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
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class Dlt645DriverCustomServiceImplTest {

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private DriverSenderService driverSenderService;

    private Dlt645DriverCustomServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new Dlt645DriverCustomServiceImpl(driverMetadata, driverSenderService);
    }

    @Test
    void validateFlagsMissingRequiredDriverAttributes() {
        ValidationReport report = service.validate(new HashMap<>());
        assertThat(report.isPassed()).isFalse();
        assertThat(report.getIssues()).isNotEmpty();
    }

    @Test
    void validatePointFlagsMissingRequiredPointAttributes() {
        PointBO point = new PointBO();
        point.setId(1L);
        ValidationReport report = service.validatePoint(new HashMap<>(), point);
        assertThat(report.isPassed()).isFalse();
        assertThat(report.getIssues()).isNotEmpty();
    }

    @Test
    void encodeAddressRejectsInvalidAddress() {
        assertThatThrownBy(() -> Dlt645Frame.encodeAddress("1234"))
                .isInstanceOf(io.github.pnoker.common.exception.ConnectorException.class);
    }

    @Test
    void encodeAddressEncodesTwelveDigits() {
        byte[] address = Dlt645Frame.encodeAddress("000000000001");
        assertThat(address).hasSize(6);
        assertThat(address[5]).isEqualTo((byte) 0x01);
    }

    @Test
    void buildReadRequestHasValidStructureAndChecksum() {
        byte[] address = Dlt645Frame.encodeAddress("000000000001");
        byte[] frame = Dlt645Frame.buildReadRequest(address, new int[]{ 0x00, 0x01, 0x00, 0x00 });
        assertThat(frame[0] & 0xFF).isEqualTo(0x68);
        assertThat(frame[frame.length - 1] & 0xFF).isEqualTo(0x16);
        assertThat(Dlt645Frame.verifyChecksum(frame)).isTrue();
    }

}
