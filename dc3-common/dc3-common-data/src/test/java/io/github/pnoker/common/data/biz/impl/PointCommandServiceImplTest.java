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
import io.github.pnoker.common.data.dal.PointCommandHistoryManager;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.data.entity.vo.PointCommandReadVO;
import io.github.pnoker.common.data.entity.vo.PointCommandWriteVO;
import io.github.pnoker.common.data.mapper.EntityStateMapper;
import io.github.pnoker.common.data.validator.PointCommandValidator;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.RwFlagEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointCommandServiceImplTest {

    @Mock
    private DeviceFacade deviceFacade;

    @Mock
    private DriverFacade driverFacade;

    @Mock
    private PointFacade pointFacade;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private PointCommandHistoryManager pointCommandHistoryManager;

    @Mock
    private EntityStateMapper entityStateMapper;

    @Mock
    private PointCommandValidator pointCommandValidator;

    @InjectMocks
    private PointCommandServiceImpl service;

    private FacadeDeviceBO device;
    private FacadePointBO point;
    private FacadeDriverBO driver;

    @BeforeEach
    void setUp() {
        device = new FacadeDeviceBO();
        device.setProfileId(5L);
        device.setEnableFlag(EnableFlagEnum.ENABLE);
        point = new FacadePointBO();
        point.setProfileId(5L);
        point.setEnableFlag(EnableFlagEnum.ENABLE);
        point.setRwFlag(RwFlagEnum.READ_WRITE);
        driver = new FacadeDriverBO();
        driver.setId(30L);
        driver.setServiceName("dc3-driver-modbus-tcp");
    }

    @Test
    void readPublishesReadCommandToOwningDriver() {
        when(deviceFacade.getById(1L, 10L)).thenReturn(device);
        when(pointFacade.getById(1L, 20L)).thenReturn(point);
        when(driverFacade.getByDeviceId(1L, 10L)).thenReturn(driver);
        mockDriverOnline();

        PointCommandReadVO vo = new PointCommandReadVO();
        vo.setDeviceId(10L);
        vo.setPointId(20L);
        service.read(1L, vo);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConstant.TOPIC_EXCHANGE_POINT_COMMAND),
                eq(RabbitConstant.ROUTING_POINT_COMMAND_PREFIX + "dc3-driver-modbus-tcp"),
                any(Object.class),
                any(org.springframework.amqp.rabbit.connection.CorrelationData.class));
        verify(pointCommandHistoryManager).save(any());
        verify(pointCommandHistoryManager).updateById(any());
    }

    @Test
    void readRejectsCommandWhenDriverUnknown() {
        when(deviceFacade.getById(1L, 10L)).thenReturn(device);
        when(pointFacade.getById(1L, 20L)).thenReturn(point);
        when(driverFacade.getByDeviceId(1L, 10L)).thenReturn(null);

        PointCommandReadVO vo = new PointCommandReadVO();
        vo.setDeviceId(10L);
        vo.setPointId(20L);
        assertThatThrownBy(() -> service.read(1L, vo))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("No driver registered");

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void readRejectsUnknownDevice() {
        when(deviceFacade.getById(1L, 99L)).thenReturn(null);
        PointCommandReadVO vo = new PointCommandReadVO();
        vo.setDeviceId(99L);
        vo.setPointId(20L);
        assertThatThrownBy(() -> service.read(1L, vo))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Device");
    }

    @Test
    void readRejectsUnknownPoint() {
        when(deviceFacade.getById(1L, 10L)).thenReturn(device);
        when(pointFacade.getById(1L, 99L)).thenReturn(null);
        PointCommandReadVO vo = new PointCommandReadVO();
        vo.setDeviceId(10L);
        vo.setPointId(99L);
        assertThatThrownBy(() -> service.read(1L, vo))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Point");
    }

    @Test
    void readRejectsCrossProfileBindingAsUnauthorized() {
        FacadeDeviceBO mismatchedDevice = new FacadeDeviceBO();
        mismatchedDevice.setProfileId(99L);
        mismatchedDevice.setEnableFlag(EnableFlagEnum.ENABLE);
        when(deviceFacade.getById(1L, 10L)).thenReturn(mismatchedDevice);
        when(pointFacade.getById(1L, 20L)).thenReturn(point);
        PointCommandReadVO vo = new PointCommandReadVO();
        vo.setDeviceId(10L);
        vo.setPointId(20L);
        assertThatThrownBy(() -> service.read(1L, vo)).isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void readRejectsDeviceWithoutAnyProfileBinding() {
        FacadeDeviceBO bareDevice = new FacadeDeviceBO();
        bareDevice.setProfileId(null);
        bareDevice.setEnableFlag(EnableFlagEnum.ENABLE);
        when(deviceFacade.getById(1L, 10L)).thenReturn(bareDevice);
        when(pointFacade.getById(1L, 20L)).thenReturn(point);
        PointCommandReadVO vo = new PointCommandReadVO();
        vo.setDeviceId(10L);
        vo.setPointId(20L);
        assertThatThrownBy(() -> service.read(1L, vo)).isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void writePublishesWriteCommandToOwningDriver() {
        when(deviceFacade.getById(1L, 10L)).thenReturn(device);
        when(pointFacade.getById(1L, 20L)).thenReturn(point);
        when(driverFacade.getByDeviceId(1L, 10L)).thenReturn(driver);
        mockDriverOnline();

        PointCommandWriteVO vo = new PointCommandWriteVO();
        vo.setDeviceId(10L);
        vo.setPointId(20L);
        vo.setValue("42.5");
        service.write(1L, vo);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConstant.TOPIC_EXCHANGE_POINT_COMMAND),
                eq(RabbitConstant.ROUTING_POINT_COMMAND_PREFIX + "dc3-driver-modbus-tcp"),
                any(Object.class),
                any(org.springframework.amqp.rabbit.connection.CorrelationData.class));
        verify(pointCommandHistoryManager).save(any());
        verify(pointCommandHistoryManager).updateById(any());
    }

    @Test
    void writeRejectsCommandWhenDriverUnknown() {
        when(deviceFacade.getById(1L, 10L)).thenReturn(device);
        when(pointFacade.getById(1L, 20L)).thenReturn(point);
        when(driverFacade.getByDeviceId(1L, 10L)).thenReturn(null);
        PointCommandWriteVO vo = new PointCommandWriteVO();
        vo.setDeviceId(10L);
        vo.setPointId(20L);
        vo.setValue("v");
        assertThatThrownBy(() -> service.write(1L, vo))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("No driver registered");
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void writeRejectsUnknownDevice() {
        when(deviceFacade.getById(1L, 99L)).thenReturn(null);
        PointCommandWriteVO vo = new PointCommandWriteVO();
        vo.setDeviceId(99L);
        vo.setPointId(20L);
        assertThatThrownBy(() -> service.write(1L, vo)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void readRejectsDisabledDevice() {
        FacadeDeviceBO disabledDevice = new FacadeDeviceBO();
        disabledDevice.setProfileId(5L);
        disabledDevice.setEnableFlag(EnableFlagEnum.DISABLE);
        when(deviceFacade.getById(1L, 10L)).thenReturn(disabledDevice);

        PointCommandReadVO vo = new PointCommandReadVO();
        vo.setDeviceId(10L);
        vo.setPointId(20L);
        assertThatThrownBy(() -> service.read(1L, vo))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void readRejectsDisabledPoint() {
        FacadePointBO disabledPoint = new FacadePointBO();
        disabledPoint.setProfileId(5L);
        disabledPoint.setEnableFlag(EnableFlagEnum.DISABLE);
        when(deviceFacade.getById(1L, 10L)).thenReturn(device);
        when(pointFacade.getById(1L, 20L)).thenReturn(disabledPoint);

        PointCommandReadVO vo = new PointCommandReadVO();
        vo.setDeviceId(10L);
        vo.setPointId(20L);
        assertThatThrownBy(() -> service.read(1L, vo))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void writeRejectsReadOnlyPoint() {
        FacadePointBO readOnlyPoint = new FacadePointBO();
        readOnlyPoint.setProfileId(5L);
        readOnlyPoint.setEnableFlag(EnableFlagEnum.ENABLE);
        readOnlyPoint.setRwFlag(RwFlagEnum.READ_ONLY);
        when(deviceFacade.getById(1L, 10L)).thenReturn(device);
        when(pointFacade.getById(1L, 20L)).thenReturn(readOnlyPoint);

        PointCommandWriteVO vo = new PointCommandWriteVO();
        vo.setDeviceId(10L);
        vo.setPointId(20L);
        vo.setValue("1.0");
        assertThatThrownBy(() -> service.write(1L, vo))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("not writable");
    }

    private void mockDriverOnline() {
        EntityStateDO driverState = new EntityStateDO();
        driverState.setStateFlag(EntityStatusEnum.ONLINE.getIndex());
        when(entityStateMapper.selectOne(any())).thenReturn(driverState);
    }
}
