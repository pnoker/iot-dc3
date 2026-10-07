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
package io.github.pnoker.common.data.biz.insight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.data.biz.DashboardService;
import io.github.pnoker.common.data.biz.PointCommandHistoryService;
import io.github.pnoker.common.data.biz.analytics.DataAnalyticsService;
import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel;
import io.github.pnoker.common.data.entity.vo.dashboard.AlertStatsVO;
import io.github.pnoker.common.data.entity.vo.dashboard.TodayStatsVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class InsightDispatcherTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private DataAnalyticsService dataAnalyticsService;

    @Mock
    private PointCommandHistoryService pointCommandHistoryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rejectsUnknownOperation() {
        StepVerifier.create(dispatcher().invoke(11L, "nope", "{}"))
                .expectError(IllegalArgumentException.class)
                .verify();
        verifyNoInteractions(dashboardService);
    }

    @Test
    void rejectsInvalidTenant() {
        StepVerifier.create(dispatcher().invoke(0L, InsightFacadeOperations.ALERT_STATS, "{}"))
                .expectError(IllegalArgumentException.class)
                .verify();
        verifyNoInteractions(dashboardService);
    }

    @Test
    void alertStatsRoutesWithoutArguments() {
        AlertStatsVO stats = new AlertStatsVO();
        stats.setTotal(9);
        when(dashboardService.alertStats(11L)).thenReturn(Mono.just(stats));

        StepVerifier.create(dispatcher().invoke(11L, InsightFacadeOperations.ALERT_STATS, ""))
                .assertNext(payload -> assertThat(payload).isSameAs(stats))
                .verifyComplete();
    }

    @Test
    void alertLatestClampsTheLimit() {
        when(dashboardService.alertLatest(eq(11L), eq(100))).thenReturn(Mono.just(java.util.List.of()));

        StepVerifier.create(dispatcher().invoke(11L, InsightFacadeOperations.ALERT_LATEST, "{\"limit\":9999}"))
                .assertNext(payload -> assertThat(payload).isEqualTo(java.util.List.of()))
                .verifyComplete();
        verify(dashboardService).alertLatest(11L, 100);
    }

    @Test
    void statsTodayComputesPercentChange() {
        when(dashboardService.countToday(11L)).thenReturn(Mono.just(12L));
        when(dashboardService.countYesterday(11L)).thenReturn(Mono.just(10L));
        when(dashboardService.countTotal(11L)).thenReturn(Mono.just(100L));

        StepVerifier.create(dispatcher().invoke(11L, InsightFacadeOperations.STATS_TODAY, ""))
                .assertNext(payload -> {
                    TodayStatsVO stats = (TodayStatsVO) payload;
                    assertThat(stats.getToday()).isEqualTo(12);
                    assertThat(stats.getPercentChange()).isEqualTo(20);
                })
                .verifyComplete();
    }

    @Test
    void analyticsPayloadDeserializesForQueryLatest() {
        when(dataAnalyticsService.getLatest(eq(11L), any(AnalyticsModel.QueryLatestRequest.class)))
                .thenReturn(Mono.just(new AnalyticsModel.LatestValuesResponse("ok", 1, "NONE", java.util.List.of())));

        StepVerifier.create(dispatcher()
                        .invoke(
                                11L,
                                InsightFacadeOperations.QUERY_LATEST,
                                "{\"series\":[{\"deviceId\":1,\"pointId\":2}]}"))
                .assertNext(payload -> assertThat(payload).isInstanceOf(AnalyticsModel.LatestValuesResponse.class))
                .verifyComplete();
        verify(dataAnalyticsService).getLatest(eq(11L), any(AnalyticsModel.QueryLatestRequest.class));
    }

    @Test
    void pointCommandStatusRequiresCommandId() {
        StepVerifier.create(dispatcher().invoke(11L, InsightFacadeOperations.POINT_COMMAND_STATUS, "{}"))
                .expectError(IllegalArgumentException.class)
                .verify();
        verifyNoInteractions(pointCommandHistoryService);
    }

    @Test
    void pointCommandStatusRoutesByCommandId() {
        var history = new io.github.pnoker.common.data.entity.vo.PointCommandHistoryVO();
        history.setCommandId("cmd-9");
        when(pointCommandHistoryService.getByCommandId(11L, "cmd-9")).thenReturn(Mono.just(history));

        StepVerifier.create(dispatcher()
                        .invoke(11L, InsightFacadeOperations.POINT_COMMAND_STATUS, "{\"commandId\":\"cmd-9\"}"))
                .assertNext(payload -> assertThat(payload).isSameAs(history))
                .verifyComplete();
    }

    @Test
    void malformedRequestJsonFailsLoudly() {
        StepVerifier.create(dispatcher().invoke(11L, InsightFacadeOperations.ALERT_LATEST, "{broken"))
                .expectError(Exception.class)
                .verify();
        verifyNoInteractions(dashboardService);
    }

    private InsightDispatcher dispatcher() {
        return new InsightDispatcher(dashboardService, dataAnalyticsService, pointCommandHistoryService, objectMapper);
    }

    /** Local aliases keeping the test lines short. */
    private static final class InsightFacadeOperations {
        private static final String ALERT_STATS =
                io.github.pnoker.common.facade.api.InsightFacade.Operations.ALERT_STATS;
        private static final String ALERT_LATEST =
                io.github.pnoker.common.facade.api.InsightFacade.Operations.ALERT_LATEST;
        private static final String STATS_TODAY =
                io.github.pnoker.common.facade.api.InsightFacade.Operations.STATS_TODAY;
        private static final String QUERY_LATEST =
                io.github.pnoker.common.facade.api.InsightFacade.Operations.QUERY_LATEST;
        private static final String POINT_COMMAND_STATUS =
                io.github.pnoker.common.facade.api.InsightFacade.Operations.POINT_COMMAND_STATUS;
    }
}
