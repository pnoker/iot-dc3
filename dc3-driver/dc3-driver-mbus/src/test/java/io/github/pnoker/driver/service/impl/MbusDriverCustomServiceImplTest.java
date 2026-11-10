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

import io.github.pnoker.common.driver.entity.bean.ValidationReport;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MbusDriverCustomServiceImplTest {

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private DriverSenderService driverSenderService;

    private MbusDriverCustomServiceImpl service;

    @BeforeEach
    void setUp() {
        DriverProperties driverProperties = new DriverProperties();
        driverProperties.setCode("MbusDriver");
        service = new MbusDriverCustomServiceImpl(driverMetadata, driverSenderService, driverProperties);
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
    void buildReqUd2HasValidStructureAndChecksum() {
        byte[] frame = MbusFrame.buildReqUd2(0);
        assertThat(frame[0] & 0xFF).isEqualTo(0x68);
        assertThat(frame[frame.length - 1] & 0xFF).isEqualTo(0x16);
        assertThat(MbusFrame.verifyChecksum(frame)).isTrue();
    }

    @Test
    void buildSndNkeHasValidStructure() {
        byte[] frame = MbusFrame.buildSndNke(0);
        assertThat(frame[0] & 0xFF).isEqualTo(0x10);
        assertThat(frame[frame.length - 1] & 0xFF).isEqualTo(0x16);
    }

    @Test
    void decodesLittleEndianIntegersPerEn13757() {
        // 500 stored LSB-first (F4 01): the old big-endian read returned 62465.
        MbusRecord twoByte = new MbusRecord(0x02, 0x13, new byte[] {(byte) 0xF4, 0x01});
        assertThat(MbusFrame.decodeValue(twoByte, "AUTO")).isEqualTo("500");

        // 24-bit values accumulate unsigned across the default branch.
        MbusRecord threeByte = new MbusRecord(0x03, 0x13, new byte[] {0x20, (byte) 0xA1, 0x07});
        assertThat(MbusFrame.decodeValue(threeByte, "INT")).isEqualTo("500000");

        // 64-bit integer per EN 13757 data-length code 5.
        MbusRecord eightByte = new MbusRecord(0x05, 0x13, new byte[] {1, 0, 0, 0, 0, 0, 0, 0});
        assertThat(MbusFrame.decodeValue(eightByte, "INT")).isEqualTo("1");
    }

    @Test
    void decodesBcdLeastSignificantDigitsFirst() {
        // 1234 kWh stored as digits low-pair-first (34 12).
        MbusRecord bcd = new MbusRecord(0x0A, 0x14, new byte[] {0x34, 0x12});
        assertThat(MbusFrame.decodeValue(bcd, "AUTO")).isEqualTo("1234");

        // leading zero nibbles are trimmed
        MbusRecord padded = new MbusRecord(0x0A, 0x14, new byte[] {0x07, 0x00});
        assertThat(MbusFrame.decodeValue(padded, "BCD")).isEqualTo("7");
    }

    @Test
    void decodesLittleEndianFloats() {
        // 100.0f stored LSB-first (00 00 C8 42).
        MbusRecord record = new MbusRecord(0x04, 0x38, new byte[] {0x00, 0x00, (byte) 0xC8, 0x42});
        assertThat(MbusFrame.decodeValue(record, "FLOAT")).isEqualTo("100.0");
    }

    @Test
    void parseRecordsWalksDifeAndVifeExtensionChains() {
        // DIF 0x84 (32-bit + DIFE), DIFE 0x80 (chain continues), DIFE 0x00,
        // VIF 0xFB (plant), VIFE 0x23 — the value follows the full chain.
        byte[] data = {(byte) 0x84, (byte) 0x80, 0x00, (byte) 0xFB, 0x23, 0x40, 0x0D, 0x03, 0x00};
        List<MbusRecord> records = MbusFrame.parseRecords(data);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getDif() & 0x0F).isEqualTo(0x04);
        // the value starts after DIF + 2 DIFEs + VIF + VIFE
        assertThat(records.get(0).getValueBytes()).containsExactly(0x40, 0x0D, 0x03, 0x00);
        assertThat(MbusFrame.decodeValue(records.get(0), "INT")).isEqualTo("200000");
    }

    @Test
    void parseRecordsStopsOnSpecialAndFillerDifs() {
        byte[] data = {0x02, 0x13, 0x39, 0x30, (byte) 0x2F};
        List<MbusRecord> records = MbusFrame.parseRecords(data);

        assertThat(records).hasSize(1);
        assertThat(MbusFrame.decodeValue(records.get(0), "INT")).isEqualTo("12345");
    }
}
