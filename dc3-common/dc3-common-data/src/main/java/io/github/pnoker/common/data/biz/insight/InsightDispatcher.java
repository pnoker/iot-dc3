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

import io.github.pnoker.common.data.biz.DashboardService;
import io.github.pnoker.common.data.biz.PointCommandHistoryService;
import io.github.pnoker.common.data.biz.analytics.DataAnalyticsService;
import io.github.pnoker.common.data.entity.query.AlertPageQuery;
import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel;
import io.github.pnoker.common.data.entity.vo.dashboard.TodayStatsVO;
import io.github.pnoker.common.utils.TimeRangeUtil;
import io.github.pnoker.db.core.page.PageRequest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/**
 * Single routing implementation behind the agent-facing insight channel: it maps canonical
 * operation names onto the dashboard, analytics and point-command services. Both the gRPC
 * transport ({@code InsightServer}) and the in-process transport ({@code InsightLocalFacade})
 * delegate here so the operation surface has exactly one implementation.
 *
 * <p>Requests arrive as JSON objects; numeric arguments are clamped defensively because the
 * caller is an LLM, not a form-validated UI. Responses are the underlying service payloads;
 * the transport layer serializes them to JSON.</p>
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InsightDispatcher {

    private static final int MAX_LIMIT = 100;
    private static final int MAX_DAYS = 365;
    private static final int MAX_HOURS = 24 * 365;

    private static final Set<String> OPERATIONS = Set.of(
            "alert_stats",
            "alert_latest",
            "alert_page",
            "alert_trend",
            "alert_top_sources",
            "alert_type_distribution",
            "alert_aging",
            "alert_mtta",
            "stats_today",
            "stats_timeseries",
            "stats_top",
            "stats_latest_stream",
            "stats_latency",
            "stats_activity",
            "protocol_health",
            "query_latest",
            "query_history",
            "compute_stats",
            "compare_periods",
            "rank_entities",
            "trend_analysis",
            "threshold_report",
            "correlate",
            "data_quality_report",
            "point_command_status");

    private final DashboardService dashboardService;
    private final DataAnalyticsService dataAnalyticsService;
    private final PointCommandHistoryService pointCommandHistoryService;
    private final ObjectMapper objectMapper;

    /**
     * Invoke one insight operation by name.
     *
     * @param tenantId tenant id
     * @param operation canonical operation name
     * @param requestJson JSON-encoded request payload, may be blank for no-argument operations
     * @return the service response payload mono
     */
    public Mono<Object> invoke(Long tenantId, String operation, String requestJson) {
        if (Objects.isNull(tenantId) || tenantId <= 0) {
            return Mono.error(new IllegalArgumentException("tenantId is required"));
        }
        if (!OPERATIONS.contains(operation)) {
            return Mono.error(new IllegalArgumentException("Unknown insight operation: " + operation));
        }
        return Mono.defer(() -> route(tenantId, operation, readRequest(requestJson)));
    }

    private Mono<Object> route(Long tenantId, String operation, Map<String, Object> request) {
        return switch (operation) {
            case "alert_stats" -> dashboardService.alertStats(tenantId).map(Object.class::cast);
            case "alert_latest" ->
                dashboardService
                        .alertLatest(tenantId, clamp(intValue(request, "limit", 10), 1, MAX_LIMIT))
                        .map(Object.class::cast);
            case "alert_page" -> alertPage(tenantId, request);
            case "alert_trend" ->
                dashboardService
                        .alertTrend(tenantId, clamp(intValue(request, "days", 30), 1, MAX_DAYS))
                        .map(Object.class::cast);
            case "alert_top_sources" ->
                dashboardService
                        .alertTopSources(
                                tenantId,
                                clamp(intValue(request, "days", 30), 1, MAX_DAYS),
                                clamp(intValue(request, "limit", 10), 1, MAX_LIMIT))
                        .map(Object.class::cast);
            case "alert_type_distribution" ->
                dashboardService
                        .alertTypeDistribution(tenantId, clamp(intValue(request, "days", 30), 1, MAX_DAYS))
                        .map(Object.class::cast);
            case "alert_aging" -> dashboardService.alertAgingBacklog(tenantId).map(Object.class::cast);
            case "alert_mtta" ->
                dashboardService
                        .alertMtta(tenantId, clamp(intValue(request, "days", 30), 1, MAX_DAYS))
                        .map(Object.class::cast);
            case "stats_today" -> statsToday(tenantId);
            case "stats_timeseries" ->
                dashboardService
                        .timeseries(
                                tenantId,
                                stringValue(request, "granularity", "hour"),
                                clamp(intValue(request, "rangeHours", 24), 1, MAX_HOURS))
                        .map(Object.class::cast);
            case "stats_top" ->
                dashboardService
                        .top(
                                tenantId,
                                stringValue(request, "dimension", "device"),
                                clamp(intValue(request, "rangeHours", 24), 1, MAX_HOURS),
                                clamp(intValue(request, "limit", 10), 1, MAX_LIMIT))
                        .map(Object.class::cast);
            case "stats_latest_stream" ->
                dashboardService
                        .latestStream(tenantId, clamp(intValue(request, "limit", 20), 1, MAX_LIMIT))
                        .map(Object.class::cast);
            case "stats_latency" ->
                dashboardService
                        .latencyHistogram(tenantId, clamp(intValue(request, "rangeHours", 24), 1, MAX_HOURS))
                        .map(Object.class::cast);
            case "stats_activity" ->
                dashboardService
                        .hourlyActivity(tenantId, clamp(intValue(request, "rangeHours", 168), 1, MAX_HOURS))
                        .map(Object.class::cast);
            case "protocol_health" -> dashboardService.protocolHealth(tenantId).map(Object.class::cast);
            case "query_latest" ->
                dataAnalyticsService
                        .getLatest(tenantId, readAnalytics(request, AnalyticsModel.QueryLatestRequest.class))
                        .map(Object.class::cast);
            case "query_history" ->
                dataAnalyticsService
                        .getHistory(tenantId, readAnalytics(request, AnalyticsModel.QueryHistoryRequest.class))
                        .map(Object.class::cast);
            case "compute_stats" ->
                dataAnalyticsService
                        .computeStats(tenantId, readAnalytics(request, AnalyticsModel.ComputeStatsRequest.class))
                        .map(Object.class::cast);
            case "compare_periods" ->
                dataAnalyticsService
                        .comparePeriods(tenantId, readAnalytics(request, AnalyticsModel.ComparePeriodsRequest.class))
                        .map(Object.class::cast);
            case "rank_entities" ->
                dataAnalyticsService
                        .rankEntities(tenantId, readAnalytics(request, AnalyticsModel.RankEntitiesRequest.class))
                        .map(Object.class::cast);
            case "trend_analysis" ->
                dataAnalyticsService
                        .trendAnalysis(tenantId, readAnalytics(request, AnalyticsModel.TrendAnalysisRequest.class))
                        .map(Object.class::cast);
            case "threshold_report" ->
                dataAnalyticsService
                        .thresholdReport(tenantId, readAnalytics(request, AnalyticsModel.ThresholdReportRequest.class))
                        .map(Object.class::cast);
            case "correlate" ->
                dataAnalyticsService
                        .correlate(tenantId, readAnalytics(request, AnalyticsModel.CorrelateRequest.class))
                        .map(Object.class::cast);
            case "data_quality_report" ->
                dataAnalyticsService
                        .qualityReport(tenantId, readAnalytics(request, AnalyticsModel.QualityReportRequest.class))
                        .map(Object.class::cast);
            case "point_command_status" -> pointCommandStatus(tenantId, request);
            default -> Mono.error(new IllegalArgumentException("Unknown insight operation: " + operation));
        };
    }

    private Mono<Object> alertPage(Long tenantId, Map<String, Object> request) {
        AlertPageQuery query = new AlertPageQuery();
        query.setSource(stringValueOrNull(request, "source"));
        query.setSourceId(longValueOrNull(request, "sourceId"));
        query.setAlarmTypeFlag(intValueOrNull(request, "alarmTypeFlag"));
        query.setConfirmFlag(intValueOrNull(request, "confirmFlag"));
        query.setRangeKey(stringValueOrNull(request, "rangeKey"));
        query.setOffset(Math.max(0, longValue(request, "offset", 0)));
        query.setLimit(clamp(intValue(request, "limit", 50), 1, PageRequest.MAX_LIMIT));
        LocalDateTime from = TimeRangeUtil.resolveFrom(query.getRangeKey(), null);
        PageRequest page = new PageRequest(query.getOffset(), query.getLimit());
        return dashboardService
                .alertPage(
                        tenantId,
                        query.getSource(),
                        query.getSourceId(),
                        query.getAlarmTypeFlag(),
                        query.getConfirmFlag(),
                        from,
                        page)
                .map(Object.class::cast);
    }

    private Mono<Object> statsToday(Long tenantId) {
        return Mono.zip(
                        dashboardService.countToday(tenantId),
                        dashboardService.countYesterday(tenantId),
                        dashboardService.countTotal(tenantId))
                .map(values -> {
                    long today = values.getT1();
                    long yesterday = values.getT2();
                    long percentChange = yesterday > 0
                            ? Math.round(((double) (today - yesterday) * 100.0) / yesterday)
                            : today > 0 ? 100 : 0;
                    return (Object) new TodayStatsVO(today, yesterday, values.getT3(), percentChange);
                });
    }

    private Mono<Object> pointCommandStatus(Long tenantId, Map<String, Object> request) {
        String commandId = stringValueOrNull(request, "commandId");
        if (Objects.isNull(commandId) || commandId.isBlank()) {
            return Mono.error(new IllegalArgumentException("commandId is required"));
        }
        return pointCommandHistoryService.getByCommandId(tenantId, commandId).map(Object.class::cast);
    }

    private Map<String, Object> readRequest(String requestJson) {
        if (Objects.isNull(requestJson) || requestJson.isBlank()) {
            return Map.of();
        }
        return objectMapper.readValue(requestJson, Map.class);
    }

    private <T> T readAnalytics(Map<String, Object> request, Class<T> type) {
        return objectMapper.readValue(objectMapper.writeValueAsString(request), type);
    }

    private static int intValue(Map<String, Object> request, String key, int defaultValue) {
        Object value = request.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ignored) {
                // fall through to the default
            }
        }
        return defaultValue;
    }

    private static Integer intValueOrNull(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(key + " must be an integer");
        }
    }

    private static long longValue(Map<String, Object> request, String key, long defaultValue) {
        Object value = request.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException ignored) {
                // fall through to the default
            }
        }
        return defaultValue;
    }

    private static Long longValueOrNull(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(key + " must be a long");
        }
    }

    private static String stringValue(Map<String, Object> request, String key, String defaultValue) {
        Object value = request.get(key);
        if (value == null || value.toString().isBlank()) {
            return defaultValue;
        }
        return value.toString();
    }

    private static String stringValueOrNull(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return value.toString();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
