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
package io.github.pnoker.common.data.biz.analytics;

import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel.*;
import reactor.core.publisher.Mono;

/**
 * AI analytics query facade (docs/design/tsdb-abstraction.md §9.7, S19): nine
 * coarse-grained, conclusion-shaped reads combining TSDB port primitives with
 * relational metadata. The tenant id is an explicit parameter on every method —
 * MCP callers get it injected from their authenticated principal and can never
 * cross tenants. Scan volume (series count, samples pulled, window span) is
 * bounded; over-budget requests return a structured degradation hint instead of
 * silently truncating.
 *
 * @author pnoker
 * @since 2026.8.21
 */
public interface DataAnalyticsService {

    /**
     * Latest values for the requested points.
     *
     * @param tenantId tenant scope; results never cross tenants
     * @param request  point selection plus freshness/degradation budget
     */
    Mono<LatestValuesResponse> queryLatest(Long tenantId, QueryLatestRequest request);

    /**
     * Time-windowed history for the requested points.
     *
     * @param tenantId tenant scope; results never cross tenants
     * @param request  window, sampling, and budget constraints
     */
    Mono<HistoryResponse> queryHistory(Long tenantId, QueryHistoryRequest request);

    /**
     * Aggregate statistics (min/max/avg/count and friends) over a window.
     *
     * @param tenantId tenant scope; results never cross tenants
     * @param request  aggregation target and window
     */
    Mono<StatsResponse> computeStats(Long tenantId, ComputeStatsRequest request);

    /**
     * Compare two windows of the same point set.
     *
     * @param tenantId tenant scope; results never cross tenants
     * @param request  the baseline and comparison windows
     */
    Mono<CompareResponse> comparePeriods(Long tenantId, ComparePeriodsRequest request);

    /**
     * Rank entities (points/devices) by the requested metric.
     *
     * @param tenantId tenant scope; results never cross tenants
     * @param request  metric, window, and result limit
     */
    Mono<RankResponse> rankEntities(Long tenantId, RankEntitiesRequest request);

    /**
     * Trend detection over a window (slope, seasonality, outliers).
     *
     * @param tenantId tenant scope; results never cross tenants
     * @param request  analysis window and sensitivity
     */
    Mono<TrendResponse> trendAnalysis(Long tenantId, TrendAnalysisRequest request);

    /**
     * Threshold-breach report for the requested points.
     *
     * @param tenantId tenant scope; results never cross tenants
     * @param request  thresholds and observation window
     */
    Mono<ThresholdResponse> thresholdReport(Long tenantId, ThresholdReportRequest request);

    /**
     * Correlation between two point series over a window.
     *
     * @param tenantId tenant scope; results never cross tenants
     * @param request  the two series and the window
     */
    Mono<CorrelationResponse> correlate(Long tenantId, CorrelateRequest request);

    /**
     * Data-quality report (gaps, staleness, duplicates) for the requested points.
     *
     * @param tenantId tenant scope; results never cross tenants
     * @param request  quality dimensions and window
     */
    Mono<QualityResponse> qualityReport(Long tenantId, QualityReportRequest request);
}
