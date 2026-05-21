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

package io.github.pnoker.common.data.biz.repository;

import io.github.pnoker.common.data.dal.PointValueManager;
import io.github.pnoker.common.data.entity.builder.PointValueBuilder;
import io.github.pnoker.common.data.entity.model.PointValueDO;
import io.github.pnoker.common.data.mapper.PointValueMapper;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.bo.WindowAggregateResult;
import io.github.pnoker.common.entity.query.WindowAggregateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostgresRepositoryServiceImplWindowTest {

    @Mock
    private PointValueBuilder pointValueBuilder;

    @Mock
    private PointValueManager pointValueManager;

    @Mock
    private PointValueMapper pointValueMapper;

    @InjectMocks
    private PostgresRepositoryServiceImpl service;

    @Test
    void aggregateInWindowDelegatesToMapper() {
        WindowAggregateResult expected = new WindowAggregateResult(BigDecimal.valueOf(82.5), 4L);
        WindowAggregateRequest req = WindowAggregateRequest.builder()
                .tenantId(7L).deviceId(1L).pointId(11L)
                .function("AVG")
                .from(LocalDateTime.now().minusMinutes(5))
                .to(LocalDateTime.now())
                .build();
        when(pointValueMapper.aggregateInWindow(req)).thenReturn(expected);

        WindowAggregateResult result = service.aggregateInWindow(req);

        assertThat(result).isSameAs(expected);
        verify(pointValueMapper).aggregateInWindow(req);
    }

    @Test
    void aggregateInWindowReturnsEmptyWhenMapperReturnsNull() {
        WindowAggregateRequest req = WindowAggregateRequest.builder()
                .tenantId(7L).deviceId(1L).pointId(11L)
                .function("AVG")
                .from(LocalDateTime.now().minusMinutes(5))
                .to(LocalDateTime.now())
                .build();
        when(pointValueMapper.aggregateInWindow(req)).thenReturn(null);

        WindowAggregateResult result = service.aggregateInWindow(req);

        assertThat(result.value()).isNull();
        assertThat(result.sampleCount()).isZero();
    }

    @Test
    void aggregateInWindowGuardsAgainstIncompleteRequests() {
        // Missing from / to / function should never reach the mapper — the
        // alarm pipeline never produces those, but a null guard is cheap.
        assertThat(service.aggregateInWindow(null).sampleCount()).isZero();
        assertThat(service.aggregateInWindow(WindowAggregateRequest.builder().build()).sampleCount()).isZero();
        verify(pointValueMapper, never()).aggregateInWindow(any());
    }

    @Test
    void samplesInWindowDelegatesToMapperAndConvertsToBOs() {
        LocalDateTime from = LocalDateTime.now().minusMinutes(5);
        LocalDateTime to = LocalDateTime.now();
        PointValueDO row = new PointValueDO();
        row.setNumValue(80.0);
        when(pointValueMapper.samplesInWindow(eq(7L), eq(1L), eq(11L), eq(from), eq(to)))
                .thenReturn(List.of(row));
        when(pointValueBuilder.buildBOByDO(row)).thenReturn(new PointValueBO());

        List<PointValueBO> result = service.samplesInWindow(7L, 1L, 11L, from, to);

        ArgumentCaptor<PointValueDO> captor = ArgumentCaptor.forClass(PointValueDO.class);
        verify(pointValueBuilder).buildBOByDO(captor.capture());
        assertThat(captor.getValue()).isSameAs(row);
        assertThat(result).hasSize(1);
    }

    @Test
    void samplesInWindowGuardsAgainstNullArgs() {
        assertThat(service.samplesInWindow(null, 1L, 11L, LocalDateTime.now(), LocalDateTime.now())).isEmpty();
        assertThat(service.samplesInWindow(7L, null, 11L, LocalDateTime.now(), LocalDateTime.now())).isEmpty();
        assertThat(service.samplesInWindow(7L, 1L, null, LocalDateTime.now(), LocalDateTime.now())).isEmpty();
        assertThat(service.samplesInWindow(7L, 1L, 11L, null, LocalDateTime.now())).isEmpty();
        assertThat(service.samplesInWindow(7L, 1L, 11L, LocalDateTime.now(), null)).isEmpty();
        verify(pointValueMapper, never()).samplesInWindow(any(), any(), any(), any(), any());
    }

}
