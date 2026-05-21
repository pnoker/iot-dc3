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
import io.github.pnoker.common.data.biz.PointValueService;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.query.PointValueQuery;
import io.github.pnoker.common.facade.entity.bo.FacadePointValueBO;
import io.github.pnoker.common.facade.local.builder.FacadePointValueBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointValueLocalFacadeTest {

    @Mock
    private PointValueService pointValueService;

    @Mock
    private FacadePointValueBuilder facadePointValueBuilder;

    private PointValueLocalFacade facade;

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static <T> T any(Class<T> clazz) {
        return org.mockito.ArgumentMatchers.any(clazz);
    }

    @BeforeEach
    void setUp() {
        facade = new PointValueLocalFacade(pointValueService, facadePointValueBuilder);
    }

    @Test
    void lastValueReturnsNullWhenLatestPageMissing() {
        when(pointValueService.latest(any(PointValueQuery.class))).thenReturn(null);
        assertThat(facade.lastValue(1L, 2L, 3L)).isNull();
        verify(facadePointValueBuilder, never()).toFacadeBO(any());
    }

    @Test
    void lastValueReturnsNullWhenLatestPageEmpty() {
        Page<PointValueBO> page = new Page<>(1, 1);
        page.setRecords(List.of());
        when(pointValueService.latest(any(PointValueQuery.class))).thenReturn(page);
        assertThat(facade.lastValue(1L, 2L, 3L)).isNull();
    }

    @Test
    void lastValueMapsFirstRecordThroughBuilderAndPassesQueryFields() {
        PointValueBO bo = new PointValueBO();
        FacadePointValueBO mapped = new FacadePointValueBO();
        Page<PointValueBO> page = new Page<>(1, 1);
        page.setRecords(List.of(bo));
        when(pointValueService.latest(any(PointValueQuery.class))).thenReturn(page);
        when(facadePointValueBuilder.toFacadeBO(bo)).thenReturn(mapped);

        assertThat(facade.lastValue(1L, 2L, 3L)).isSameAs(mapped);

        ArgumentCaptor<PointValueQuery> captor = ArgumentCaptor.forClass(PointValueQuery.class);
        verify(pointValueService).latest(captor.capture());
        PointValueQuery passed = captor.getValue();
        assertThat(passed.getTenantId()).isEqualTo(1L);
        assertThat(passed.getDeviceId()).isEqualTo(2L);
        assertThat(passed.getPointId()).isEqualTo(3L);
    }

    @Test
    void historyReturnsEmptyForNullOrEmptyServiceResult() {
        when(pointValueService.history(1L, 2L, 3L, 10)).thenReturn(null);
        assertThat(facade.history(1L, 2L, 3L, 10)).isEmpty();
        when(pointValueService.history(1L, 2L, 3L, 10)).thenReturn(List.of());
        assertThat(facade.history(1L, 2L, 3L, 10)).isEmpty();
    }

    @Test
    void historyForwardsServiceResultUnchanged() {
        when(pointValueService.history(1L, 2L, 3L, 10)).thenReturn(List.of("23.5", "24.0"));
        assertThat(facade.history(1L, 2L, 3L, 10)).containsExactly("23.5", "24.0");
    }

}
