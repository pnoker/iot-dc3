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
import io.github.pnoker.common.data.entity.vo.dashboard.AgingBacklogVO;
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

    @Test
    void everyCanonicalOperationRoutesWithoutUnknownErrors() {
        String[] operations = {
            io.github.pnoker.common.facade.api.InsightFacade.Operations.ALERT_STATS,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.ALERT_LATEST,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.ALERT_PAGE,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.ALERT_TREND,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.ALERT_TOP_SOURCES,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.ALERT_TYPE_DISTRIBUTION,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.ALERT_AGING,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.ALERT_MTTA,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.STATS_TODAY,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.STATS_TIMESERIES,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.STATS_TOP,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.STATS_LATEST_STREAM,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.STATS_LATENCY,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.STATS_ACTIVITY,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.PROTOCOL_HEALTH,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.QUERY_LATEST,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.QUERY_HISTORY,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.COMPUTE_STATS,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.COMPARE_PERIODS,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.RANK_ENTITIES,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.TREND_ANALYSIS,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.THRESHOLD_REPORT,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.CORRELATE,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.DATA_QUALITY_REPORT,
            io.github.pnoker.common.facade.api.InsightFacade.Operations.POINT_COMMAND_STATUS,
        };
        // stub every service with benign empty responses so each operation completes
        when(dashboardService.countToday(11L)).thenReturn(Mono.just(0L));
        when(dashboardService.countYesterday(11L)).thenReturn(Mono.just(0L));
        when(dashboardService.countTotal(11L)).thenReturn(Mono.just(0L));
        when(dashboardService.alertStats(11L)).thenReturn(Mono.just(new AlertStatsVO()));
        when(dashboardService.alertAgingBacklog(11L)).thenReturn(Mono.just(new AgingBacklogVO()));
        when(dashboardService.alertLatest(eq(11L), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(Mono.just(java.util.List.of()));
        when(dashboardService.alertTrend(eq(11L), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(Mono.just(java.util.List.of()));
        when(dashboardService.alertTopSources(
                        eq(11L), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(Mono.just(java.util.List.of()));
        when(dashboardService.alertTypeDistribution(eq(11L), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(Mono.just(java.util.List.of()));
        when(dashboardService.alertMtta(eq(11L), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(Mono.just(java.util.List.of()));
        when(dashboardService.timeseries(
                        eq(11L), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(Mono.just(java.util.List.of()));
        when(dashboardService.top(
                        eq(11L),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyInt(),
                        org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(Mono.just(java.util.List.of()));
        when(dashboardService.latestStream(eq(11L), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(Mono.just(java.util.List.of()));
        when(dashboardService.latencyHistogram(eq(11L), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(Mono.just(java.util.List.of()));
        when(dashboardService.hourlyActivity(eq(11L), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(Mono.just(java.util.List.of()));
        when(dashboardService.protocolHealth(11L)).thenReturn(Mono.just(java.util.List.of()));
        when(dashboardService.alertPage(
                        eq(11L),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(Mono.just(io.github.pnoker.db.core.page.OffsetPage.of(java.util.List.of(), 0, 50, 0)));
        when(dataAnalyticsService.getLatest(eq(11L), any()))
                .thenReturn(Mono.just(new AnalyticsModel.LatestValuesResponse("ok", 0, "NONE", java.util.List.of())));
        when(dataAnalyticsService.getHistory(eq(11L), any()))
                .thenReturn(Mono.just(new AnalyticsModel.HistoryResponse("ok", 0, "NONE", java.util.Map.of())));
        when(dataAnalyticsService.computeStats(eq(11L), any()))
                .thenReturn(Mono.just(new AnalyticsModel.StatsResponse("ok", 0, "NONE", java.util.Map.of())));
        when(dataAnalyticsService.comparePeriods(eq(11L), any()))
                .thenReturn(Mono.just(new AnalyticsModel.CompareResponse("ok", 0, "NONE", java.util.Map.of())));
        when(dataAnalyticsService.rankEntities(eq(11L), any()))
                .thenReturn(Mono.just(new AnalyticsModel.RankResponse("ok", 0, "NONE", java.util.List.of())));
        when(dataAnalyticsService.trendAnalysis(eq(11L), any()))
                .thenReturn(Mono.just(new AnalyticsModel.TrendResponse("ok", 0, "NONE", java.util.Map.of())));
        when(dataAnalyticsService.thresholdReport(eq(11L), any()))
                .thenReturn(Mono.just(new AnalyticsModel.ThresholdResponse("ok", 0, "NONE", java.util.Map.of())));
        when(dataAnalyticsService.correlate(eq(11L), any()))
                .thenReturn(Mono.just(new AnalyticsModel.CorrelationResponse("ok", 0, "NONE", 0.0, 0, "STORE")));
        when(dataAnalyticsService.qualityReport(eq(11L), any()))
                .thenReturn(Mono.just(new AnalyticsModel.QualityResponse(
                        "ok", 0, "NONE", 0, 0, 0.0, java.util.List.of(), java.util.Map.of())));
        var history = new io.github.pnoker.common.data.entity.vo.PointCommandHistoryVO();
        when(pointCommandHistoryService.getByCommandId(11L, "cmd-x")).thenReturn(Mono.just(history));

        InsightDispatcher dispatcher = dispatcher();
        for (String operation : operations) {
            String requestJson =
                    operation.equals(InsightFacadeOperations.POINT_COMMAND_STATUS) ? "{\"commandId\":\"cmd-x\"}" : "{}";
            try {
                StepVerifier.create(dispatcher.invoke(11L, operation, requestJson))
                        .expectNextCount(1)
                        .verifyComplete();
            } catch (AssertionError error) {
                throw new AssertionError("operation " + operation + " must route: " + error.getMessage(), error);
            }
        }
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
