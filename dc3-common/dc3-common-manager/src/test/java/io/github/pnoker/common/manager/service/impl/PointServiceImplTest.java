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
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.manager.dal.PointAttributeConfigManager;
import io.github.pnoker.common.manager.dal.PointManager;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.entity.builder.PointBuilder;
import io.github.pnoker.common.manager.entity.model.DeviceDO;
import io.github.pnoker.common.manager.entity.model.PointDO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.mapper.DeviceMapper;
import io.github.pnoker.common.manager.mapper.PointMapper;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.ProfileService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointServiceImplTest {

    @Mock
    private PointBuilder pointBuilder;

    @Mock
    private PointManager pointManager;

    @Mock
    private PointMapper pointMapper;

    @Mock
    private MetadataEventPublisher metadataEventPublisher;

    @Mock
    private ProfileService profileService;

    @Mock
    private PointAttributeConfigManager pointAttributeConfigManager;

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private DriverService driverService;

    @InjectMocks
    private PointServiceImpl service;

    private PointBO bo;
    private PointDO doRow;
    private ProfileBO profile;

    @BeforeEach
    void setUp() {
        bo = new PointBO();
        bo.setId(1L);
        bo.setPointName("Temperature");
        bo.setPointCode("temp");
        bo.setProfileId(5L);
        bo.setTenantId(100L);

        doRow = new PointDO();
        doRow.setId(1L);
        doRow.setPointName("Temperature");
        doRow.setPointCode("temp");
        doRow.setProfileId(5L);
        doRow.setTenantId(100L);

        profile = new ProfileBO();
        profile.setId(5L);
        profile.setTenantId(100L);
    }

    @Test
    void saveSucceedsAndPublishesAddEvent() {
        when(profileService.getById(5L)).thenReturn(profile);
        when(pointManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(pointBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(pointManager.save(doRow)).thenReturn(true);
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        assertThatNoException().isThrownBy(() -> service.add(bo));
        assertThat(bo.getPointCode()).isNull();
        verify(metadataEventPublisher, atLeastOnce()).publishEvent(any(MetadataEvent.class));
    }

    @Test
    void saveRejectsWhenProfileMissing() {
        when(profileService.getById(5L)).thenReturn(null);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void saveRejectsWhenProfileBelongsToOtherTenant() {
        ProfileBO otherTenant = new ProfileBO();
        otherTenant.setTenantId(999L);
        when(profileService.getById(5L)).thenReturn(otherTenant);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void saveRejectsDuplicate() {
        when(profileService.getById(5L)).thenReturn(profile);
        when(pointManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(DuplicateException.class);
        verify(pointManager, never()).save(any(PointDO.class));
    }

    @Test
    void saveThrowsAddExceptionWhenManagerReturnsFalse() {
        when(profileService.getById(5L)).thenReturn(profile);
        when(pointManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(pointBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(pointManager.save(doRow)).thenReturn(false);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(AddException.class);
    }

    @Test
    void removeRejectsUnknownId() {
        when(pointManager.getById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void removeThrowsDeleteExceptionWhenManagerReturnsFalse() {
        when(pointManager.getById(1L)).thenReturn(doRow);
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(pointManager.removeById(1L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(DeleteException.class);
    }

    @Test
    void removeSucceedsAndPublishesEvents() {
        when(pointManager.getById(1L)).thenReturn(doRow);
        when(pointManager.removeById(1L)).thenReturn(true);
        DeviceDO device1 = new DeviceDO();
        device1.setId(10L);
        DeviceDO device2 = new DeviceDO();
        device2.setId(11L);
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(device1, device2));
        assertThatNoException().isThrownBy(() -> service.delete(1L));
        // 1 DELETE for the point itself + 2 UPDATE for affected devices
        verify(metadataEventPublisher, atLeastOnce()).publishEvent(any(MetadataEvent.class));
    }

    @Test
    void updateRejectsUnknownId() {
        when(pointManager.getById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.update(bo)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateRejectsCrossTenantWrites() {
        PointDO otherTenant = new PointDO();
        otherTenant.setId(1L);
        otherTenant.setTenantId(999L);
        when(pointManager.getById(1L)).thenReturn(otherTenant);
        assertThatThrownBy(() -> service.update(bo))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Resource does not exist");
        verify(pointManager, never()).updateById(any(PointDO.class));
    }

    @Test
    void updateRejectsDuplicateAcrossDifferentRow() {
        when(pointManager.getById(1L)).thenReturn(doRow);
        when(profileService.getById(5L)).thenReturn(profile);
        PointDO other = new PointDO();
        other.setId(2L);
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(pointManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(other);
        assertThatThrownBy(() -> service.update(bo)).isInstanceOf(DuplicateException.class);
    }

    @Test
    void updateThrowsUpdateExceptionWhenManagerReturnsFalse() {
        bo.setPointCode("client-change");
        when(pointManager.getById(1L)).thenReturn(doRow);
        when(profileService.getById(5L)).thenReturn(profile);
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(pointManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        when(pointBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(pointManager.updateById(doRow)).thenReturn(false);
        assertThatThrownBy(() -> service.update(bo)).isInstanceOf(UpdateException.class);
        assertThat(bo.getPointCode()).isEqualTo("temp");
    }

    @Test
    void listByIdsReturnsEmptyForBlankInput() {
        assertThat(service.listByIds(null)).isEmpty();
        assertThat(service.listByIds(Set.of())).isEmpty();
    }

    @Test
    void listByIdsDelegatesToManager() {
        when(pointManager.listByIds(Set.of(1L))).thenReturn(List.of(doRow));
        when(pointBuilder.buildBOListByDOList(List.of(doRow))).thenReturn(List.of(bo));
        assertThat(service.listByIds(Set.of(1L))).containsExactly(bo);
    }

    @Test
    void unitReturnsEmptyForBlankInput() {
        assertThat(service.unit(null)).isEmpty();
        assertThat(service.unit(Set.of())).isEmpty();
    }

    @Test
    void unitMapsIdToUnit() {
        doRow.setUnit("celsius");
        when(pointManager.listByIds(Set.of(1L))).thenReturn(List.of(doRow));
        assertThat(service.unit(Set.of(1L))).containsEntry(1L, "celsius");
    }

    @Test
    void listByDeviceIdReturnsEmptyWhenDeviceMissing() {
        when(deviceMapper.selectById(eq(99L))).thenReturn(null);
        assertThat(service.listByDeviceId(99L, 1L)).isEmpty();
    }
}
