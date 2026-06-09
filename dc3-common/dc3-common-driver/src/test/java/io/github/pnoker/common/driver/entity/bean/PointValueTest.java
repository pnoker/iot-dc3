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
package io.github.pnoker.common.driver.entity.bean;

import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.enums.PointTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PointValue} verifying that the ReadPointValue constructor
 * correctly propagates calValue and numValue.
 */
class PointValueTest {

    private DeviceBO device;
    private PointBO point;

    @BeforeEach
    void setUp() {
        device = new DeviceBO();
        device.setId(1L);
        point = new PointBO();
        point.setId(10L);
        point.setBaseValue(BigDecimal.ZERO);
        point.setMultiple(BigDecimal.ONE);
        point.setValueDecimal((byte) 6);
    }

    @Test
    void numericTypePopulatesNumValue() {
        point.setPointTypeFlag(PointTypeEnum.DOUBLE);
        ReadPointValue readPointValue = new ReadPointValue(device, point, "42.5");
        PointValue pv = new PointValue(readPointValue);

        assertThat(pv.getCalValue()).isEqualTo("42.5");
        assertThat(pv.getNumValue()).isEqualTo(42.5);
        assertThat(pv.getDeviceId()).isEqualTo(1L);
        assertThat(pv.getPointId()).isEqualTo(10L);
        assertThat(pv.getCreateTime()).isNotNull();
    }

    @Test
    void stringTypeLeavesNumValueNull() {
        point.setPointTypeFlag(PointTypeEnum.STRING);
        ReadPointValue readPointValue = new ReadPointValue(device, point, "hello");
        PointValue pv = new PointValue(readPointValue);

        assertThat(pv.getCalValue()).isEqualTo("hello");
        assertThat(pv.getNumValue()).isNull();
    }

    @Test
    void booleanTrueMapsNumValueToOne() {
        point.setPointTypeFlag(PointTypeEnum.BOOLEAN);
        ReadPointValue readPointValue = new ReadPointValue(device, point, "true");
        PointValue pv = new PointValue(readPointValue);

        assertThat(pv.getCalValue()).isEqualTo("true");
        assertThat(pv.getNumValue()).isEqualTo(1.0);
    }

    @Test
    void booleanFalseMapsNumValueToZero() {
        point.setPointTypeFlag(PointTypeEnum.BOOLEAN);
        ReadPointValue readPointValue = new ReadPointValue(device, point, "false");
        PointValue pv = new PointValue(readPointValue);

        assertThat(pv.getCalValue()).isEqualTo("false");
        assertThat(pv.getNumValue()).isEqualTo(0.0);
    }

    @Test
    void intTypePopulatesNumValue() {
        point.setPointTypeFlag(PointTypeEnum.INT);
        ReadPointValue readPointValue = new ReadPointValue(device, point, "100");
        PointValue pv = new PointValue(readPointValue);

        assertThat(pv.getCalValue()).isEqualTo("100");
        assertThat(pv.getNumValue()).isEqualTo(100.0);
    }

    @Test
    void builderHasNumValueField() {
        PointValue pv = PointValue.builder()
                .deviceId(1L)
                .pointId(10L)
                .rawValue("raw")
                .calValue("cal")
                .numValue(42.0)
                .build();

        assertThat(pv.getNumValue()).isEqualTo(42.0);
        assertThat(pv.getCalValue()).isEqualTo("cal");
    }

    @Test
    void nullNumValueIsSerializedAsNull() {
        PointValue pv = PointValue.builder()
                .deviceId(1L)
                .pointId(10L)
                .calValue("text")
                .build();

        assertThat(pv.getNumValue()).isNull();
    }
}
