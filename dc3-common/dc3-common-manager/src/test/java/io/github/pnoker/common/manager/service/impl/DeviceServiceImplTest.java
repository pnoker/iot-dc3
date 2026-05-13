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

package io.github.pnoker.common.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.biz.ImportDeviceService;
import io.github.pnoker.common.manager.dal.DeviceManager;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.builder.DeviceBuilder;
import io.github.pnoker.common.manager.entity.model.DeviceDO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.mapper.DeviceMapper;
import io.github.pnoker.common.manager.service.DriverAttributeService;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.PointAttributeService;
import io.github.pnoker.common.manager.service.PointService;
import io.github.pnoker.common.manager.service.ProfileBindService;
import io.github.pnoker.common.manager.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceImplTest {

    @Mock
    private DeviceBuilder deviceBuilder;

    @Mock
    private DeviceManager deviceManager;

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private PointService pointService;

    @Mock
    private ProfileBindService profileBindService;

    @Mock
    private DriverService driverService;

    @Mock
    private ProfileService profileService;

    @Mock
    private DriverAttributeService driverAttributeService;

    @Mock
    private PointAttributeService pointAttributeService;

    @Mock
    private ImportDeviceService importDeviceService;

    @Mock
    private MetadataEventPublisher metadataEventPublisher;

    @InjectMocks
    private DeviceServiceImpl service;

    private DeviceBO bo;
    private DeviceDO doRow;
    private DriverBO driver;

    @BeforeEach
    void setUp() {
        bo = new DeviceBO();
        bo.setId(1L);
        bo.setDeviceName("Boiler-A");
        bo.setDeviceCode("boiler-a");
        bo.setDriverId(7L);
        bo.setTenantId(100L);
        bo.setProfileIds(new ArrayList<>());

        doRow = new DeviceDO();
        doRow.setId(1L);
        doRow.setDeviceName("Boiler-A");
        doRow.setDriverId(7L);
        doRow.setTenantId(100L);

        driver = new DriverBO();
        driver.setId(7L);
        driver.setTenantId(100L);
    }

    @Test
    void saveSucceedsForUniqueDeviceWithMatchingTenantDriver() {
        when(driverService.selectById(7L)).thenReturn(driver);
        when(deviceManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(deviceBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(deviceManager.save(doRow)).thenReturn(true);

        assertThatNoException().isThrownBy(() -> service.save(bo));
        verify(metadataEventPublisher, atLeastOnce()).publishEvent(any(MetadataEvent.class));
    }

    @Test
    void saveRejectsWhenDriverMissing() {
        when(driverService.selectById(7L)).thenReturn(null);
        assertThatThrownBy(() -> service.save(bo)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void saveRejectsWhenDriverBelongsToOtherTenant() {
        DriverBO otherTenant = new DriverBO();
        otherTenant.setTenantId(999L);
        when(driverService.selectById(7L)).thenReturn(otherTenant);
        assertThatThrownBy(() -> service.save(bo)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void saveRejectsDuplicateDeviceName() {
        when(driverService.selectById(7L)).thenReturn(driver);
        when(deviceManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        assertThatThrownBy(() -> service.save(bo)).isInstanceOf(DuplicateException.class);
        verify(deviceManager, never()).save(any(DeviceDO.class));
    }

    @Test
    void saveThrowsAddExceptionWhenManagerReturnsFalse() {
        when(driverService.selectById(7L)).thenReturn(driver);
        when(deviceManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(deviceBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(deviceManager.save(doRow)).thenReturn(false);
        assertThatThrownBy(() -> service.save(bo)).isInstanceOf(AddException.class);
    }

    @Test
    void removeRejectsUnknownId() {
        when(deviceManager.getById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.remove(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void removeRollsBackWhenProfileBindRemovalFails() {
        when(deviceManager.getById(1L)).thenReturn(doRow);
        when(profileBindService.removeByDeviceId(1L)).thenReturn(false);
        assertThatThrownBy(() -> service.remove(1L)).isInstanceOf(DeleteException.class);
        verify(deviceManager, never()).removeById(any(Long.class));
    }

    @Test
    void removeRollsBackWhenManagerRemoveReturnsFalse() {
        when(deviceManager.getById(1L)).thenReturn(doRow);
        when(profileBindService.removeByDeviceId(1L)).thenReturn(true);
        when(deviceManager.removeById(1L)).thenReturn(false);
        assertThatThrownBy(() -> service.remove(1L)).isInstanceOf(DeleteException.class);
    }

    @Test
    void updateRejectsUnknownId() {
        when(deviceManager.getById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.update(bo)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateRejectsCrossTenantWrites() {
        DeviceDO otherTenant = new DeviceDO();
        otherTenant.setId(1L);
        otherTenant.setTenantId(999L);
        when(deviceManager.getById(1L)).thenReturn(otherTenant);
        assertThatThrownBy(() -> service.update(bo))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Resource does not exist");
        verify(deviceManager, never()).updateById(any(DeviceDO.class));
    }
}
