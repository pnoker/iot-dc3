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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointLocalFacadeTest {

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

    @Test
    void getByIdReturnsNullWhenServiceReturnsNull() {
        when(pointService.getById(1L)).thenReturn(null);
        assertThat(facade.getById(1L)).isNull();
        verify(facadePointBuilder, never()).toFacadeBO(any());
    }

    @Test
    void getByIdMapsThroughBuilder() {
        PointBO bo = new PointBO();
        FacadePointBO mapped = new FacadePointBO();
        when(pointService.getById(1L)).thenReturn(bo);
        when(facadePointBuilder.toFacadeBO(bo)).thenReturn(mapped);
        assertThat(facade.getById(1L)).isSameAs(mapped);
    }

    @Test
    void listByIdsReturnsEmptyForNullOrEmptyInput() {
        assertThat(facade.listByIds(null)).isEmpty();
        assertThat(facade.listByIds(Set.of())).isEmpty();
    }

    @Test
    void listByIdsMapsEachThroughBuilder() {
        PointBO bo = new PointBO();
        FacadePointBO mapped = new FacadePointBO();
        when(pointService.listByIds(any())).thenReturn(List.of(bo));
        when(facadePointBuilder.toFacadeBO(bo)).thenReturn(mapped);
        assertThat(facade.listByIds(Set.of(1L))).containsExactly(mapped);
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
