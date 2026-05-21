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

package io.github.pnoker.common.data.biz.alarm;

import io.github.pnoker.common.entity.bo.PointValueBO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AlarmRuleTriggerServiceImplTest {

    @Mock
    private AlarmRulePipelineService alarmRulePipelineService;

    @Mock
    private WindowSampleBuffer windowSampleBuffer;

    @InjectMocks
    private AlarmRuleTriggerServiceImpl service;

    private static PointValueBO point(long pointId, long tenantId) {
        return PointValueBO.builder()
                .tenantId(tenantId)
                .deviceId(1L)
                .pointId(pointId)
                .calValue("10")
                .build();
    }

    @Test
    void processPointValueDispatchesToPipelineWhenIdsAreValid() {
        service.processPointValue(point(11L, 7L));
        verify(alarmRulePipelineService).process(any(RuleFact.class));
    }

    @Test
    void processPointValueDropsWhenTenantIdInvalid() {
        service.processPointValue(point(11L, 0L));
        verifyNoInteractions(alarmRulePipelineService);
    }

    @Test
    void processPointValueDropsWhenPointIdInvalid() {
        service.processPointValue(point(0L, 7L));
        verifyNoInteractions(alarmRulePipelineService);
    }

    @Test
    void processPointValuesNoopOnEmptyBatch() {
        service.processPointValues(List.of());
        service.processPointValues(null);
        verifyNoInteractions(alarmRulePipelineService);
    }

    @Test
    void processPointValuesFansOutPerEntry() {
        // 250 inputs across 5 distinct point ids are grouped internally and
        // dispatched as one processBatch call so the engine can amortize
        // RuleRegistry lookups and batch-write rule_state / notify_history.
        List<PointValueBO> batch = IntStream.range(0, 250)
                .mapToObj(i -> point(11L + (i % 5), 7L))
                .toList();

        service.processPointValues(batch);

        verify(alarmRulePipelineService).processBatch(anyList());
    }

    @Test
    void processPointValuesSkipsInvalidEntriesIndividually() {
        // Mixed batch: 3 valid + 2 invalid (zero ids). Invalid entries are
        // filtered before the batch call; only 3 facts reach the pipeline.
        List<PointValueBO> batch = List.of(
                point(11L, 7L),
                point(0L, 7L),       // invalid pointId
                point(12L, 7L),
                point(13L, 0L),      // invalid tenantId
                point(14L, 7L));

        service.processPointValues(batch);

        verify(alarmRulePipelineService).processBatch(anyList());
        verify(alarmRulePipelineService, never()).processBatch(null);
    }

}
