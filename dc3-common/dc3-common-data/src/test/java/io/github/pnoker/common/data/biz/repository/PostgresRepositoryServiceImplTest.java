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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostgresRepositoryServiceImplTest {

    @Mock
    private PointValueBuilder pointValueBuilder;

    @Mock
    private PointValueManager pointValueManager;

    @Mock
    private PointValueMapper pointValueMapper;

    @InjectMocks
    private PostgresRepositoryServiceImpl service;

    private PointValueBO numericBO;
    private PointValueBO stringBO;
    private PointValueDO numericDO;
    private PointValueDO stringDO;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        numericBO = PointValueBO.builder()
                .messageId("m-1").schemaVersion(1).driverNode("node-a").sequence(1L)
                .deviceId(1L).pointId(10L).driverId(20L).tenantId(100L)
                .rawValue("42").calValue("42.5").numValue(42.5)
                .createTime(now).operateTime(now).build();
        stringBO = PointValueBO.builder()
                .messageId("m-2").schemaVersion(1).driverNode("node-a").sequence(2L)
                .deviceId(2L).pointId(20L).driverId(20L).tenantId(100L)
                .rawValue("on").calValue("on").createTime(now).operateTime(now).build();

        numericDO = new PointValueDO();
        numericDO.setMessageId("m-1");
        numericDO.setNumValue(42.5);
        stringDO = new PointValueDO();
        stringDO.setMessageId("m-2");
    }

    @Test
    void savesHistoryAndLatestProjectionFromSameConvertedBatch() {
        List<PointValueBO> input = List.of(numericBO, stringBO);
        List<PointValueDO> converted = List.of(numericDO, stringDO);
        when(pointValueBuilder.buildDOListByBOList(input)).thenReturn(converted);
        when(pointValueMapper.insertHistoryBatch(converted)).thenReturn(List.of("m-1", "m-2"));

        service.savePointValues(input);

        var order = inOrder(pointValueMapper);
        order.verify(pointValueMapper).insertHistoryBatch(converted);
        order.verify(pointValueMapper).upsertLatestBatch(converted);
        assertThat(converted).extracting(PointValueDO::getNumValue)
                .containsExactly(42.5, null);
        verifyNoInteractions(pointValueManager);
    }

    @Test
    void singleSaveUsesTheSameTransactionalBatchPath() {
        when(pointValueBuilder.buildDOListByBOList(anyList())).thenReturn(List.of(numericDO));
        when(pointValueMapper.insertHistoryBatch(List.of(numericDO))).thenReturn(List.of("m-1"));

        service.savePointValue(numericBO);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PointValueDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(pointValueMapper).insertHistoryBatch(captor.capture());
        verify(pointValueMapper).upsertLatestBatch(captor.getValue());
        assertThat(captor.getValue()).extracting(PointValueDO::getMessageId)
                .containsExactly("m-1");
    }

    @Test
    void emptyBatchDoesNotTouchDatabase() {
        service.savePointValues(List.of());
        verifyNoInteractions(pointValueBuilder, pointValueMapper, pointValueManager);
    }

    @Test
    void duplicateMessageIdCannotUpsertTheSameLatestKeyTwice() {
        List<PointValueBO> input = List.of(numericBO, numericBO);
        when(pointValueBuilder.buildDOListByBOList(input)).thenReturn(List.of(numericDO, numericDO));
        when(pointValueMapper.insertHistoryBatch(anyList())).thenReturn(List.of("m-1"));

        List<PointValueBO> accepted = service.savePointValues(input);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PointValueDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(pointValueMapper).upsertLatestBatch(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(accepted).containsExactly(numericBO);
    }

    @Test
    void latestReadsSharedProjection() {
        when(pointValueMapper.selectLatestPointValues(100L, 1L, List.of(10L)))
                .thenReturn(List.of(numericDO));
        when(pointValueBuilder.buildBOListByDOList(List.of(numericDO)))
                .thenReturn(List.of(numericBO));

        List<PointValueBO> result = service.listLatestPointValues(100L, 1L, List.of(10L));

        assertThat(result).containsExactly(numericBO);
    }
}
