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

package io.github.pnoker.common.data.controller;

import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.data.biz.analytics.DataAnalyticsService;
import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * AI analytics endpoints (docs/design/tsdb-abstraction.md §9.7). Nine
 * coarse-grained, conclusion-shaped reads for MCP agents: the tool catalog
 * synthesizes these paths from the OpenAPI snapshot, and the gateway forwards
 * tools/call here over HTTP. The tenant id always comes from the authenticated
 * principal — never from the request body — so an agent cannot cross tenants.
 * All operations are read-only, idempotent and bounded by the facade's scan
 * budget.
 *
 * @author pnoker
 * @since 2026.8.21
 */
@Tag(name = "analytics", description = "AI analytics: coarse-grained statistical reads over device point time series, shaped as self-contained conclusions for agents")
@Slf4j
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController implements BaseController {

    private final DataAnalyticsService dataAnalyticsService;

    /**
     * Current values of one or more series, addressed by ids or unambiguous names.
     */
    @PreAuthorize("@perm.can('analytics', 'list')")
    @Operation(summary = "Query Latest Values", description = "Return the current value of one or more device points for the current tenant, "
            + "addressed by ids or unambiguous names. Ambiguous names fail with the candidate list.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/query_latest")
    public Mono<AnalyticsModel.LatestValuesResponse> queryLatest(@RequestBody AnalyticsModel.QueryLatestRequest request) {
        return getTenantId().flatMap(tenantId -> dataAnalyticsService.queryLatest(tenantId, request));
    }

    /**
     * Windowed history per series, either raw samples or M4 down-sampling.
     */
    @PreAuthorize("@perm.can('analytics', 'list')")
    @Operation(summary = "Query History", description = "Windowed per-series history: RAW mode returns the newest samples (bounded); "
            + "M4 mode returns per-bucket first/min/max/last for chart-grade rendering of long windows.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/query_history")
    public Mono<AnalyticsModel.HistoryResponse> queryHistory(@RequestBody AnalyticsModel.QueryHistoryRequest request) {
        return getTenantId().flatMap(tenantId -> dataAnalyticsService.queryHistory(tenantId, request));
    }

    /**
     * Statistical profile per series: mean, std-dev, extremes, count, percentiles.
     */
    @PreAuthorize("@perm.can('analytics', 'list')")
    @Operation(summary = "Compute Stats", description = "Statistical profile of each series inside a window: mean, standard deviation, "
            + "extremes, sample count and configurable percentiles, computed from a bounded newest-sample pull.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/compute_stats")
    public Mono<AnalyticsModel.StatsResponse> computeStats(@RequestBody AnalyticsModel.ComputeStatsRequest request) {
        return getTenantId().flatMap(tenantId -> dataAnalyticsService.computeStats(tenantId, request));
    }

    /**
     * Same series across two windows: delta and percentage change.
     */
    @PreAuthorize("@perm.can('analytics', 'list')")
    @Operation(summary = "Compare Periods", description = "Compare each series between two windows (e.g. this week vs last week): "
            + "per-series averages with delta and percentage change.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/compare_periods")
    public Mono<AnalyticsModel.CompareResponse> comparePeriods(@RequestBody AnalyticsModel.ComparePeriodsRequest request) {
        return getTenantId().flatMap(tenantId -> dataAnalyticsService.comparePeriods(tenantId, request));
    }

    /**
     * Top entities by activity or by an aggregate metric.
     */
    @PreAuthorize("@perm.can('analytics', 'list')")
    @Operation(summary = "Rank Entities", description = "Rank devices, points or drivers inside a window by ACTIVITY (sample count) "
            + "or by MEAN / MAX / MIN over their series.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/rank_entities")
    public Mono<AnalyticsModel.RankResponse> rankEntities(@RequestBody AnalyticsModel.RankEntitiesRequest request) {
        return getTenantId().flatMap(tenantId -> dataAnalyticsService.rankEntities(tenantId, request));
    }

    /**
     * Per-series trend: least-squares slope over bucket averages.
     */
    @PreAuthorize("@perm.can('analytics', 'list')")
    @Operation(summary = "Trend Analysis", description = "Per-series trend over a window: linear-regression slope across bucket averages "
            + "plus first/last values and total percentage change.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/trend_analysis")
    public Mono<AnalyticsModel.TrendResponse> trendAnalysis(@RequestBody AnalyticsModel.TrendAnalysisRequest request) {
        return getTenantId().flatMap(tenantId -> dataAnalyticsService.trendAnalysis(tenantId, request));
    }

    /**
     * Threshold exceedance report: when, how long, how extreme.
     */
    @PreAuthorize("@perm.can('analytics', 'list')")
    @Operation(summary = "Threshold Report", description = "Report when each series exceeded (or fell below) a threshold inside a window: "
            + "merged exceedance intervals, total duration, sample count and the peak value.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/threshold_report")
    public Mono<AnalyticsModel.ThresholdResponse> thresholdReport(@RequestBody AnalyticsModel.ThresholdReportRequest request) {
        return getTenantId().flatMap(tenantId -> dataAnalyticsService.thresholdReport(tenantId, request));
    }

    /**
     * Pearson correlation between two series over aligned buckets.
     */
    @PreAuthorize("@perm.can('analytics', 'list')")
    @Operation(summary = "Correlate Two Series", description = "Pearson correlation between two series over aligned time buckets; "
            + "served by the store when it supports SQL-side correlation, otherwise computed from bucketed pulls.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/correlate")
    public Mono<AnalyticsModel.CorrelationResponse> correlate(@RequestBody AnalyticsModel.CorrelateRequest request) {
        return getTenantId().flatMap(tenantId -> dataAnalyticsService.correlate(tenantId, request));
    }

    /**
     * Tenant-level data quality: coverage, silent sources, quality-code census.
     */
    @PreAuthorize("@perm.can('analytics', 'list')")
    @Operation(summary = "Data Quality Report", description = "Tenant-level data quality overview for a window: point coverage, series "
            + "silent beyond a threshold, and a sampled distribution of quality codes.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/data_quality_report")
    public Mono<AnalyticsModel.QualityResponse> qualityReport(@RequestBody AnalyticsModel.QualityReportRequest request) {
        return getTenantId().flatMap(tenantId -> dataAnalyticsService.qualityReport(tenantId, request));
    }

}
