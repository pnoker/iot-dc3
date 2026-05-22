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

import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class VirtualDriverCustomServiceImplTest {

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private DriverSenderService driverSenderService;

    @InjectMocks
    private VirtualDriverCustomServiceImpl service;

    private static PointBO pointOfType(PointTypeFlagEnum type) {
        PointBO point = new PointBO();
        point.setId(1L);
        point.setPointName("p");
        point.setPointTypeFlag(type);
        return point;
    }

    private static MetadataEventDTO metadataEvent(MetadataTypeEnum type, MetadataOperateTypeEnum op, Long id) {
        MetadataEventDTO event = new MetadataEventDTO();
        event.setMetadataType(type);
        event.setOperateType(op);
        event.setId(id);
        return event;
    }

    @Test
    void initialIsNoOpAndDoesNotTouchCollaborators() {
        assertThatNoException().isThrownBy(() -> service.initial());
        verifyNoInteractions(driverMetadata, driverSenderService);
    }

    @Test
    void scheduleDoesNotReportDeviceStatus() {
        assertThatNoException().isThrownBy(() -> service.schedule());
        verifyNoInteractions(driverSenderService);
    }

    @Test
    void scheduleIsSilentWhenNoDevicesRegistered() {
        assertThatNoException().isThrownBy(() -> service.schedule());
        verifyNoInteractions(driverSenderService);
    }

    @Test
    void eventForDeviceLogsAndTouchesNothing() {
        MetadataEventDTO event = metadataEvent(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.ADD, 7L);
        service.event(event);
        verifyNoInteractions(driverMetadata, driverSenderService);
    }

    @Test
    void eventForPointLogsAndTouchesNothing() {
        MetadataEventDTO event = metadataEvent(MetadataTypeEnum.POINT, MetadataOperateTypeEnum.UPDATE, 9L);
        service.event(event);
        verifyNoInteractions(driverMetadata, driverSenderService);
    }

    @Test
    void readReturnsConstantStringForStringPoint() {
        PointBO point = pointOfType(PointTypeFlagEnum.STRING);
        ReadPointValue readPointValue = service.read(null, null, null, point);
        assertThat(readPointValue.getValue()).isEqualTo("abcd1234");
    }

    @Test
    void readProducesParsableBooleanForBooleanPoint() {
        PointBO point = pointOfType(PointTypeFlagEnum.BOOLEAN);
        ReadPointValue readPointValue = service.read(null, null, null, point);
        assertThat(readPointValue.getValue()).isIn("true", "false");
    }

    @Test
    void readProducesNumericValueInRangeForOtherPointTypes() {
        for (PointTypeFlagEnum type : new PointTypeFlagEnum[]{
                PointTypeFlagEnum.INT, PointTypeFlagEnum.LONG, PointTypeFlagEnum.FLOAT, PointTypeFlagEnum.DOUBLE,
                PointTypeFlagEnum.BYTE, PointTypeFlagEnum.SHORT
        }) {
            PointBO point = pointOfType(type);
            ReadPointValue readPointValue = service.read(null, null, null, point);
            double v = Double.parseDouble(readPointValue.getValue());
            assertThat(v).isBetween(0.0, 100.0);
        }
    }

    @Test
    void writeReturnsFalseUnconditionally() {
        Boolean result = service.write(null, null, null, null,
                WritePointValue.builder().value("anything").type(PointTypeFlagEnum.STRING).build());
        assertThat(result).isFalse();
        verify(driverSenderService, never()).pointValueSender(org.mockito.ArgumentMatchers.anyList());
    }
}
