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
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceQuery;
import io.github.pnoker.common.facade.local.builder.FacadeDeviceBuilder;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.query.DeviceQuery;
import io.github.pnoker.common.manager.service.DeviceService;
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
class DeviceLocalFacadeTest {

    private static final Long TENANT_ID = 1L;

    @Mock
    private DeviceService deviceService;

    @Mock
    private FacadeDeviceBuilder facadeDeviceBuilder;

    private DeviceLocalFacade facade;

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static <T> T any(Class<T> clazz) {
        return org.mockito.ArgumentMatchers.any(clazz);
    }

    @BeforeEach
    void setUp() {
        facade = new DeviceLocalFacade(deviceService, facadeDeviceBuilder);
    }

    @AfterEach
    void clearTenant() {
        // Guard against a leaked tenant context bleeding into the next test on the same
        // pooled thread — a real bug we want these tests to surface, not mask.
        TenantContextHolder.clear();
    }

    @Test
    void getByIdReturnsNullWhenServiceReturnsNull() {
        when(deviceService.getById(1L)).thenReturn(null);
        assertThat(facade.getById(TENANT_ID, 1L)).isNull();
        verify(facadeDeviceBuilder, never()).toFacadeBO(any());
        // context must be restored after the call (no leak to the next caller)
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void getByIdMapsThroughBuilder() {
        DeviceBO source = new DeviceBO();
        FacadeDeviceBO mapped = new FacadeDeviceBO();
        when(deviceService.getById(1L)).thenAnswer(inv -> {
            // the tenant context must be set BEFORE the service is invoked
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(TENANT_ID);
            return source;
        });
        when(facadeDeviceBuilder.toFacadeBO(source)).thenReturn(mapped);
        assertThat(facade.getById(TENANT_ID, 1L)).isSameAs(mapped);
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void getByIdClearsContextEvenWhenServiceThrows() {
        // try/finally must release the tenant binding on failure, otherwise a reused
        // pooled thread carries the previous caller's tenant into the next request.
        when(deviceService.getById(1L)).thenThrow(new RuntimeException("manager center unreachable"));
        assertThatThrownBy(() -> facade.getById(TENANT_ID, 1L))
                .isInstanceOf(RuntimeException.class);
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void listByIdsReturnsEmptyForNullOrEmptyInput() {
        assertThat(facade.listByIds(TENANT_ID, null)).isEmpty();
        assertThat(facade.listByIds(TENANT_ID, Set.of())).isEmpty();
        verify(deviceService, never()).listByIds(any());
    }

    @Test
    void listByIdsReturnsEmptyWhenServiceReturnsNullOrEmpty() {
        when(deviceService.listByIds(any())).thenReturn(null);
        assertThat(facade.listByIds(TENANT_ID, Set.of(1L))).isEmpty();

        when(deviceService.listByIds(any())).thenReturn(List.of());
        assertThat(facade.listByIds(TENANT_ID, Set.of(1L))).isEmpty();
    }

    @Test
    void listByIdsMapsEachThroughBuilder() {
        DeviceBO bo1 = new DeviceBO();
        DeviceBO bo2 = new DeviceBO();
        FacadeDeviceBO mapped1 = new FacadeDeviceBO();
        FacadeDeviceBO mapped2 = new FacadeDeviceBO();
        when(deviceService.listByIds(any())).thenReturn(List.of(bo1, bo2));
        when(facadeDeviceBuilder.toFacadeBO(bo1)).thenReturn(mapped1);
        when(facadeDeviceBuilder.toFacadeBO(bo2)).thenReturn(mapped2);

        assertThat(facade.listByIds(TENANT_ID, Set.of(1L, 2L))).containsExactly(mapped1, mapped2);
    }

    @Test
    void listByPageReturnsEmptyWhenServiceReturnsNullPage() {
        FacadeDeviceQuery query = new FacadeDeviceQuery();
        when(facadeDeviceBuilder.toManagerQuery(query)).thenReturn(new DeviceQuery());
        when(deviceService.list(any(DeviceQuery.class))).thenReturn(null);

        FacadePage<FacadeDeviceBO> page = facade.listByPage(query);
        assertThat(page).isNotNull();
        assertThat(page.getRecords()).isEmpty();
    }

    @Test
    void listByPageMapsRecordsAndCarriesPaginationMeta() {
        FacadeDeviceQuery query = new FacadeDeviceQuery();
        when(facadeDeviceBuilder.toManagerQuery(query)).thenReturn(new DeviceQuery());

        DeviceBO bo = new DeviceBO();
        FacadeDeviceBO mapped = new FacadeDeviceBO();
        Page<DeviceBO> page = new Page<>(2, 20, 100);
        page.setRecords(List.of(bo));
        when(deviceService.list(any(DeviceQuery.class))).thenReturn(page);
        when(facadeDeviceBuilder.toFacadeBO(bo)).thenReturn(mapped);

        FacadePage<FacadeDeviceBO> result = facade.listByPage(query);
        assertThat(result.getCurrent()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getTotal()).isEqualTo(100);
        assertThat(result.getPages()).isEqualTo(5);
        assertThat(result.getRecords()).containsExactly(mapped);
    }

    @Test
    void listByProfileIdMapsResults() {
        DeviceBO bo = new DeviceBO();
        FacadeDeviceBO mapped = new FacadeDeviceBO();
        when(deviceService.listByProfileId(5L, null)).thenReturn(List.of(bo));
        when(facadeDeviceBuilder.toFacadeBO(bo)).thenReturn(mapped);
        assertThat(facade.listByProfileId(TENANT_ID, 5L)).containsExactly(mapped);
    }

    @Test
    void listByProfileIdReturnsEmptyWhenServiceReturnsNullOrEmpty() {
        when(deviceService.listByProfileId(5L, null)).thenReturn(null);
        assertThat(facade.listByProfileId(TENANT_ID, 5L)).isEmpty();
        when(deviceService.listByProfileId(5L, null)).thenReturn(List.of());
        assertThat(facade.listByProfileId(TENANT_ID, 5L)).isEmpty();
    }

    @Test
    void listByDriverIdMapsResults() {
        DeviceBO bo = new DeviceBO();
        FacadeDeviceBO mapped = new FacadeDeviceBO();
        when(deviceService.listByDriverId(9L, null)).thenReturn(List.of(bo));
        when(facadeDeviceBuilder.toFacadeBO(bo)).thenReturn(mapped);
        assertThat(facade.listByDriverId(TENANT_ID, 9L)).containsExactly(mapped);
    }

    @Test
    void listByDriverIdReturnsEmptyWhenServiceReturnsNullOrEmpty() {
        when(deviceService.listByDriverId(9L, null)).thenReturn(null);
        assertThat(facade.listByDriverId(TENANT_ID, 9L)).isEmpty();
        when(deviceService.listByDriverId(9L, null)).thenReturn(List.of());
        assertThat(facade.listByDriverId(TENANT_ID, 9L)).isEmpty();
    }

}
