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
package io.github.pnoker.common.agentic.tools;

import io.github.pnoker.common.agentic.entity.model.AgenticToolResult;
import io.github.pnoker.common.agentic.utils.AgenticToolContextUtil;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.facade.api.InsightFacade;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/**
 * Insight tools exposing alert analytics, dashboard stats, analytics queries and the point
 * command status loop through the {@link InsightFacade} insight channel.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InsightTool {

    private static final String DOMAIN = "insight";

    private final Optional<InsightFacade> insightFacade;

    private final ObjectMapper objectMapper;

    /**
     * Return the tenant's current alert totals with a breakdown by alarm type.
     *
     * @param toolContext tool context
     * @return alert stats result
     */
    public Mono<AgenticToolResult<Object>> getAlertStats(ToolContext toolContext) {
        return invoke(
                InsightFacade.Operations.ALERT_STATS, Map.of(), "Alert stats loaded", "getAlertStats", toolContext);
    }

    /**
     * Return the most recent alerts, newest first.
     *
     * @param limit maximum number of alerts, 1..100
     * @param toolContext tool context
     * @return latest alerts result
     */
    public Mono<AgenticToolResult<Object>> getLatestAlerts(Integer limit, ToolContext toolContext) {
        Map<String, Object> request = new LinkedHashMap<>();
        if (Objects.nonNull(limit)) {
            request.put("limit", limit);
        }
        return invoke(
                InsightFacade.Operations.ALERT_LATEST, request, "Latest alerts loaded", "getLatestAlerts", toolContext);
    }

    /**
     * Page through alerts with optional filters.
     *
     * @param source alert source filter: device or driver
     * @param sourceId source entity id filter
     * @param alarmTypeFlag alarm type flag filter
     * @param confirmFlag confirm flag filter
     * @param rangeKey preset range key: today, 24h, 7d or 30d
     * @param offset canonical pagination offset
     * @param limit canonical pagination limit, 1..200
     * @param toolContext tool context
     * @return alert page result
     */
    public Mono<AgenticToolResult<Object>> pageAlerts(
            String source,
            Long sourceId,
            Integer alarmTypeFlag,
            Integer confirmFlag,
            String rangeKey,
            long offset,
            int limit,
            ToolContext toolContext) {
        if (offset < 0) {
            return Mono.just(AgenticToolResult.invalid("Offset must be non-negative."));
        }
        if (limit < 1 || limit > 200) {
            return Mono.just(AgenticToolResult.invalid("Limit must be between 1 and 200."));
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("source", source);
        request.put("sourceId", sourceId);
        request.put("alarmTypeFlag", alarmTypeFlag);
        request.put("confirmFlag", confirmFlag);
        request.put("rangeKey", rangeKey);
        request.put("offset", offset);
        request.put("limit", limit);
        return invoke(InsightFacade.Operations.ALERT_PAGE, request, "Alert page loaded", "pageAlerts", toolContext);
    }

    /**
     * Return the daily alert trend over the last days.
     *
     * @param days trend window in days, 1..365
     * @param toolContext tool context
     * @return alert trend result
     */
    public Mono<AgenticToolResult<Object>> getAlertTrend(Integer days, ToolContext toolContext) {
        Map<String, Object> request = new LinkedHashMap<>();
        if (Objects.nonNull(days)) {
            request.put("days", days);
        }
        return invoke(
                InsightFacade.Operations.ALERT_TREND, request, "Alert trend loaded", "getAlertTrend", toolContext);
    }

    /**
     * Return the noisiest alert sources over the last days.
     *
     * @param days window in days, 1..365
     * @param limit maximum number of sources, 1..100
     * @param toolContext tool context
     * @return top alert sources result
     */
    public Mono<AgenticToolResult<Object>> getAlertTopSources(Integer days, Integer limit, ToolContext toolContext) {
        Map<String, Object> request = new LinkedHashMap<>();
        if (Objects.nonNull(days)) {
            request.put("days", days);
        }
        if (Objects.nonNull(limit)) {
            request.put("limit", limit);
        }
        return invoke(
                InsightFacade.Operations.ALERT_TOP_SOURCES,
                request,
                "Alert top sources loaded",
                "getAlertTopSources",
                toolContext);
    }

    /**
     * Return the alert type distribution over the last days.
     *
     * @param days window in days, 1..365
     * @param toolContext tool context
     * @return alert type distribution result
     */
    public Mono<AgenticToolResult<Object>> getAlertTypeDistribution(Integer days, ToolContext toolContext) {
        Map<String, Object> request = new LinkedHashMap<>();
        if (Objects.nonNull(days)) {
            request.put("days", days);
        }
        return invoke(
                InsightFacade.Operations.ALERT_TYPE_DISTRIBUTION,
                request,
                "Alert type distribution loaded",
                "getAlertTypeDistribution",
                toolContext);
    }

    /**
     * Return the unconfirmed alert aging backlog buckets.
     *
     * @param toolContext tool context
     * @return aging backlog result
     */
    public Mono<AgenticToolResult<Object>> getAlertAgingBacklog(ToolContext toolContext) {
        return invoke(
                InsightFacade.Operations.ALERT_AGING,
                Map.of(),
                "Alert aging backlog loaded",
                "getAlertAgingBacklog",
                toolContext);
    }

    /**
     * Return the mean-time-to-acknowledge trend over the last days.
     *
     * @param days window in days, 1..365
     * @param toolContext tool context
     * @return mtta trend result
     */
    public Mono<AgenticToolResult<Object>> getAlertMtta(Integer days, ToolContext toolContext) {
        Map<String, Object> request = new LinkedHashMap<>();
        if (Objects.nonNull(days)) {
            request.put("days", days);
        }
        return invoke(InsightFacade.Operations.ALERT_MTTA, request, "Alert MTTA loaded", "getAlertMtta", toolContext);
    }

    /**
     * Return today's, yesterday's and cumulative entity counts with day-over-day change.
     *
     * @param toolContext tool context
     * @return today stats result
     */
    public Mono<AgenticToolResult<Object>> getTodayStats(ToolContext toolContext) {
        return invoke(
                InsightFacade.Operations.STATS_TODAY, Map.of(), "Today stats loaded", "getTodayStats", toolContext);
    }

    /**
     * Return bucketed entity counts over a rolling window.
     *
     * @param granularity time bucket granularity: hour or day
     * @param rangeHours rolling window in hours, 1..8760
     * @param toolContext tool context
     * @return timeseries result
     */
    public Mono<AgenticToolResult<Object>> getStatsTimeseries(
            String granularity, Integer rangeHours, ToolContext toolContext) {
        Map<String, Object> request = new LinkedHashMap<>();
        if (Objects.nonNull(granularity) && !granularity.isBlank()) {
            request.put("granularity", granularity);
        }
        if (Objects.nonNull(rangeHours)) {
            request.put("rangeHours", rangeHours);
        }
        return invoke(
                InsightFacade.Operations.STATS_TIMESERIES,
                request,
                "Stats timeseries loaded",
                "getStatsTimeseries",
                toolContext);
    }

    /**
     * Return the most active entities by dimension over a rolling window.
     *
     * @param dimension ranking dimension: device, point or driver
     * @param rangeHours rolling window in hours, 1..8760
     * @param limit maximum number of entities, 1..100
     * @param toolContext tool context
     * @return top entities result
     */
    public Mono<AgenticToolResult<Object>> getTopEntities(
            String dimension, Integer rangeHours, Integer limit, ToolContext toolContext) {
        Map<String, Object> request = new LinkedHashMap<>();
        if (Objects.nonNull(dimension) && !dimension.isBlank()) {
            request.put("dimension", dimension);
        }
        if (Objects.nonNull(rangeHours)) {
            request.put("rangeHours", rangeHours);
        }
        if (Objects.nonNull(limit)) {
            request.put("limit", limit);
        }
        return invoke(
                InsightFacade.Operations.STATS_TOP, request, "Top entities loaded", "getTopEntities", toolContext);
    }

    /**
     * Return the latest point value stream across devices.
     *
     * @param limit maximum number of values, 1..100
     * @param toolContext tool context
     * @return latest value stream result
     */
    public Mono<AgenticToolResult<Object>> getLatestValueStream(Integer limit, ToolContext toolContext) {
        Map<String, Object> request = new LinkedHashMap<>();
        if (Objects.nonNull(limit)) {
            request.put("limit", limit);
        }
        return invoke(
                InsightFacade.Operations.STATS_LATEST_STREAM,
                request,
                "Latest value stream loaded",
                "getLatestValueStream",
                toolContext);
    }

    /**
     * Return the point-value collection latency histogram over a rolling window.
     *
     * @param rangeHours rolling window in hours, 1..8760
     * @param toolContext tool context
     * @return latency histogram result
     */
    public Mono<AgenticToolResult<Object>> getLatencyHistogram(Integer rangeHours, ToolContext toolContext) {
        Map<String, Object> request = new LinkedHashMap<>();
        if (Objects.nonNull(rangeHours)) {
            request.put("rangeHours", rangeHours);
        }
        return invoke(
                InsightFacade.Operations.STATS_LATENCY,
                request,
                "Latency histogram loaded",
                "getLatencyHistogram",
                toolContext);
    }

    /**
     * Return the hour-of-week activity grid over a rolling window.
     *
     * @param rangeHours rolling window in hours, 1..8760
     * @param toolContext tool context
     * @return hourly activity result
     */
    public Mono<AgenticToolResult<Object>> getHourlyActivity(Integer rangeHours, ToolContext toolContext) {
        Map<String, Object> request = new LinkedHashMap<>();
        if (Objects.nonNull(rangeHours)) {
            request.put("rangeHours", rangeHours);
        }
        return invoke(
                InsightFacade.Operations.STATS_ACTIVITY,
                request,
                "Hourly activity loaded",
                "getHourlyActivity",
                toolContext);
    }

    /**
     * Return per-protocol fleet health: driver and device counts per protocol service.
     *
     * @param toolContext tool context
     * @return protocol health result
     */
    public Mono<AgenticToolResult<Object>> getProtocolHealth(ToolContext toolContext) {
        return invoke(
                InsightFacade.Operations.PROTOCOL_HEALTH,
                Map.of(),
                "Protocol health loaded",
                "getProtocolHealth",
                toolContext);
    }

    /**
     * Run the query_latest analytics operation: latest values for up to 20 series.
     *
     * @param request analytics request payload (series selectors)
     * @param toolContext tool context
     * @return latest values result
     */
    public Mono<AgenticToolResult<Object>> analyticsQueryLatest(Map<String, Object> request, ToolContext toolContext) {
        return invoke(
                InsightFacade.Operations.QUERY_LATEST,
                request,
                "Analytics latest values loaded",
                "analyticsQueryLatest",
                toolContext);
    }

    /**
     * Run the query_history analytics operation: RAW or M4 downsampled history.
     *
     * @param request analytics request payload (series, window, mode, maxPoints)
     * @param toolContext tool context
     * @return history result
     */
    public Mono<AgenticToolResult<Object>> analyticsQueryHistory(Map<String, Object> request, ToolContext toolContext) {
        return invoke(
                InsightFacade.Operations.QUERY_HISTORY,
                request,
                "Analytics history loaded",
                "analyticsQueryHistory",
                toolContext);
    }

    /**
     * Run the compute_stats analytics operation: mean, std dev, min, max, percentiles.
     *
     * @param request analytics request payload (series, window, percentiles)
     * @param toolContext tool context
     * @return stats result
     */
    public Mono<AgenticToolResult<Object>> analyticsComputeStats(Map<String, Object> request, ToolContext toolContext) {
        return invoke(
                InsightFacade.Operations.COMPUTE_STATS,
                request,
                "Analytics stats loaded",
                "analyticsComputeStats",
                toolContext);
    }

    /**
     * Run the compare_periods analytics operation: current window against baseline.
     *
     * @param request analytics request payload (series, current, baseline)
     * @param toolContext tool context
     * @return comparison result
     */
    public Mono<AgenticToolResult<Object>> analyticsComparePeriods(
            Map<String, Object> request, ToolContext toolContext) {
        return invoke(
                InsightFacade.Operations.COMPARE_PERIODS,
                request,
                "Analytics period comparison loaded",
                "analyticsComparePeriods",
                toolContext);
    }

    /**
     * Run the rank_entities analytics operation: rank devices, points or drivers by metric.
     *
     * @param request analytics request payload (dimension, metric, window, limit)
     * @param toolContext tool context
     * @return ranking result
     */
    public Mono<AgenticToolResult<Object>> analyticsRankEntities(Map<String, Object> request, ToolContext toolContext) {
        return invoke(
                InsightFacade.Operations.RANK_ENTITIES,
                request,
                "Analytics ranking loaded",
                "analyticsRankEntities",
                toolContext);
    }

    /**
     * Run the trend_analysis analytics operation: slope and change per series.
     *
     * @param request analytics request payload (series, window, buckets)
     * @param toolContext tool context
     * @return trend result
     */
    public Mono<AgenticToolResult<Object>> analyticsTrendAnalysis(
            Map<String, Object> request, ToolContext toolContext) {
        return invoke(
                InsightFacade.Operations.TREND_ANALYSIS,
                request,
                "Analytics trend loaded",
                "analyticsTrendAnalysis",
                toolContext);
    }

    /**
     * Run the threshold_report analytics operation: exceedances above or below a threshold.
     *
     * @param request analytics request payload (series, window, operator, threshold)
     * @param toolContext tool context
     * @return threshold report result
     */
    public Mono<AgenticToolResult<Object>> analyticsThresholdReport(
            Map<String, Object> request, ToolContext toolContext) {
        return invoke(
                InsightFacade.Operations.THRESHOLD_REPORT,
                request,
                "Analytics threshold report loaded",
                "analyticsThresholdReport",
                toolContext);
    }

    /**
     * Run the correlate analytics operation: pearson correlation between two series.
     *
     * @param request analytics request payload (seriesA, seriesB, window, alignBucketSeconds)
     * @param toolContext tool context
     * @return correlation result
     */
    public Mono<AgenticToolResult<Object>> analyticsCorrelate(Map<String, Object> request, ToolContext toolContext) {
        return invoke(
                InsightFacade.Operations.CORRELATE,
                request,
                "Analytics correlation loaded",
                "analyticsCorrelate",
                toolContext);
    }

    /**
     * Run the data_quality_report analytics operation: coverage and silent series.
     *
     * @param request analytics request payload (window, silentMinutes)
     * @param toolContext tool context
     * @return quality report result
     */
    public Mono<AgenticToolResult<Object>> analyticsDataQualityReport(
            Map<String, Object> request, ToolContext toolContext) {
        return invoke(
                InsightFacade.Operations.DATA_QUALITY_REPORT,
                request,
                "Analytics quality report loaded",
                "analyticsDataQualityReport",
                toolContext);
    }

    /**
     * Look up the execution status of a point command by its command identifier, closing the
     * readPointValue loop.
     *
     * @param commandId command identifier returned by readPointValue
     * @param toolContext tool context
     * @return point command status result
     */
    public Mono<AgenticToolResult<Object>> getPointCommandStatus(String commandId, ToolContext toolContext) {
        if (Objects.isNull(commandId) || commandId.isBlank()) {
            return Mono.just(AgenticToolResult.invalid("Command ID must not be blank."));
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("commandId", commandId);
        return invoke(
                InsightFacade.Operations.POINT_COMMAND_STATUS,
                request,
                "Point command status loaded",
                "getPointCommandStatus",
                toolContext);
    }

    private Mono<AgenticToolResult<Object>> invoke(
            String operation, Map<String, Object> request, String okMessage, String toolName, ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}", toolName, tenantId);
        InsightFacade facade = insightFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return Mono.just(AgenticToolResult.unavailable(AgenticConstant.ToolMessage.INSIGHT_UNAVAILABLE));
        }
        Map<String, Object> payload = Objects.isNull(request) ? Map.of() : request;
        String requestJson = payload.isEmpty() ? "" : objectMapper.writeValueAsString(payload);
        return facade.invokeReactive(tenantId, operation, requestJson)
                .map(json -> Objects.isNull(json) || json.isBlank()
                        ? AgenticToolResult.<Object>empty("No data returned by " + toolName, null)
                        : AgenticToolResult.ok(okMessage, objectMapper.readValue(json, Object.class)))
                .onErrorResume(error -> {
                    log.warn("Agentic insight tool failed, tool={}, operation={}", toolName, operation, error);
                    return Mono.just(AgenticToolResult.error(
                            AgenticConstant.ToolMessage.REQUEST_FAILED + ": " + error.getMessage()));
                });
    }
}
