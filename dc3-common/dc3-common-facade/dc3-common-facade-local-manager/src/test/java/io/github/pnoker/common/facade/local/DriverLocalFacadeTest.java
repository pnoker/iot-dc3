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

package io.github.pnoker.common.facade.local;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDriverQuery;
import io.github.pnoker.common.facade.local.builder.FacadeDriverBuilder;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.query.DriverQuery;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverLocalFacadeTest {

    private static final Long TENANT_ID = 1L;

    @Mock
    private DriverService driverService;

    @Mock
    private FacadeDriverBuilder facadeDriverBuilder;

    private DriverLocalFacade facade;

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static <T> T any(Class<T> clazz) {
        return org.mockito.ArgumentMatchers.any(clazz);
    }

    @BeforeEach
    void setUp() {
        facade = new DriverLocalFacade(driverService, facadeDriverBuilder);
    }

    @AfterEach
    void clearTenant() {
        // Guard against a leaked tenant context bleeding into the next test on the same
        // pooled thread — a real bug we want these tests to surface, not mask.
        TenantContextHolder.clear();
    }

    @Test
    void getByIdReturnsNullWhenServiceReturnsNull() {
        when(driverService.getById(1L)).thenReturn(null);
        assertThat(facade.getById(TENANT_ID, 1L)).isNull();
        verify(facadeDriverBuilder, never()).toFacadeBO(any());
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void getByIdMapsThroughBuilder() {
        DriverBO bo = new DriverBO();
        FacadeDriverBO mapped = new FacadeDriverBO();
        when(driverService.getById(1L)).thenAnswer(inv -> {
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(TENANT_ID);
            return bo;
        });
        when(facadeDriverBuilder.toFacadeBO(bo)).thenReturn(mapped);
        assertThat(facade.getById(TENANT_ID, 1L)).isSameAs(mapped);
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void getByIdClearsContextEvenWhenServiceThrows() {
        when(driverService.getById(1L)).thenThrow(new RuntimeException("manager center unreachable"));
        assertThatThrownBy(() -> facade.getById(TENANT_ID, 1L))
                .isInstanceOf(RuntimeException.class);
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void listByIdsReturnsEmptyForNullOrEmptyInput() {
        assertThat(facade.listByIds(TENANT_ID, null)).isEmpty();
        assertThat(facade.listByIds(TENANT_ID, Set.of())).isEmpty();
    }

    @Test
    void listByIdsReturnsEmptyWhenServiceReturnsNullOrEmpty() {
        when(driverService.listByIds(any())).thenReturn(null);
        assertThat(facade.listByIds(TENANT_ID, Set.of(1L))).isEmpty();
        when(driverService.listByIds(any())).thenReturn(List.of());
        assertThat(facade.listByIds(TENANT_ID, Set.of(1L))).isEmpty();
    }

    @Test
    void listByIdsMapsEachThroughBuilder() {
        DriverBO bo = new DriverBO();
        FacadeDriverBO mapped = new FacadeDriverBO();
        when(driverService.listByIds(any())).thenReturn(List.of(bo));
        when(facadeDriverBuilder.toFacadeBO(bo)).thenReturn(mapped);
        assertThat(facade.listByIds(TENANT_ID, Set.of(1L))).containsExactly(mapped);
    }

    @Test
    void listByPageReturnsEmptyWhenServiceReturnsNullPage() {
        FacadeDriverQuery query = new FacadeDriverQuery();
        when(facadeDriverBuilder.toManagerQuery(query)).thenReturn(new DriverQuery());
        when(driverService.list(any(DriverQuery.class))).thenReturn(null);
        assertThat(facade.listByPage(query).getRecords()).isEmpty();
    }

    @Test
    void listByPageMapsRecordsAndPaginationMeta() {
        FacadeDriverQuery query = new FacadeDriverQuery();
        when(facadeDriverBuilder.toManagerQuery(query)).thenReturn(new DriverQuery());
        DriverBO bo = new DriverBO();
        FacadeDriverBO mapped = new FacadeDriverBO();
        Page<DriverBO> page = new Page<>(1, 10, 50);
        page.setRecords(List.of(bo));
        when(driverService.list(any(DriverQuery.class))).thenReturn(page);
        when(facadeDriverBuilder.toFacadeBO(bo)).thenReturn(mapped);

        FacadePage<FacadeDriverBO> result = facade.listByPage(query);
        assertThat(result.getCurrent()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotal()).isEqualTo(50);
        assertThat(result.getPages()).isEqualTo(5);
        assertThat(result.getRecords()).containsExactly(mapped);
    }

    @Test
    void listByDeviceIdReturnsNullWhenServiceReturnsNull() {
        when(driverService.getByDeviceId(7L, null)).thenReturn(null);
        assertThat(facade.getByDeviceId(TENANT_ID, 7L)).isNull();
    }

    @Test
    void listByDeviceIdMapsThroughBuilder() {
        DriverBO bo = new DriverBO();
        FacadeDriverBO mapped = new FacadeDriverBO();
        when(driverService.getByDeviceId(7L, null)).thenReturn(bo);
        when(facadeDriverBuilder.toFacadeBO(bo)).thenReturn(mapped);
        assertThat(facade.getByDeviceId(TENANT_ID, 7L)).isSameAs(mapped);
    }

}
