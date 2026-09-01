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

import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.CursorPage;
import reactor.core.publisher.Mono;
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
        when(pointValueService.latest(any(PointValueQuery.class))).thenReturn(Mono.empty());
        assertThat(facade.lastValue(1L, 2L, 3L).block()).isNull();
        verify(facadePointValueBuilder, never()).toFacadeBO(any());
    }

    @Test
    void lastValueReturnsNullWhenLatestPageEmpty() {
        OffsetPage<PointValueBO> page = OffsetPage.of(List.of(), 0, 1, 0);
        when(pointValueService.latest(any(PointValueQuery.class))).thenReturn(Mono.just(page));
        assertThat(facade.lastValue(1L, 2L, 3L).block()).isNull();
    }

    @Test
    void lastValueMapsFirstRecordThroughBuilderAndPassesQueryFields() {
        PointValueBO bo = new PointValueBO();
        FacadePointValueBO mapped = new FacadePointValueBO();
        OffsetPage<PointValueBO> page = OffsetPage.of(List.of(bo), 0, 1, 1);
        when(pointValueService.latest(any(PointValueQuery.class))).thenReturn(Mono.just(page));
        when(facadePointValueBuilder.toFacadeBO(bo)).thenReturn(mapped);

        assertThat(facade.lastValue(1L, 2L, 3L).block()).isSameAs(mapped);

        ArgumentCaptor<PointValueQuery> captor = ArgumentCaptor.forClass(PointValueQuery.class);
        verify(pointValueService).latest(captor.capture());
        PointValueQuery passed = captor.getValue();
        assertThat(passed.getTenantId()).isEqualTo(1L);
        assertThat(passed.getDeviceId()).isEqualTo(2L);
        assertThat(passed.getPointId()).isEqualTo(3L);
    }

    @Test
    void historyReturnsEmptyForNullOrEmptyServiceResult() {
        when(pointValueService.history(1L, 2L, 3L, null, 10)).thenReturn(Mono.empty());
        assertThat(facade.history(1L, 2L, 3L, null, 10).block()).isNull();
        when(pointValueService.history(1L, 2L, 3L, null, 10)).thenReturn(Mono.just(CursorPage.of(List.of(), null)));
        assertThat(facade.history(1L, 2L, 3L, null, 10).block().items()).isEmpty();
    }

    @Test
    void historyForwardsServiceResultUnchanged() {
        when(pointValueService.history(1L, 2L, 3L, null, 10)).thenReturn(Mono.just(CursorPage.of(List.of(
                PointValueBO.builder().calValue("23.5").build(),
                PointValueBO.builder().calValue("24.0").build()), null)));
        when(facadePointValueBuilder.toFacadeBO(any(PointValueBO.class))).thenAnswer(inv ->
                FacadePointValueBO.builder().value(inv.<PointValueBO>getArgument(0).getCalValue()).build());
        assertThat(facade.history(1L, 2L, 3L, null, 10).block().items())
                .map(FacadePointValueBO::getValue).containsExactly("23.5", "24.0");
    }

}
