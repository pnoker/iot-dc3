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

import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel.ComparePeriodsRequest;
import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel.CompareResponse;
import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel.ComputeStatsRequest;
import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel.CorrelateRequest;
import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel.CorrelationResponse;
import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel.HistoryResponse;
import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel.LatestValuesResponse;
import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel.QueryHistoryRequest;
import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel.QueryLatestRequest;
import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel.QualityReportRequest;
import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel.QualityResponse;
import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel.RankEntitiesRequest;
import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel.RankResponse;
import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel.StatsResponse;
import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel.ThresholdReportRequest;
import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel.ThresholdResponse;
import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel.TrendAnalysisRequest;
import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel.TrendResponse;

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

    LatestValuesResponse queryLatest(Long tenantId, QueryLatestRequest request);

    HistoryResponse queryHistory(Long tenantId, QueryHistoryRequest request);

    StatsResponse computeStats(Long tenantId, ComputeStatsRequest request);

    CompareResponse comparePeriods(Long tenantId, ComparePeriodsRequest request);

    RankResponse rankEntities(Long tenantId, RankEntitiesRequest request);

    TrendResponse trendAnalysis(Long tenantId, TrendAnalysisRequest request);

    ThresholdResponse thresholdReport(Long tenantId, ThresholdReportRequest request);

    CorrelationResponse correlate(Long tenantId, CorrelateRequest request);

    QualityResponse qualityReport(Long tenantId, QualityReportRequest request);
}
