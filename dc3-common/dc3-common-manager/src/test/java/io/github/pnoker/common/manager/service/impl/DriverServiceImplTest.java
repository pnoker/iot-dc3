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
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.manager.dal.DeviceManager;
import io.github.pnoker.common.manager.dal.DriverManager;
import io.github.pnoker.common.manager.dal.PointManager;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.builder.DriverBuilder;
import io.github.pnoker.common.manager.entity.model.DeviceDO;
import io.github.pnoker.common.manager.entity.model.DriverDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverServiceImplTest {

    @Mock
    private DriverBuilder driverBuilder;

    @Mock
    private DriverManager driverManager;

    @Mock
    private DeviceManager deviceManager;

    @Mock
    private PointManager pointManager;

    @InjectMocks
    private DriverServiceImpl service;

    private DriverBO bo;
    private DriverDO doRow;

    @BeforeEach
    void setUp() {
        bo = new DriverBO();
        bo.setId(1L);
        bo.setDriverName("ModbusTcp");
        bo.setDriverCode("modbus-tcp");
        bo.setServiceName("dc3-driver-modbus-tcp");
        bo.setTenantId(100L);

        doRow = new DriverDO();
        doRow.setId(1L);
        doRow.setDriverName("ModbusTcp");
        doRow.setDriverCode("modbus-tcp");
        doRow.setTenantId(100L);
    }

    @Test
    void saveSucceedsForUniqueDriver() {
        when(driverManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(driverBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(driverManager.save(doRow)).thenReturn(true);
        assertThatNoException().isThrownBy(() -> service.add(bo));
        verify(driverManager).save(doRow);
    }

    @Test
    void saveRejectsDuplicateNameWithinTenant() {
        when(driverManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(DuplicateException.class);
        verify(driverManager, never()).save(any());
    }

    @Test
    void saveThrowsAddExceptionWhenManagerReturnsFalse() {
        when(driverManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(driverBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(driverManager.save(doRow)).thenReturn(false);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(AddException.class);
    }

    @Test
    void removeRejectsUnknownId() {
        when(driverManager.getById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void removeThrowsDeleteExceptionWhenManagerReturnsFalse() {
        when(driverManager.getById(1L)).thenReturn(doRow);
        when(driverManager.removeById(1L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(DeleteException.class);
    }

    @Test
    void updateRejectsUnknownId() {
        when(driverManager.getById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.update(bo)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateRejectsCrossTenantWrites() {
        DriverDO otherTenant = new DriverDO();
        otherTenant.setId(1L);
        otherTenant.setTenantId(999L);
        when(driverManager.getById(1L)).thenReturn(otherTenant);
        assertThatThrownBy(() -> service.update(bo))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Resource does not exist");
        verify(driverManager, never()).updateById(any(DriverDO.class));
    }

    @Test
    void updateRejectsDuplicateAcrossDifferentRow() {
        when(driverManager.getById(1L)).thenReturn(doRow);
        DriverDO other = new DriverDO();
        other.setId(2L);
        other.setTenantId(100L);
        when(driverManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(other);
        assertThatThrownBy(() -> service.update(bo)).isInstanceOf(DuplicateException.class);
    }

    @Test
    void updateAcceptsSameRowOnDuplicateCheck() {
        when(driverManager.getById(1L)).thenReturn(doRow);
        when(driverManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        when(driverBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(driverManager.updateById(doRow)).thenReturn(true);
        assertThatNoException().isThrownBy(() -> service.update(bo));
    }

    @Test
    void updateThrowsUpdateExceptionWhenManagerReturnsFalse() {
        when(driverManager.getById(1L)).thenReturn(doRow);
        when(driverManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        when(driverBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(driverManager.updateById(doRow)).thenReturn(false);
        assertThatThrownBy(() -> service.update(bo)).isInstanceOf(UpdateException.class);
    }

    @Test
    void getByIdRejectsUnknownId() {
        when(driverManager.getById(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.getById(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void listByIdsReturnsEmptyForNullOrEmpty() {
        assertThat(service.listByIds(null)).isEmpty();
        assertThat(service.listByIds(Set.of())).isEmpty();
        verify(driverManager, never()).listByIds(any());
    }

    @Test
    void listByIdsDelegatesToManager() {
        when(driverManager.listByIds(Set.of(1L, 2L))).thenReturn(List.of(doRow));
        when(driverBuilder.buildBOListByDOList(List.of(doRow))).thenReturn(List.of(bo));
        assertThat(service.listByIds(Set.of(1L, 2L))).containsExactly(bo);
    }

    @Test
    void listByProfileIdReturnsEmptyWhenNoDeviceBound() {
        when(deviceManager.list(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        assertThat(service.listByProfileId(5L)).isEmpty();
        verify(deviceManager, never()).listByIds(any());
    }

    @Test
    void listByProfileIdGathersDriverIdsFromDevices() {
        DeviceDO d1 = new DeviceDO();
        d1.setId(10L);
        d1.setDriverId(1L);
        DeviceDO d2 = new DeviceDO();
        d2.setId(11L);
        d2.setDriverId(1L);
        when(deviceManager.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(d1, d2));
        when(driverManager.listByIds(Set.of(1L))).thenReturn(List.of(doRow));
        when(driverBuilder.buildBOListByDOList(List.of(doRow))).thenReturn(List.of(bo));

        assertThat(service.listByProfileId(5L)).containsExactly(bo);
    }
}
