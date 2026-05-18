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
import io.github.pnoker.common.manager.dal.ProfileBindManager;
import io.github.pnoker.common.manager.entity.bo.ProfileBindBO;
import io.github.pnoker.common.manager.entity.builder.ProfileBindBuilder;
import io.github.pnoker.common.manager.entity.model.ProfileBindDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileBindServiceImplTest {

    @Mock
    private ProfileBindBuilder profileBindBuilder;

    @Mock
    private ProfileBindManager profileBindManager;

    @InjectMocks
    private ProfileBindServiceImpl service;

    private ProfileBindBO bo;
    private ProfileBindDO doRow;

    @BeforeEach
    void setUp() {
        bo = new ProfileBindBO();
        bo.setId(1L);
        bo.setProfileId(5L);
        bo.setDeviceId(10L);
        bo.setTenantId(100L);

        doRow = new ProfileBindDO();
        doRow.setId(1L);
        doRow.setProfileId(5L);
        doRow.setDeviceId(10L);
        doRow.setTenantId(100L);
    }

    @Test
    void saveSucceedsForUniqueBinding() {
        when(profileBindManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(profileBindBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(profileBindManager.save(doRow)).thenReturn(true);
        assertThatNoException().isThrownBy(() -> service.add(bo));
    }

    @Test
    void saveRejectsDuplicate() {
        when(profileBindManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(DuplicateException.class);
        verify(profileBindManager, never()).save(any(ProfileBindDO.class));
    }

    @Test
    void saveThrowsAddExceptionWhenManagerReturnsFalse() {
        when(profileBindManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(profileBindBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(profileBindManager.save(doRow)).thenReturn(false);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(AddException.class);
    }

    @Test
    void removeRejectsUnknownId() {
        when(profileBindManager.getById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void removeThrowsDeleteExceptionWhenManagerReturnsFalse() {
        when(profileBindManager.getById(1L)).thenReturn(doRow);
        when(profileBindManager.removeById(1L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(DeleteException.class);
    }

    @Test
    void removeByDeviceIdNoOpsWhenNoBinding() {
        when(profileBindManager.count(any(LambdaQueryWrapper.class))).thenReturn(0L);
        service.removeByDeviceId(99L);
        verify(profileBindManager, never()).remove(any(LambdaQueryWrapper.class));
    }

    @Test
    void removeByDeviceIdDelegatesToManagerWhenBindingsExist() {
        when(profileBindManager.count(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(profileBindManager.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        service.removeByDeviceId(10L);
        verify(profileBindManager).remove(any(LambdaQueryWrapper.class));
    }

    @Test
    void removeByDeviceIdThrowsWhenManagerRemoveReturnsFalse() {
        when(profileBindManager.count(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(profileBindManager.remove(any(LambdaQueryWrapper.class))).thenReturn(false);
        assertThatThrownBy(() -> service.removeByDeviceId(10L)).isInstanceOf(DeleteException.class);
    }

    @Test
    void removeByDeviceIdAndProfileIdNoOpsWhenNoBinding() {
        when(profileBindManager.count(any(LambdaQueryWrapper.class))).thenReturn(0L);
        service.removeByDeviceIdAndProfileId(10L, 5L);
        verify(profileBindManager, never()).remove(any(LambdaQueryWrapper.class));
    }

    @Test
    void updateRejectsUnknownId() {
        when(profileBindManager.getById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.update(bo)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateThrowsUpdateExceptionWhenManagerReturnsFalse() {
        when(profileBindManager.getById(1L)).thenReturn(doRow);
        when(profileBindManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        when(profileBindBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(profileBindManager.updateById(doRow)).thenReturn(false);
        assertThatThrownBy(() -> service.update(bo)).isInstanceOf(UpdateException.class);
    }

    @Test
    void getByIdRejectsUnknownId() {
        when(profileBindManager.getById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.getById(1L)).isInstanceOf(NotFoundException.class);
    }
}
