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
import io.github.pnoker.common.auth.dal.TenantBindManager;
import io.github.pnoker.common.auth.entity.bo.TenantBindBO;
import io.github.pnoker.common.auth.entity.builder.TenantBindBuilder;
import io.github.pnoker.common.auth.entity.model.TenantBindDO;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
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
class TenantBindServiceImplTest {

    @Mock
    private TenantBindBuilder tenantBindBuilder;

    @Mock
    private TenantBindManager tenantBindManager;

    @InjectMocks
    private TenantBindServiceImpl service;

    private TenantBindBO bo;
    private TenantBindDO doRow;

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
        bo = new TenantBindBO();
        setField(bo, "id", 5L);
        setField(bo, "tenantId", 1L);
        setField(bo, "userId", 7L);

        doRow = new TenantBindDO();
        doRow.setId(5L);
        doRow.setTenantId(1L);
        doRow.setUserId(7L);
    }

    @Test
    void saveSucceedsWhenNoDuplicate() {
        when(tenantBindManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(tenantBindBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(tenantBindManager.save(doRow)).thenReturn(true);
        assertThatNoException().isThrownBy(() -> service.add(bo));
    }

    @Test
    void saveRejectsDuplicateBinding() {
        when(tenantBindManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(DuplicateException.class);
        verify(tenantBindManager, never()).save(any());
    }

    @Test
    void saveThrowsAddExceptionWhenManagerReturnsFalse() {
        when(tenantBindManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(tenantBindBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(tenantBindManager.save(doRow)).thenReturn(false);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(AddException.class);
    }

    @Test
    void removeRejectsUnknownId() {
        when(tenantBindManager.getById(5L)).thenReturn(null);
        assertThatThrownBy(() -> service.delete(5L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void removeThrowsDeleteExceptionWhenManagerReturnsFalse() {
        when(tenantBindManager.getById(5L)).thenReturn(doRow);
        when(tenantBindManager.removeById(5L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(5L)).isInstanceOf(DeleteException.class);
    }

    @Test
    void selectByTenantIdAndUserIdReturnsBoFromDal() {
        when(tenantBindManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        when(tenantBindBuilder.buildBOByDO(doRow)).thenReturn(bo);
        assertThat(service.selectByTenantIdAndUserId(1L, 7L)).isSameAs(bo);
    }

    // The happy-path of listUserIdsByTenantId requires mybatis-plus lambda metadata
    // which is unavailable in pure Mockito; that branch is exercised by the
    // Testcontainers PG slice test introduced in a later stage.

    @Test
    void selectByTenantIdAndUserIdReturnsNullWhenMissing() {
        when(tenantBindManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(tenantBindBuilder.buildBOByDO(null)).thenReturn(null);
        assertThat(service.selectByTenantIdAndUserId(1L, 7L)).isNull();
    }

    @Test
    void listUserIdsByTenantIdReturnsEmptyForNull() {
        assertThat(service.listUserIdsByTenantId(null)).isEmpty();
        verify(tenantBindManager, never()).listObjs(any(LambdaQueryWrapper.class), any());
    }
}
