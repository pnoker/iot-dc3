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
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadePointQuery;
import io.github.pnoker.common.facade.local.builder.FacadePointBuilder;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.query.PointQuery;
import io.github.pnoker.common.manager.service.PointService;
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
class PointLocalFacadeTest {

    private static final Long TENANT_ID = 1L;

    @Mock
    private PointService pointService;

    @Mock
    private FacadePointBuilder facadePointBuilder;

    private PointLocalFacade facade;

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static <T> T any(Class<T> clazz) {
        return org.mockito.ArgumentMatchers.any(clazz);
    }

    @BeforeEach
    void setUp() {
        facade = new PointLocalFacade(pointService, facadePointBuilder);
    }

    @AfterEach
    void clearTenant() {
        // Guard against a leaked tenant context bleeding into the next test on the same
        // pooled thread — a real bug we want these tests to surface, not mask.
        TenantContextHolder.clear();
    }

    @Test
    void getByIdReturnsNullWhenServiceReturnsNull() {
        when(pointService.getById(1L)).thenReturn(null);
        assertThat(facade.getById(TENANT_ID, 1L)).isNull();
        verify(facadePointBuilder, never()).toFacadeBO(any());
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void getByIdMapsThroughBuilder() {
        PointBO bo = new PointBO();
        FacadePointBO mapped = new FacadePointBO();
        when(pointService.getById(1L)).thenAnswer(inv -> {
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(TENANT_ID);
            return bo;
        });
        when(facadePointBuilder.toFacadeBO(bo)).thenReturn(mapped);
        assertThat(facade.getById(TENANT_ID, 1L)).isSameAs(mapped);
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void getByIdClearsContextEvenWhenServiceThrows() {
        when(pointService.getById(1L)).thenThrow(new RuntimeException("manager center unreachable"));
        assertThatThrownBy(() -> facade.getById(TENANT_ID, 1L))
                .isInstanceOf(RuntimeException.class);
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void listByIdsReturnsEmptyForNullOrEmptyInput() {
        assertThat(facade.listByIds(TENANT_ID, null)).isEmpty();
        assertThat(facade.listByIds(TENANT_ID, Set.of())).isEmpty();
        verify(pointService, never()).listByIds(any());
    }

    @Test
    void listByIdsMapsEachThroughBuilder() {
        PointBO bo = new PointBO();
        FacadePointBO mapped = new FacadePointBO();
        when(pointService.listByIds(any())).thenReturn(List.of(bo));
        when(facadePointBuilder.toFacadeBO(bo)).thenReturn(mapped);
        assertThat(facade.listByIds(TENANT_ID, Set.of(1L))).containsExactly(mapped);
    }

    @Test
    void listByPageMapsRecordsAndPaginationMeta() {
        FacadePointQuery query = new FacadePointQuery();
        when(facadePointBuilder.toManagerQuery(query)).thenReturn(new PointQuery());
        PointBO bo = new PointBO();
        FacadePointBO mapped = new FacadePointBO();
        Page<PointBO> page = new Page<>(3, 25, 75);
        page.setRecords(List.of(bo));
        when(pointService.list(any(PointQuery.class))).thenReturn(page);
        when(facadePointBuilder.toFacadeBO(bo)).thenReturn(mapped);

        FacadePage<FacadePointBO> result = facade.listByPage(query);
        assertThat(result.getCurrent()).isEqualTo(3);
        assertThat(result.getSize()).isEqualTo(25);
        assertThat(result.getTotal()).isEqualTo(75);
        assertThat(result.getPages()).isEqualTo(3);
        assertThat(result.getRecords()).containsExactly(mapped);
    }

    @Test
    void listByPageReturnsEmptyWhenServiceReturnsNullPage() {
        FacadePointQuery query = new FacadePointQuery();
        when(facadePointBuilder.toManagerQuery(query)).thenReturn(new PointQuery());
        when(pointService.list(any(PointQuery.class))).thenReturn(null);
        assertThat(facade.listByPage(query).getRecords()).isEmpty();
    }

}
