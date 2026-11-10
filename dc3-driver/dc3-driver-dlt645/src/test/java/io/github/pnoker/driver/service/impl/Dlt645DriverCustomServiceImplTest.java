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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.pnoker.common.driver.entity.bean.ValidationReport;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Dlt645DriverCustomServiceImplTest {

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private DriverSenderService driverSenderService;

    private Dlt645DriverCustomServiceImpl service;

    @BeforeEach
    void setUp() {
        DriverProperties driverProperties = new DriverProperties();
        driverProperties.setCode("Dlt645Driver");
        service = new Dlt645DriverCustomServiceImpl(driverMetadata, driverSenderService, driverProperties);
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
        byte[] frame = Dlt645Frame.buildReadRequest(address, new int[] {0x00, 0x01, 0x00, 0x00});
        assertThat(frame[0] & 0xFF).isEqualTo(0x68);
        assertThat(frame[frame.length - 1] & 0xFF).isEqualTo(0x16);
        assertThat(Dlt645Frame.verifyChecksum(frame)).isTrue();
    }

    @Test
    void parseRejectsMalformedFramesWithShortDataField() {
        // L<4 is malformed (a legit response carries at least the 4-byte data
        // identifier); the parser used to crash with NegativeArraySizeException.
        byte[] address = Dlt645Frame.encodeAddress("000000000001");
        byte[] frame = Dlt645Frame.buildReadRequest(address, new int[] {0x00, 0x01, 0x00, 0x00});
        frame[9] = 0x02;
        assertThatThrownBy(() -> Dlt645Frame.parse(frame))
                .isInstanceOf(io.github.pnoker.common.exception.ConnectorException.class)
                .hasMessageContaining("data length invalid");
    }

    @Test
    void parseReturnsDataWithResponseOffsetReversed() {
        byte[] address = Dlt645Frame.encodeAddress("000000000001");
        byte[] response = new byte[12 + 5];
        response[0] = (byte) 0x68;
        System.arraycopy(address, 0, response, 1, 6);
        response[7] = (byte) 0x68;
        response[8] = (byte) 0x91;
        response[9] = 0x05; // L = DI(4) + one data byte
        response[10] = 0x00;
        response[11] = 0x01;
        response[12] = 0x00;
        response[13] = 0x00;
        response[14] = (byte) (0x42 + 0x33);
        response[response.length - 2] =
                (byte) io.github.pnoker.common.driver.support.CodecUtil.sumChecksum(response, 0, response.length - 2);
        response[response.length - 1] = (byte) 0x16;

        byte[] data = Dlt645Frame.parse(response);
        assertThat(data).containsExactly(0x42);
    }
}
