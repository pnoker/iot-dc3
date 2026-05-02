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

package io.github.pnoker.common.data.biz;

import io.github.pnoker.common.data.entity.vo.dashboard.*;

import java.util.List;

/**
 * Dashboard aggregation service — powers the home page's live feed, trend
 * chart, top-N ranking and total counters.
 *
 * <p>All methods read from the history data source (Timescale hypertables);
 * tenant scoping is enforced per call.</p>
 *
 * @author pnoker
 * @since 2026.5.2
 */
public interface DashboardService {

    /**
     * Total point-value rows written today (00:00 local → now) for the tenant.
     */
    long countToday(Long tenantId);

    /**
     * Total point-value rows written yesterday (for the "vs yesterday" delta
     * on the today-total indicator card).
     */
    long countYesterday(Long tenantId);

    /**
     * Time-bucketed series of point-value row counts. {@code granularity} is
     * either {@code "hour"} (24-hour default) or {@code "day"} (7/30-day).
     * {@code rangeHours} controls how far back to look.
     */
    List<TimeseriesPointVO> timeseries(Long tenantId, String granularity, int rangeHours);

    /**
     * Top-N devices / points / drivers by point-value count.
     *
     * @param dimension  one of {@code device}, {@code point}, {@code driver}
     * @param rangeHours lookback window in hours (e.g. 24, 168, 720)
     * @param limit      max rows returned (clamped 1..50)
     */
    List<TopEntityVO> top(Long tenantId, String dimension, int rangeHours, int limit);

    /**
     * Most recent N point-value rows across every typed hypertable — drives
     * the dashboard's live feed panel. {@code size} clamped 1..100.
     */
    List<LatestPointValueVO> latestStream(Long tenantId, int size);

    /**
     * Aggregate counters for the alert indicator card (total, unconfirmed, by
     * event-type), scoped to one tenant.
     */
    AlertStatsVO alertStats(Long tenantId);

    /**
     * Most recent N alert rows across device + driver event tables for the
     * given tenant. Used by the alert list panel on the home page.
     * {@code size} clamped 1..50.
     */
    List<AlertItemVO> alertLatest(Long tenantId, int size);

    /**
     * Acquisition-to-storage latency histogram across all point-value rows in
     * the given lookback window. Returns exactly 6 rows (bins 0..5) even if a
     * bin is empty — the service pads missing buckets with zero counts.
     */
    List<LatencyBucketVO> latencyHistogram(Long tenantId, int rangeHours);

    /**
     * Day-of-week × hour-of-day activity heatmap (7 × 24 = 168 cells). Missing
     * (dow, hour) pairs are padded with zero so the frontend always gets a
     * fully-filled grid.
     */
    List<ActivityCellVO> hourlyActivity(Long tenantId, int rangeHours);
}
