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

package io.github.pnoker.common.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.pnoker.common.auth.dal.UserLoginManager;
import io.github.pnoker.common.auth.entity.bo.UserLoginBO;
import io.github.pnoker.common.auth.entity.builder.UserLoginBuilder;
import io.github.pnoker.common.auth.entity.model.UserLoginDO;
import io.github.pnoker.common.auth.service.TenantBindService;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.EmptyException;
import io.github.pnoker.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLoginServiceImplTest {

    @Mock
    private UserLoginBuilder userLoginBuilder;

    @Mock
    private UserLoginManager userLoginManager;

    @Mock
    private TenantBindService tenantBindService;

    @InjectMocks
    private UserLoginServiceImpl service;

    private UserLoginBO bo;
    private UserLoginDO doRow;

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    @BeforeEach
    void setUp() throws Exception {
        bo = new UserLoginBO();
        setField(bo, "id", 7L);
        setField(bo, "userId", 1L);
        setField(bo, "loginName", "alice");

        doRow = new UserLoginDO();
        doRow.setId(7L);
        doRow.setUserId(1L);
        doRow.setLoginName("alice");
    }

    @Test
    void saveSucceedsWhenNoDuplicate() {
        when(userLoginManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(userLoginBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(userLoginManager.save(doRow)).thenReturn(true);
        assertThatNoException().isThrownBy(() -> service.add(bo));
    }

    @Test
    void saveRejectsDuplicateLoginName() {
        when(userLoginManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(DuplicateException.class);
        verify(userLoginManager, never()).save(any());
    }

    @Test
    void saveThrowsAddExceptionWhenManagerReturnsFalse() {
        when(userLoginManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(userLoginBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(userLoginManager.save(doRow)).thenReturn(false);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(AddException.class);
    }

    @Test
    void removeRejectsUnknownId() {
        when(userLoginManager.getById(7L)).thenReturn(null);
        assertThatThrownBy(() -> service.delete(7L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void removeThrowsDeleteExceptionWhenManagerReturnsFalse() {
        when(userLoginManager.getById(7L)).thenReturn(doRow);
        when(userLoginManager.removeById(7L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(7L)).isInstanceOf(DeleteException.class);
    }

    @Test
    void getByLoginNameThrowsEmptyExceptionForBlankWhenAsked() {
        assertThatThrownBy(() -> service.getByLoginName("", true))
                .isInstanceOf(EmptyException.class);
    }

    @Test
    void getByLoginNameReturnsNullForBlankWhenSilent() {
        assertThat(service.getByLoginName(null, false)).isNull();
        assertThat(service.getByLoginName("", false)).isNull();
    }

    @Test
    void getByLoginNameThrowsNotFoundWhenAskedAndMissing() {
        when(userLoginManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertThatThrownBy(() -> service.getByLoginName("missing", true))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getByLoginNameReturnsBoForExistingEnabledLogin() {
        when(userLoginManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        when(userLoginBuilder.buildBOByDO(doRow)).thenReturn(bo);
        assertThat(service.getByLoginName("alice", false)).isSameAs(bo);
    }

    @Test
    void isLoginNameValidReturnsFalseForUnknownLogin() {
        when(userLoginManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertThat(service.isLoginNameValid("ghost")).isFalse();
    }

    @Test
    void isLoginNameValidReturnsTrueForEnabledLogin() throws Exception {
        setField(bo, "enableFlag", EnableFlagEnum.ENABLE);
        when(userLoginManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        when(userLoginBuilder.buildBOByDO(doRow)).thenReturn(bo);
        assertThat(service.isLoginNameValid("alice")).isTrue();
    }

    @Test
    void isLoginNameValidReturnsFalseForDisabledLogin() throws Exception {
        setField(bo, "enableFlag", EnableFlagEnum.DISABLE);
        when(userLoginManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        when(userLoginBuilder.buildBOByDO(doRow)).thenReturn(bo);
        assertThat(service.isLoginNameValid("alice")).isFalse();
    }

    @Test
    void isLoginNameAvailableReturnsTrueWhenTenantHasNoMembers() {
        when(tenantBindService.listUserIdsByTenantId(1L)).thenReturn(java.util.List.of());
        assertThat(service.isLoginNameAvailable("alice", 1L)).isTrue();
    }

    @Test
    void isLoginNameAvailableReturnsTrueWhenNameNotInTenant() {
        when(tenantBindService.listUserIdsByTenantId(1L)).thenReturn(java.util.List.of(1L, 2L));
        when(userLoginManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertThat(service.isLoginNameAvailable("newuser", 1L)).isTrue();
    }

    @Test
    void isLoginNameAvailableReturnsFalseWhenNameExistsInTenant() {
        when(tenantBindService.listUserIdsByTenantId(1L)).thenReturn(java.util.List.of(1L, 2L));
        when(userLoginManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        assertThat(service.isLoginNameAvailable("alice", 1L)).isFalse();
    }
}
