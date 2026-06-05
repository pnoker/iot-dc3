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
import io.github.pnoker.common.auth.dal.UserPasswordManager;
import io.github.pnoker.common.auth.entity.bo.UserPasswordBO;
import io.github.pnoker.common.auth.entity.builder.UserPasswordBuilder;
import io.github.pnoker.common.auth.entity.model.UserPasswordDO;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.DecodeUtil;
import io.github.pnoker.common.utils.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPasswordServiceImplTest {

    @Mock
    private UserPasswordBuilder userPasswordBuilder;

    @Mock
    private UserPasswordManager userPasswordManager;

    @InjectMocks
    private UserPasswordServiceImpl service;

    private UserPasswordBO bo;
    private UserPasswordDO doRow;

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
        bo = new UserPasswordBO();
        setField(bo, "id", 9L);
        setField(bo, "loginPassword", "secret");

        doRow = new UserPasswordDO();
        doRow.setId(9L);
        doRow.setLoginPassword("secret");
    }

    @Test
    void saveHashesPasswordBeforePersisting() {
        when(userPasswordManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(userPasswordBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(userPasswordManager.save(doRow)).thenReturn(true);

        service.add(bo);

        ArgumentCaptor<UserPasswordDO> captor = ArgumentCaptor.forClass(UserPasswordDO.class);
        verify(userPasswordManager).save(captor.capture());
        String stored = captor.getValue().getLoginPassword();
        assertThat(stored).startsWith("$2a$");
        assertThat(PasswordUtil.verify(DecodeUtil.md5("secret"), stored)).isTrue();
    }

    @Test
    void saveRejectsDuplicate() {
        when(userPasswordManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(DuplicateException.class);
    }

    @Test
    void saveThrowsAddExceptionWhenManagerReturnsFalse() {
        when(userPasswordManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(userPasswordBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(userPasswordManager.save(any(UserPasswordDO.class))).thenReturn(false);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(AddException.class);
    }

    @Test
    void removeRejectsUnknownId() {
        when(userPasswordManager.getById(9L)).thenReturn(null);
        assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void removeThrowsDeleteExceptionWhenManagerReturnsFalse() {
        when(userPasswordManager.getById(9L)).thenReturn(doRow);
        when(userPasswordManager.removeById(9L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(DeleteException.class);
    }

    @Test
    void updateRehashesPassword() {
        when(userPasswordManager.getById(9L)).thenReturn(doRow);
        when(userPasswordManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        when(userPasswordBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(userPasswordManager.updateById(any(UserPasswordDO.class))).thenReturn(true);

        service.update(bo);

        ArgumentCaptor<UserPasswordDO> captor = ArgumentCaptor.forClass(UserPasswordDO.class);
        verify(userPasswordManager).updateById(captor.capture());
        String stored = captor.getValue().getLoginPassword();
        assertThat(stored).startsWith("$2a$");
        assertThat(PasswordUtil.verify(DecodeUtil.md5("secret"), stored)).isTrue();
    }

    @Test
    void updateThrowsUpdateExceptionWhenManagerReturnsFalse() {
        when(userPasswordManager.getById(9L)).thenReturn(doRow);
        when(userPasswordManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        when(userPasswordBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(userPasswordManager.updateById(any(UserPasswordDO.class))).thenReturn(false);
        assertThatThrownBy(() -> service.update(bo)).isInstanceOf(UpdateException.class);
    }

    @Test
    void resetPasswordThrowsIllegalStateWhenDefaultNotConfigured() throws Exception {
        UserPasswordBO restored = new UserPasswordBO();
        setField(restored, "id", 9L);
        setField(restored, "loginPassword", "old");

        when(userPasswordManager.getById(9L)).thenReturn(doRow, doRow);
        when(userPasswordBuilder.buildBOByDO(doRow)).thenReturn(restored);

        assertThatThrownBy(() -> service.restPassword(9L)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void resetPasswordIsNoopWhenRecordMissing() {
        when(userPasswordManager.getById(9L)).thenReturn(null);
        assertThatThrownBy(() -> service.restPassword(9L)).isInstanceOf(NotFoundException.class);
    }
}
