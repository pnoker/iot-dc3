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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.dal.TenantManager;
import io.github.pnoker.common.auth.entity.bo.TenantBO;
import io.github.pnoker.common.auth.entity.builder.TenantBuilder;
import io.github.pnoker.common.auth.entity.model.TenantDO;
import io.github.pnoker.common.auth.entity.query.TenantQuery;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
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
class TenantServiceImplTest {

    @Mock
    private TenantBuilder tenantBuilder;

    @Mock
    private TenantManager tenantManager;

    @InjectMocks
    private TenantServiceImpl service;

    private TenantBO bo;
    private TenantDO doRow;

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
        bo = new TenantBO();
        setField(bo, "id", 1L);
        bo.setTenantName("Acme");
        bo.setTenantCode("acme");

        doRow = new TenantDO();
        doRow.setId(1L);
        doRow.setTenantName("Acme");
        doRow.setTenantCode("acme");
    }

    @Test
    void saveSucceedsWhenManagerAccepts() {
        when(tenantManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(tenantBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(tenantManager.save(doRow)).thenReturn(true);

        assertThatNoException().isThrownBy(() -> service.add(bo));
        verify(tenantManager).save(doRow);
    }

    @Test
    void saveRejectsDuplicate() {
        when(tenantManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        assertThatThrownBy(() -> service.add(bo))
                .isInstanceOf(DuplicateException.class)
                .hasMessageContaining("Tenant has been duplicated");
        verify(tenantManager, never()).save(any());
    }

    @Test
    void saveThrowsAddExceptionWhenManagerReturnsFalse() {
        when(tenantManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(tenantBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(tenantManager.save(doRow)).thenReturn(false);
        assertThatThrownBy(() -> service.add(bo)).isInstanceOf(AddException.class);
    }

    @Test
    void removeSucceedsForExistingId() {
        when(tenantManager.getById(1L)).thenReturn(doRow);
        when(tenantManager.removeById(1L)).thenReturn(true);
        assertThatNoException().isThrownBy(() -> service.delete(1L));
        verify(tenantManager).removeById(1L);
    }

    @Test
    void removeRejectsUnknownId() {
        when(tenantManager.getById(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void removeThrowsDeleteExceptionWhenManagerReturnsFalse() {
        when(tenantManager.getById(1L)).thenReturn(doRow);
        when(tenantManager.removeById(1L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(DeleteException.class);
    }

    @Test
    void updateRejectsUnknownId() {
        when(tenantManager.getById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.update(bo)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateRejectsDuplicateAcrossDifferentRow() {
        when(tenantManager.getById(1L)).thenReturn(doRow);
        TenantDO other = new TenantDO();
        other.setId(2L);
        when(tenantManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(other);
        assertThatThrownBy(() -> service.update(bo)).isInstanceOf(DuplicateException.class);
    }

    @Test
    void updateAcceptsSameRowOnDuplicateCheck() {
        when(tenantManager.getById(1L)).thenReturn(doRow);
        when(tenantManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        when(tenantBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(tenantManager.updateById(doRow)).thenReturn(true);
        assertThatNoException().isThrownBy(() -> service.update(bo));
    }

    @Test
    void updateThrowsUpdateExceptionWhenManagerReturnsFalse() {
        when(tenantManager.getById(1L)).thenReturn(doRow);
        when(tenantManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        when(tenantBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(tenantManager.updateById(doRow)).thenReturn(false);
        assertThatThrownBy(() -> service.update(bo)).isInstanceOf(UpdateException.class);
    }

    @Test
    void selectByIdReturnsBoForKnownId() {
        when(tenantManager.getById(1L)).thenReturn(doRow);
        when(tenantBuilder.buildBOByDO(doRow)).thenReturn(bo);
        assertThat(service.getById(1L)).isSameAs(bo);
    }

    @Test
    void selectByIdRejectsUnknownId() {
        when(tenantManager.getById(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.getById(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void selectByCodeFiltersOnEnabledTenants() {
        when(tenantManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        when(tenantBuilder.buildBOByDO(doRow)).thenReturn(bo);
        assertThat(service.selectByCode("acme")).isSameAs(bo);
    }

    @Test
    void selectByCodeReturnsNullWhenNoMatch() {
        when(tenantManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(tenantBuilder.buildBOByDO(null)).thenReturn(null);
        assertThat(service.selectByCode("missing")).isNull();
    }

    @Test
    void selectByPageInjectsDefaultPagesWhenMissing() {
        TenantQuery query = new TenantQuery();
        Page<TenantDO> doPage = new Page<>(1, 10);
        when(tenantManager.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(doPage);
        Page<TenantBO> boPage = new Page<>(1, 10);
        when(tenantBuilder.buildBOPageByDOPage(doPage)).thenReturn(boPage);

        Page<TenantBO> result = service.list(query);

        assertThat(query.getPage()).isNotNull();
        assertThat(result).isSameAs(boPage);
    }

    @Test
    void selectByPagePreservesProvidedPagination() {
        TenantQuery query = new TenantQuery();
        Pages pages = new Pages();
        pages.setCurrent(2);
        pages.setSize(50);
        query.setPage(pages);
        Page<TenantDO> doPage = new Page<>(2, 50);
        when(tenantManager.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(doPage);
        Page<TenantBO> boPage = new Page<>(2, 50);
        when(tenantBuilder.buildBOPageByDOPage(doPage)).thenReturn(boPage);

        assertThat(service.list(query)).isSameAs(boPage);
        assertThat(query.getPage()).isSameAs(pages);
    }
}
