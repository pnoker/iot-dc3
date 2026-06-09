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
import io.github.pnoker.common.exception.AssociatedException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.manager.dal.CommandManager;
import io.github.pnoker.common.manager.dal.DeviceManager;
import io.github.pnoker.common.manager.dal.EventManager;
import io.github.pnoker.common.manager.dal.PointManager;
import io.github.pnoker.common.manager.dal.ProfileManager;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.entity.builder.ProfileBuilder;
import io.github.pnoker.common.manager.entity.model.ProfileDO;
import io.github.pnoker.common.manager.mapper.DeviceMapper;
import io.github.pnoker.common.manager.mapper.ProfileMapper;
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
class ProfileServiceImplTest {

    @Mock
    private ProfileBuilder profileBuilder;

    @Mock
    private ProfileManager profileManager;

    @Mock
    private PointManager pointManager;

    @Mock
    private CommandManager commandManager;

    @Mock
    private EventManager eventManager;

    @Mock
    private DeviceManager deviceManager;

    @Mock
    private ProfileMapper profileMapper;

    @Mock
    private DeviceMapper deviceMapper;

    @InjectMocks
    private ProfileServiceImpl service;

    private ProfileBO bo;
    private ProfileDO doRow;

    @BeforeEach
    void setUp() {
        bo = new ProfileBO();
        bo.setId(1L);
        bo.setProfileName("DefaultProfile");
        bo.setProfileCode("default");
        bo.setTenantId(100L);

        doRow = new ProfileDO();
        doRow.setId(1L);
        doRow.setProfileName("DefaultProfile");
        doRow.setProfileCode("default");
        doRow.setTenantId(100L);
    }

    @Test
    void saveSucceedsForUniqueProfile() {
        when(profileManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(profileBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(profileManager.save(doRow)).thenReturn(true);
        assertThatNoException().isThrownBy(() -> service.add(bo));
        assertThat(bo.getProfileCode()).isNull();
        verify(profileManager).save(doRow);
    }

    @Test
    void saveRejectsDuplicate() {
        when(profileManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(DuplicateException.class);
        verify(profileManager, never()).save(any(ProfileDO.class));
    }

    @Test
    void saveThrowsAddExceptionWhenManagerReturnsFalse() {
        when(profileManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(profileBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(profileManager.save(doRow)).thenReturn(false);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(AddException.class);
    }

    @Test
    void removeRejectsUnknownId() {
        when(profileManager.getById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void removeRejectsProfileReferencedByDevices() {
        when(profileManager.getById(1L)).thenReturn(doRow);
        when(deviceManager.count(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(AssociatedException.class)
                .hasMessageContaining("devices");
        verify(profileManager, never()).removeById(1L);
    }

    @Test
    void removeRejectsProfileWithPoints() {
        when(profileManager.getById(1L)).thenReturn(doRow);
        when(pointManager.count(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(AssociatedException.class)
                .hasMessageContaining("points");
        verify(profileManager, never()).removeById(1L);
    }

    @Test
    void removeRejectsProfileWithCommands() {
        when(profileManager.getById(1L)).thenReturn(doRow);
        when(commandManager.count(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(AssociatedException.class)
                .hasMessageContaining("commands");
        verify(profileManager, never()).removeById(1L);
    }

    @Test
    void removeRejectsProfileWithEvents() {
        when(profileManager.getById(1L)).thenReturn(doRow);
        when(eventManager.count(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(AssociatedException.class)
                .hasMessageContaining("events");
        verify(profileManager, never()).removeById(1L);
    }

    @Test
    void removeSucceedsWhenNoAssociatedResourcesExist() {
        when(profileManager.getById(1L)).thenReturn(doRow);
        when(profileManager.removeById(1L)).thenReturn(true);

        assertThatNoException().isThrownBy(() -> service.delete(1L));
        verify(profileManager).removeById(1L);
    }

    @Test
    void updateRejectsUnknownId() {
        when(profileManager.getById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.update(bo)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateRejectsCrossTenantWrites() {
        ProfileDO otherTenant = new ProfileDO();
        otherTenant.setId(1L);
        otherTenant.setTenantId(999L);
        when(profileManager.getById(1L)).thenReturn(otherTenant);
        assertThatThrownBy(() -> service.update(bo))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Resource does not exist");
        verify(profileManager, never()).updateById(any(ProfileDO.class));
    }

    @Test
    void updateRejectsDuplicateAcrossDifferentRow() {
        when(profileManager.getById(1L)).thenReturn(doRow);
        ProfileDO other = new ProfileDO();
        other.setId(2L);
        when(profileManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(other);
        assertThatThrownBy(() -> service.update(bo)).isInstanceOf(DuplicateException.class);
    }

    @Test
    void updateAcceptsSameRowOnDuplicateCheck() {
        bo.setProfileCode("client-change");
        when(profileManager.getById(1L)).thenReturn(doRow);
        when(profileManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        when(profileBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(profileManager.updateById(doRow)).thenReturn(true);
        assertThatNoException().isThrownBy(() -> service.update(bo));
        assertThat(bo.getProfileCode()).isEqualTo("default");
    }

    @Test
    void updateThrowsUpdateExceptionWhenManagerReturnsFalse() {
        when(profileManager.getById(1L)).thenReturn(doRow);
        when(profileManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        when(profileBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(profileManager.updateById(doRow)).thenReturn(false);
        assertThatThrownBy(() -> service.update(bo)).isInstanceOf(UpdateException.class);
    }

    @Test
    void getByIdRejectsUnknownId() {
        when(profileManager.getById(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.getById(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void listByIdsReturnsEmptyForBlankInput() {
        assertThat(service.listByIds(null)).isEmpty();
        assertThat(service.listByIds(Set.of())).isEmpty();
    }

    @Test
    void listByIdsDelegatesToManager() {
        when(profileManager.listByIds(Set.of(1L))).thenReturn(List.of(doRow));
        when(profileBuilder.buildBOListByDOList(List.of(doRow))).thenReturn(List.of(bo));
        assertThat(service.listByIds(Set.of(1L))).containsExactly(bo);
    }

    @Test
    void listByDeviceIdReturnsEmptyWhenDeviceMissing() {
        when(deviceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertThat(service.listByDeviceId(99L)).isEmpty();
    }

    @Test
    void getByNameAndTypeReturnsBoFromDal() {
        when(profileManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        when(profileBuilder.buildBOByDO(doRow)).thenReturn(bo);
        assertThat(service.getByNameAndType(100L, "DefaultProfile",
                io.github.pnoker.common.enums.ProfileTypeEnum.SYSTEM)).isSameAs(bo);
    }
}
