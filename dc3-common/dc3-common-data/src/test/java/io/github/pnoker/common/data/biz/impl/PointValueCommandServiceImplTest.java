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

package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.data.entity.vo.PointValueReadVO;
import io.github.pnoker.common.data.entity.vo.PointValueWriteVO;
import io.github.pnoker.common.entity.dto.DeviceCommandDTO;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointValueCommandServiceImplTest {

    @Mock
    private DeviceFacade deviceFacade;

    @Mock
    private DriverFacade driverFacade;

    @Mock
    private PointFacade pointFacade;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PointValueCommandServiceImpl service;

    private FacadeDeviceBO device;
    private FacadePointBO point;
    private FacadeDriverBO driver;

    @BeforeEach
    void setUp() {
        device = new FacadeDeviceBO();
        device.setProfileIds(List.of(5L));
        point = new FacadePointBO();
        point.setProfileId(5L);
        driver = new FacadeDriverBO();
        driver.setServiceName("dc3-driver-modbus-tcp");
    }

    @Test
    void readPublishesReadCommandToOwningDriver() {
        when(deviceFacade.selectById(1L, 10L)).thenReturn(device);
        when(pointFacade.selectById(1L, 20L)).thenReturn(point);
        when(driverFacade.selectByDeviceId(1L, 10L)).thenReturn(driver);

        PointValueReadVO vo = new PointValueReadVO();
        vo.setDeviceId(10L);
        vo.setPointId(20L);
        service.read(1L, vo);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConstant.TOPIC_EXCHANGE_COMMAND),
                eq(RabbitConstant.ROUTING_DEVICE_COMMAND_PREFIX + "dc3-driver-modbus-tcp"),
                any(DeviceCommandDTO.class));
    }

    @Test
    void readSilentlyDropsCommandWhenDriverUnknown() {
        when(deviceFacade.selectById(1L, 10L)).thenReturn(device);
        when(pointFacade.selectById(1L, 20L)).thenReturn(point);
        when(driverFacade.selectByDeviceId(1L, 10L)).thenReturn(null);

        PointValueReadVO vo = new PointValueReadVO();
        vo.setDeviceId(10L);
        vo.setPointId(20L);
        service.read(1L, vo);

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void readRejectsUnknownDevice() {
        when(deviceFacade.selectById(1L, 99L)).thenReturn(null);
        PointValueReadVO vo = new PointValueReadVO();
        vo.setDeviceId(99L);
        vo.setPointId(20L);
        assertThatThrownBy(() -> service.read(1L, vo))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Device");
    }

    @Test
    void readRejectsUnknownPoint() {
        when(deviceFacade.selectById(1L, 10L)).thenReturn(device);
        when(pointFacade.selectById(1L, 99L)).thenReturn(null);
        PointValueReadVO vo = new PointValueReadVO();
        vo.setDeviceId(10L);
        vo.setPointId(99L);
        assertThatThrownBy(() -> service.read(1L, vo))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Point");
    }

    @Test
    void readRejectsCrossProfileBindingAsUnauthorized() {
        FacadeDeviceBO mismatchedDevice = new FacadeDeviceBO();
        mismatchedDevice.setProfileIds(List.of(99L));
        when(deviceFacade.selectById(1L, 10L)).thenReturn(mismatchedDevice);
        when(pointFacade.selectById(1L, 20L)).thenReturn(point);
        PointValueReadVO vo = new PointValueReadVO();
        vo.setDeviceId(10L);
        vo.setPointId(20L);
        assertThatThrownBy(() -> service.read(1L, vo)).isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void readRejectsDeviceWithoutAnyProfileBinding() {
        FacadeDeviceBO bareDevice = new FacadeDeviceBO();
        bareDevice.setProfileIds(null);
        when(deviceFacade.selectById(1L, 10L)).thenReturn(bareDevice);
        when(pointFacade.selectById(1L, 20L)).thenReturn(point);
        PointValueReadVO vo = new PointValueReadVO();
        vo.setDeviceId(10L);
        vo.setPointId(20L);
        assertThatThrownBy(() -> service.read(1L, vo)).isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void writePublishesWriteCommandToOwningDriver() {
        when(deviceFacade.selectById(1L, 10L)).thenReturn(device);
        when(pointFacade.selectById(1L, 20L)).thenReturn(point);
        when(driverFacade.selectByDeviceId(1L, 10L)).thenReturn(driver);

        PointValueWriteVO vo = new PointValueWriteVO();
        vo.setDeviceId(10L);
        vo.setPointId(20L);
        vo.setValue("42.5");
        service.write(1L, vo);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConstant.TOPIC_EXCHANGE_COMMAND),
                eq(RabbitConstant.ROUTING_DEVICE_COMMAND_PREFIX + "dc3-driver-modbus-tcp"),
                any(DeviceCommandDTO.class));
    }

    @Test
    void writeSilentlyDropsCommandWhenDriverUnknown() {
        when(deviceFacade.selectById(1L, 10L)).thenReturn(device);
        when(pointFacade.selectById(1L, 20L)).thenReturn(point);
        when(driverFacade.selectByDeviceId(1L, 10L)).thenReturn(null);
        PointValueWriteVO vo = new PointValueWriteVO();
        vo.setDeviceId(10L);
        vo.setPointId(20L);
        vo.setValue("v");
        assertThatNoException().isThrownBy(() -> service.write(1L, vo));
        verify(rabbitTemplate, never()).convertAndSend(
                any(String.class), any(String.class), any(DeviceCommandDTO.class));
    }

    @Test
    void writeRejectsUnknownDevice() {
        when(deviceFacade.selectById(1L, 99L)).thenReturn(null);
        PointValueWriteVO vo = new PointValueWriteVO();
        vo.setDeviceId(99L);
        vo.setPointId(20L);
        assertThatThrownBy(() -> service.write(1L, vo)).isInstanceOf(NotFoundException.class);
    }
}
