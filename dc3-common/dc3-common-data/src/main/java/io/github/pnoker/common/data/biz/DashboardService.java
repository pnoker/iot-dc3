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
 * Dashboard aggregation service — powers the home page's live feed, trend chart, top-N
 * ranking and total counters.
 *
 * <p>
 * All methods read from the history data source (Timescale hypertables); tenant scoping
 * is enforced per call.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.2
 */
public interface DashboardService {

    /**
     * Total point-value rows written today (00:00 local → now) for the tenant.
     */
    long countToday(Long tenantId);

    /**
     * Total point-value rows written yesterday (for the "vs yesterday" delta on the
     * today-total indicator card).
     */
    long countYesterday(Long tenantId);

    /**
     * Total point-value rows across all time for the tenant.
     */
    long countTotal(Long tenantId);

    /**
     * Time-bucketed series of point-value row counts. {@code granularity} is either
     * {@code "hour"} (24-hour default) or {@code "day"} (7/30-day). {@code rangeHours}
     * controls how far back to look.
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
     * Most recent N point-value rows across every typed hypertable — drives the
     * dashboard's live feed panel. {@code size} clamped 1..100.
     */
    List<LatestPointValueVO> latestStream(Long tenantId, int size);

    /**
     * Aggregate counters for the alert indicator card (total, unconfirmed, by
     * event-type), scoped to one tenant.
     */
    AlertStatsVO alertStats(Long tenantId);

    /**
     * Most recent N alert rows across device + driver event tables for the given tenant.
     * Used by the alert list panel on the home page. {@code size} clamped 1..50.
     */
    List<AlertItemVO> alertLatest(Long tenantId, int size);

    /**
     * Acquisition-to-storage latency histogram across all point-value rows in the given
     * lookback window. Returns exactly 6 rows (bins 0..5) even if a bin is empty — the
     * service pads missing buckets with zero counts.
     */
    List<LatencyBucketVO> latencyHistogram(Long tenantId, int rangeHours);

    /**
     * Day-of-week × hour-of-day activity heatmap (7 × 24 = 168 cells). Missing (dow,
     * hour) pairs are padded with zero so the frontend always gets a fully-filled grid.
     */
    List<ActivityCellVO> hourlyActivity(Long tenantId, int rangeHours);

    /**
     * Paged event/alarm list. {@code source} is null (both tables), "device", or "driver"
     * (service whitelists). Returns a MyBatis-Plus
     * {@link com.baomidou.mybatisplus.extension.plugins.pagination.Page Page} so the JSON
     * shape matches every other list endpoint in the project
     * ({@code current / size / total / pages / records}).
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<AlertItemVO> alertPage(Long tenantId, String source,
                                                                                      Integer alarmTypeFlag, Integer confirmFlag, java.time.LocalDateTime from, long current, long size);

    /**
     * Flip confirm_flag = 1 on a single event row. Returns true when the row was actually
     * updated (id + tenant match), false otherwise.
     */
    boolean confirmAlert(Long tenantId, String source, Long id);

    /**
     * Flip confirm_flag back to 0 — undo a previous confirm.
     */
    boolean unconfirmAlert(Long tenantId, String source, Long id);

    /**
     * Bulk confirm / unconfirm. {@code confirm} chooses the direction (true = set to 1,
     * false = set to 0). Returns the number of rows actually changed across all entries.
     */
    int bulkConfirmAlert(Long tenantId,
                         java.util.List<io.github.pnoker.common.data.entity.vo.dashboard.AlertBulkConfirmRequest.Item> items,
                         boolean confirm);

    /**
     * Daily event trend for the last {@code days} days, split by device/driver source.
     */
    List<AlertTrendVO> alertTrend(Long tenantId, int days);

    /**
     * Top N event sources by alarm count in the last {@code days} days.
     */
    List<AlertTopSourceVO> alertTopSources(Long tenantId, int days, int limit);

    /**
     * ALARM counts in a day-of-week × hour-of-day heatmap over the last {@code days}
     * days. Always returns 7 × 24 = 168 cells (zero-padded).
     */
    List<AlertActivityCellVO> alertActivity(Long tenantId, int days);

    /**
     * ALARM count per {@code alarm_ext.type} bucket over the last {@code days} days,
     * ordered by count DESC.
     */
    List<AlertTypeBucketVO> alertTypeDistribution(Long tenantId, int days);

    /**
     * Sources whose ALARM count in the last {@code hours} hours reaches {@code minCount}
     * — "alarm-storm" sources worth immediate attention. Returns at most {@code limit}
     * rows, ordered by count DESC.
     */
    List<AlertTopSourceVO> alertStormSources(Long tenantId, int hours, int minCount, int limit);

    // ===== Phase-2 insights ====================================================

    /**
     * (source, eventType) pairs that fired repeatedly in the last {@code hours} hours.
     * Returns rows with count &gt;= {@code minCount}.
     */
    List<FlappingSourceVO> alertFlapping(Long tenantId, int hours, int minCount, int limit);

    /**
     * Co-occurring event pairs within a time proximity window. Heavy query — callers
     * should expect ~100ms+ on busy tenants.
     */
    List<CorrelationPairVO> alertCorrelation(Long tenantId, int hours, int windowSec, int limit);

    /**
     * Devices whose alarm count is at least 3× their profile-peer median over the
     * lookback window. Service computes medians per profile.
     */
    List<PeerDeviationVO> alertPeerDeviation(Long tenantId, int days);

    /**
     * Current unconfirmed-alarm backlog bucketed by age.
     */
    AgingBacklogVO alertAgingBacklog(Long tenantId);

    /**
     * Per-day p50/p95 MTTA trend for the last {@code days} days.
     */
    List<MttaTrendVO> alertMtta(Long tenantId, int days);

    /**
     * Driver service_name rollup.
     */
    List<ProtocolHealthVO> protocolHealth(Long tenantId);

    /**
     * Recent config edits for the trend-chart overlay.
     */
    List<ChangeImpactVO> changeImpact(Long tenantId, int days, int limit);

    /**
     * Devices/points that have data in the baseline window but have gone silent.
     * {@code baselineDays} = lookback for "was active"; {@code
     * silentMinutes} = silence threshold.
     */
    List<SilentSourceVO> silentSources(Long tenantId, int baselineDays, int silentMinutes, int limit);

    /**
     * Points declared but never reported — config-vs-reality gap.
     */
    CoverageGapVO coverageGap(Long tenantId, int limit);

}
