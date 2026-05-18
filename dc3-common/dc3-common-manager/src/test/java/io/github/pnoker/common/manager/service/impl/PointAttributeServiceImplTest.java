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
import io.github.pnoker.common.manager.dal.PointAttributeManager;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.common.manager.entity.builder.PointAttributeBuilder;
import io.github.pnoker.common.manager.entity.model.PointAttributeDO;
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
class PointAttributeServiceImplTest {

    @Mock
    private PointAttributeBuilder pointAttributeBuilder;

    @Mock
    private PointAttributeManager pointAttributeManager;

    @Mock
    private DriverService driverService;

    @InjectMocks
    private PointAttributeServiceImpl service;

    private PointAttributeBO bo;
    private PointAttributeDO doRow;
    private DriverBO driver;

    @BeforeEach
    void setUp() {
        bo = new PointAttributeBO();
        bo.setId(1L);
        bo.setAttributeName("address");
        bo.setAttributeCode("address");
        bo.setDriverId(7L);
        bo.setTenantId(100L);

        doRow = new PointAttributeDO();
        doRow.setId(1L);
        doRow.setDriverId(7L);
        doRow.setTenantId(100L);

        driver = new DriverBO();
        driver.setId(7L);
        driver.setTenantId(100L);
    }

    @Test
    void saveSucceedsForUniqueAttribute() {
        when(driverService.getById(7L)).thenReturn(driver);
        when(pointAttributeManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(pointAttributeBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(pointAttributeManager.save(doRow)).thenReturn(true);
        assertThatNoException().isThrownBy(() -> service.add(bo));
    }

    @Test
    void saveRejectsWhenDriverMissing() {
        when(driverService.getById(7L)).thenReturn(null);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void saveRejectsWhenDriverBelongsToOtherTenant() {
        DriverBO other = new DriverBO();
        other.setTenantId(999L);
        when(driverService.getById(7L)).thenReturn(other);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void saveRejectsDuplicate() {
        when(driverService.getById(7L)).thenReturn(driver);
        when(pointAttributeManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(DuplicateException.class);
    }

    @Test
    void saveThrowsAddExceptionWhenManagerReturnsFalse() {
        when(driverService.getById(7L)).thenReturn(driver);
        when(pointAttributeManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(pointAttributeBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(pointAttributeManager.save(doRow)).thenReturn(false);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(AddException.class);
    }

    @Test
    void removeRejectsUnknownId() {
        when(pointAttributeManager.getById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void removeThrowsDeleteExceptionWhenManagerReturnsFalse() {
        when(pointAttributeManager.getById(1L)).thenReturn(doRow);
        when(pointAttributeManager.removeById(1L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(DeleteException.class);
    }

    @Test
    void updateRejectsCrossTenantWrites() {
        PointAttributeDO other = new PointAttributeDO();
        other.setId(1L);
        other.setTenantId(999L);
        when(pointAttributeManager.getById(1L)).thenReturn(other);
        assertThatThrownBy(() -> service.update(bo)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateThrowsUpdateExceptionWhenManagerReturnsFalse() {
        when(pointAttributeManager.getById(1L)).thenReturn(doRow);
        when(driverService.getById(7L)).thenReturn(driver);
        when(pointAttributeManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        when(pointAttributeBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(pointAttributeManager.updateById(doRow)).thenReturn(false);
        assertThatThrownBy(() -> service.update(bo)).isInstanceOf(UpdateException.class);
    }

    @Test
    void saveBatchSilentlyIgnoresEmptyOrNull() {
        assertThatNoException().isThrownBy(() -> service.saveBatch(null));
        assertThatNoException().isThrownBy(() -> service.saveBatch(List.of()));
        verify(pointAttributeManager, never()).saveBatch(any());
    }

    @Test
    void saveBatchThrowsWhenManagerReturnsFalse() {
        when(driverService.getById(7L)).thenReturn(driver);
        when(pointAttributeBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(pointAttributeManager.saveBatch(any())).thenReturn(false);
        assertThatThrownBy(() -> service.saveBatch(List.of(bo))).isInstanceOf(AddException.class);
    }

    @Test
    void removeByIdsThrowsWhenManagerReturnsFalse() {
        when(pointAttributeManager.removeByIds(List.of(1L))).thenReturn(false);
        assertThatThrownBy(() -> service.removeByIds(List.of(1L))).isInstanceOf(DeleteException.class);
    }
}
