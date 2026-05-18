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
import io.github.pnoker.common.manager.dal.DriverAttributeManager;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.builder.DriverAttributeBuilder;
import io.github.pnoker.common.manager.entity.model.DriverAttributeDO;
import io.github.pnoker.common.manager.service.DriverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverAttributeServiceImplTest {

    @Mock
    private DriverAttributeBuilder driverAttributeBuilder;

    @Mock
    private DriverAttributeManager driverAttributeManager;

    @Mock
    private DriverService driverService;

    @InjectMocks
    private DriverAttributeServiceImpl service;

    private DriverAttributeBO bo;
    private DriverAttributeDO doRow;
    private DriverBO driver;

    @BeforeEach
    void setUp() {
        bo = new DriverAttributeBO();
        bo.setId(1L);
        bo.setAttributeName("host");
        bo.setAttributeCode("host");
        bo.setDriverId(7L);
        bo.setTenantId(100L);

        doRow = new DriverAttributeDO();
        doRow.setId(1L);
        doRow.setDriverId(7L);
        doRow.setTenantId(100L);

        driver = new DriverBO();
        driver.setId(7L);
        driver.setTenantId(100L);
    }

    @Test
    void saveSucceedsForUniqueAttribute() {
        when(driverService.selectById(7L)).thenReturn(driver);
        when(driverAttributeManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(driverAttributeBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(driverAttributeManager.save(doRow)).thenReturn(true);
        assertThatNoException().isThrownBy(() -> service.add(bo));
    }

    @Test
    void saveRejectsWhenDriverMissing() {
        when(driverService.selectById(7L)).thenReturn(null);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void saveRejectsWhenDriverBelongsToOtherTenant() {
        DriverBO otherTenant = new DriverBO();
        otherTenant.setTenantId(999L);
        when(driverService.selectById(7L)).thenReturn(otherTenant);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void saveRejectsDuplicate() {
        when(driverService.selectById(7L)).thenReturn(driver);
        when(driverAttributeManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(DuplicateException.class);
    }

    @Test
    void saveThrowsAddExceptionWhenManagerReturnsFalse() {
        when(driverService.selectById(7L)).thenReturn(driver);
        when(driverAttributeManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(driverAttributeBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(driverAttributeManager.save(doRow)).thenReturn(false);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(AddException.class);
    }

    @Test
    void removeRejectsUnknownId() {
        when(driverAttributeManager.getById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void removeThrowsDeleteExceptionWhenManagerReturnsFalse() {
        when(driverAttributeManager.getById(1L)).thenReturn(doRow);
        when(driverAttributeManager.removeById(1L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(DeleteException.class);
    }

    @Test
    void updateRejectsCrossTenantWrites() {
        DriverAttributeDO otherTenant = new DriverAttributeDO();
        otherTenant.setId(1L);
        otherTenant.setTenantId(999L);
        when(driverAttributeManager.getById(1L)).thenReturn(otherTenant);
        assertThatThrownBy(() -> service.update(bo)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateThrowsUpdateExceptionWhenManagerReturnsFalse() {
        when(driverAttributeManager.getById(1L)).thenReturn(doRow);
        when(driverService.selectById(7L)).thenReturn(driver);
        when(driverAttributeManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        when(driverAttributeBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(driverAttributeManager.updateById(doRow)).thenReturn(false);
        assertThatThrownBy(() -> service.update(bo)).isInstanceOf(UpdateException.class);
    }

    @Test
    void saveBatchSilentlyIgnoresEmptyAndNullInput() {
        assertThatNoException().isThrownBy(() -> service.saveBatch(null));
        assertThatNoException().isThrownBy(() -> service.saveBatch(List.of()));
        verify(driverAttributeManager, never()).saveBatch(any());
    }

    @Test
    void saveBatchValidatesTenantPerEntry() {
        when(driverService.selectById(7L)).thenReturn(driver);
        when(driverAttributeBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(driverAttributeManager.saveBatch(any())).thenReturn(true);
        assertThatNoException().isThrownBy(() -> service.saveBatch(List.of(bo)));
    }

    @Test
    void saveBatchThrowsAddExceptionWhenManagerReturnsFalse() {
        when(driverService.selectById(7L)).thenReturn(driver);
        when(driverAttributeBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(driverAttributeManager.saveBatch(any())).thenReturn(false);
        assertThatThrownBy(() -> service.saveBatch(List.of(bo))).isInstanceOf(AddException.class);
    }

    @Test
    void updateBatchSilentlyIgnoresEmptyInput() {
        assertThatNoException().isThrownBy(() -> service.updateBatch(null));
        assertThatNoException().isThrownBy(() -> service.updateBatch(List.of()));
    }

    @Test
    void updateBatchThrowsWhenManagerReturnsFalse() {
        when(driverService.selectById(7L)).thenReturn(driver);
        when(driverAttributeBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(driverAttributeManager.updateBatchById(any())).thenReturn(false);
        assertThatThrownBy(() -> service.updateBatch(List.of(bo))).isInstanceOf(UpdateException.class);
    }

    @Test
    void removeByIdsSilentlyIgnoresEmptyInput() {
        assertThatNoException().isThrownBy(() -> service.removeByIds(null));
        assertThatNoException().isThrownBy(() -> service.removeByIds(List.of()));
        verify(driverAttributeManager, never()).removeByIds(any());
    }

    @Test
    void removeByIdsThrowsDeleteExceptionWhenManagerReturnsFalse() {
        when(driverAttributeManager.removeByIds(List.of(1L))).thenReturn(false);
        assertThatThrownBy(() -> service.removeByIds(List.of(1L))).isInstanceOf(DeleteException.class);
    }
}
