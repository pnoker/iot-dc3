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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.biz.DashboardService;
import io.github.pnoker.common.data.entity.query.AlertPageQuery;
import io.github.pnoker.common.data.entity.vo.dashboard.*;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.utils.TimeRangeUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Dashboard / home-page aggregate endpoints. All tenant-scoped via
 * {@link BaseController#getTenantId()}.
 *
 * <p>
 * Route summary (all GET, all under {@code /api/v3/data/dashboard}):
 * </p>
 * <ul>
 * <li>{@code /stats/today} — today total + yesterday total for delta</li>
 * <li>{@code /stats/timeseries?granularity=hour|day&rangeHours=24}</li>
 * <li>{@code /top?dimension=device|point|driver&rangeHours=24&limit=10}</li>
 * <li>{@code /stream?size=20} — most recent rows (user-triggered refresh)</li>
 * <li>{@code /alert/stats} — total + unconfirmed + by-type breakdown</li>
 * <li>{@code /alert/latest?size=10} — most recent alerts</li>
 * </ul>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.2
 */
@Tag(name = "dashboard", description = "Data monitoring dashboard configuration: manage data-side dashboard layouts, widgets, and visualization preferences for device data monitoring")
@Slf4j
@RestController
@RequestMapping(DataConstant.DASHBOARD_URL_PREFIX)
@RequiredArgsConstructor
public class DashboardController implements BaseController {

    private final DashboardService dashboardService;

    private final io.github.pnoker.common.data.biz.SystemHealthService systemHealthService;

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Today Statistics", description = "Return today's, yesterday's and cumulative counts (devices, points, alerts) with day-over-day change ratios for the current tenant. Powers the dashboard headline tiles.")
    @GetMapping("/stats/today")
    public Mono<R<TodayStatsVO>> today() {
        return getTenantId().flatMap(tenantId -> async(() -> {
            long today = dashboardService.countToday(tenantId);
            long yesterday = dashboardService.countYesterday(tenantId);
            long total = dashboardService.countTotal(tenantId);
            // Convenience delta for the UI: +12% (positive) / -3% (negative) / 0.
            long percentChange = yesterday > 0
                    ? Math.round(((double) (today - yesterday) * 100.0) / yesterday)
                    : today > 0 ? 100 : 0;
            return R.ok(new TodayStatsVO(today, yesterday, total, percentChange));
        }));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Dashboard Time Series", description = "Bucket today's, yesterday's and cumulative device/point/alert counts into hour or day buckets over a rolling window (range_key or range_hours) for the current tenant. Use to render the dashboard trend chart.")
    @GetMapping("/stats/timeseries")
    public Mono<R<List<TimeseriesPointVO>>> timeseries(
            @Parameter(description = "Time bucket granularity, for example hour or day")
            @RequestParam(value = "granularity", defaultValue = "hour") String granularity,
            @Parameter(description = "Fallback rolling time range in hours")
            @RequestParam(value = "range_hours", defaultValue = "24") int rangeHours,
            @Parameter(description = "Preset time range key: today, 24h, 7d, or 30d")
            @RequestParam(value = "range_key", required = false) String rangeKey) {
        int effectiveHours = resolveEffectiveHours(rangeKey, rangeHours);
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.timeseries(tenantId, granularity, effectiveHours))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Dashboard Ranking", description = "Rank the tenant's devices, points or drivers by activity over a rolling window (range_key or range_hours), returning the top N entries for the chosen dimension. Use to surface the busiest entities.")
    @GetMapping("/top")
    public Mono<R<List<TopEntityVO>>> top(@Parameter(description = "Ranking dimension") @RequestParam(value = "dimension", defaultValue = "device") String dimension,
                                          @Parameter(description = "Fallback rolling time range in hours")
                                          @RequestParam(value = "range_hours", defaultValue = "24") int rangeHours,
                                          @Parameter(description = "Preset time range key: today, 24h, 7d, or 30d")
                                          @RequestParam(value = "range_key", required = false) String rangeKey,
                                          @Parameter(description = "Maximum number of items to return")
                                          @RequestParam(value = "limit", defaultValue = "10") int limit) {
        int effectiveHours = resolveEffectiveHours(rangeKey, rangeHours);
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.top(tenantId, dimension, effectiveHours, limit))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Dashboard Live Stream", description = "Return the N most recent point-value readings across the tenant's points, newest first. Use to populate the live feed panel on a user-triggered refresh.")
    @GetMapping("/stream")
    public Mono<R<List<LatestPointValueVO>>> stream(@Parameter(description = "Maximum number of items to return") @RequestParam(value = "size", defaultValue = "20") int size) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.latestStream(tenantId, size))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Alert Statistics", description = "Return the tenant's current alert totals: total count, unconfirmed count and a breakdown by alarm type. Use to populate the alert summary cards.")
    @GetMapping("/alert/stats")
    public Mono<R<AlertStatsVO>> alertStats() {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.alertStats(tenantId))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "List Latest Alerts", description = "Return the N most recent alerts for the current tenant, newest first. Use to populate the recent-alerts list; each item carries source, alarm type, message and confirm state.")
    @GetMapping("/alert/latest")
    public Mono<R<List<AlertItemVO>>> alertLatest(@Parameter(description = "Maximum number of items to return") @RequestParam(value = "size", defaultValue = "10") int size) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.alertLatest(tenantId, size))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Latency Histogram", description = "Bucket the tenant's point-value collection latencies over a rolling window (range_key or range_hours) into histogram bands. Use to assess how fresh the ingested readings are.")
    @GetMapping("/stats/latency")
    public Mono<R<List<LatencyBucketVO>>> latencyHistogram(
            @Parameter(description = "Fallback rolling time range in hours")
            @RequestParam(value = "range_hours", defaultValue = "24") int rangeHours,
            @Parameter(description = "Preset time range key: today, 24h, 7d, or 30d")
            @RequestParam(value = "range_key", required = false) String rangeKey) {
        int effectiveHours = resolveEffectiveHours(rangeKey, rangeHours);
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.latencyHistogram(tenantId, effectiveHours))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Hourly Activity", description = "Aggregate the tenant's data-collection activity into hour-of-day cells over a rolling window (default 168 hours / one week). Use to render the activity heatmap and spot quiet or peak hours.")
    @GetMapping("/stats/activity")
    public Mono<R<List<ActivityCellVO>>> hourlyActivity(
            @Parameter(description = "Fallback rolling time range in hours")
            @RequestParam(value = "range_hours", defaultValue = "168") int rangeHours,
            @Parameter(description = "Preset time range key: today, 24h, 7d, or 30d")
            @RequestParam(value = "range_key", required = false) String rangeKey) {
        int effectiveHours = resolveEffectiveHours(rangeKey, rangeHours);
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.hourlyActivity(tenantId, effectiveHours))));
    }

    /**
     * System-wide health snapshot for the home banner. Infra / center probes are
     * platform-wide but driver / device fleet summaries are tenant-scoped, so we thread
     * tenantId through to match the gRPC facades' tenant filter.
     */
    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get System Health", description = "Return a platform-wide health snapshot: center and infra probe status plus tenant-scoped driver and device fleet summaries. Use to populate the home banner health widget.")
    @GetMapping("/system/health")
    public Mono<R<SystemHealthVO>> systemHealth() {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(systemHealthService.snapshot(tenantId))));
    }

    @PreAuthorize("@perm.can('dashboard', 'list')")
    @Operation(summary = "List Alerts", description = "Page through the tenant's alert records with filters for source (driver/device/point), alarm type, confirm flag and a time window. Use to browse or triage the alert history table.")
    @PostMapping("/alert/page")
    public Mono<R<Page<AlertItemVO>>> alertPage(
            @RequestBody(required = false) AlertPageQuery query) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            AlertPageQuery q = Objects.isNull(query)
                    ? new AlertPageQuery() : query;
            LocalDateTime from = TimeRangeUtil.resolveFrom(q.getRangeKey(), q.getRangeHours());
            long current = Objects.isNull(q.getCurrent()) ? 1L : q.getCurrent();
            long size = Objects.isNull(q.getSize()) ? 20L : q.getSize();
            return R.ok(dashboardService.alertPage(tenantId, q.getSource(), q.getAlarmTypeFlag(),
                    q.getConfirmFlag(), from, current, size));
        }));
    }

    @PreAuthorize("@perm.can('dashboard', 'list')")
    @Operation(summary = "Confirm Alert", description = "Mark a single alert (by source and record id) as acknowledged for the current tenant. Returns true when the row was actually updated.")
    @PostMapping("/alert/confirm")
    public Mono<R<Boolean>> alertConfirm(@Parameter(description = "Alert source") @RequestParam String source,
                                         @Parameter(description = "Record ID") @RequestParam Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.confirmAlert(tenantId, source, id))));
    }

    @PreAuthorize("@perm.can('dashboard', 'list')")
    @Operation(summary = "Unconfirm Alert", description = "Remove the acknowledgement flag from a single alert (by source and record id) for the current tenant. Returns true when the row was actually updated.")
    @PostMapping("/alert/unconfirm")
    public Mono<R<Boolean>> alertUnconfirm(@Parameter(description = "Alert source") @RequestParam String source,
                                           @Parameter(description = "Record ID") @RequestParam Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.unconfirmAlert(tenantId, source, id))));
    }

    /**
     * Bulk confirm or unconfirm. Body = { confirm: true|false, items: [{source, id}, ...]
     * }. Returns the number of rows actually changed.
     */
    @PreAuthorize("@perm.can('dashboard', 'list')")
    @Operation(summary = "Bulk Confirm Alerts", description = "Apply confirm or unconfirm to a list of alerts (each {source, id}) for the current tenant in one call. Returns the count of rows actually changed.")
    @PostMapping("/alert/bulk_confirm")
    public Mono<R<Integer>> alertBulkConfirm(
            @RequestBody AlertBulkConfirmRequest body) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            List<AlertBulkConfirmRequest.Item> items = Objects.isNull(body) || Objects.isNull(body.getItems())
                    ? Collections.emptyList()
                    : body.getItems();
            boolean confirm = Objects.isNull(body) || Objects.isNull(body.getConfirm()) || body.getConfirm();
            return R.ok(dashboardService.bulkConfirmAlert(tenantId, items, confirm));
        }));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Alert Trend", description = "Return daily alert counts over a rolling day range for the current tenant. " +
            "Use to visualize whether alert volume is rising or falling; each item is one day's count.")
    @GetMapping("/alert/trend")
    public Mono<R<List<AlertTrendVO>>> alertTrend(@Parameter(description = "Rolling day range") @RequestParam(value = "days", defaultValue = "30") int days,
                                                  @Parameter(description = "Preset time range key: today, 24h, 7d, or 30d")
                                                  @RequestParam(value = "range_key", required = false) String rangeKey) {
        int effectiveDays = resolveEffectiveDays(rangeKey, days);
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.alertTrend(tenantId, effectiveDays))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Top Alert Sources", description = "Rank the tenant's alert sources (driver/device/point) by alert volume over a rolling day range, returning the top N. Use to find which entities generate the most alerts.")
    @GetMapping("/alert/top_sources")
    public Mono<R<List<AlertTopSourceVO>>> alertTopSources(@Parameter(description = "Rolling day range") @RequestParam(value = "days", defaultValue = "30") int days,
                                                           @Parameter(description = "Preset time range key: today, 24h, 7d, or 30d")
                                                           @RequestParam(value = "range_key", required = false) String rangeKey,
                                                           @Parameter(description = "Maximum number of items to return")
                                                           @RequestParam(value = "limit", defaultValue = "10") int limit) {
        int effectiveDays = resolveEffectiveDays(rangeKey, days);
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.alertTopSources(tenantId, effectiveDays, limit))));
    }

    /**
     * Resolve the UI's {@code rangeKey} (preferred) against the legacy {@code rangeHours}
     * integer. {@code TODAY} is mapped to the hours elapsed since local midnight so
     * bucketed queries cover the full day so far; other keys use the canonical 24 / 168 /
     * 720 hour spans.
     */
    private int resolveEffectiveHours(String rangeKey, int rangeHours) {
        Integer resolved = TimeRangeUtil.resolveHours(rangeKey, rangeHours);
        return Objects.nonNull(resolved) ? resolved : rangeHours;
    }

    /**
     * Resolve the UI's {@code rangeKey} against the legacy {@code days} integer for
     * day-bucketed endpoints. {@code TODAY} / {@code H24} both collapse to 1 day.
     */
    private int resolveEffectiveDays(String rangeKey, int days) {
        Integer resolved = TimeRangeUtil.resolveDays(rangeKey, days);
        return Objects.nonNull(resolved) ? resolved : days;
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Alert Activity", description = "Aggregate the tenant's alerts into hour-of-day-by-day cells over a rolling day range. Use to render the alert heatmap and surface peak alerting hours.")
    @GetMapping("/alert/activity")
    public Mono<R<List<AlertActivityCellVO>>> alertActivity(@Parameter(description = "Rolling day range") @RequestParam(value = "days", defaultValue = "7") int days,
                                                            @Parameter(description = "Preset time range key: today, 24h, 7d, or 30d")
                                                            @RequestParam(value = "range_key", required = false) String rangeKey) {
        int effectiveDays = resolveEffectiveDays(rangeKey, days);
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.alertActivity(tenantId, effectiveDays))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Alert Type Distribution", description = "Bucket the tenant's alerts by alarm type over a rolling day range. Use to see which alarm categories dominate within the window.")
    @GetMapping("/alert/type_distribution")
    public Mono<R<List<AlertTypeBucketVO>>> alertTypeDistribution(
            @Parameter(description = "Rolling day range")
            @RequestParam(value = "days", defaultValue = "30") int days,
            @Parameter(description = "Preset time range key: today, 24h, 7d, or 30d")
            @RequestParam(value = "range_key", required = false) String rangeKey) {
        int effectiveDays = resolveEffectiveDays(rangeKey, days);
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.alertTypeDistribution(tenantId, effectiveDays))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Alert Storm Sources", description = "Detect the tenant's alert storms by flagging sources whose alert count inside a short rolling hour window exceeds a threshold. Use to spot bursty or flooding alert producers.")
    @GetMapping("/alert/storm_sources")
    public Mono<R<List<AlertTopSourceVO>>> alertStormSources(
            @Parameter(description = "Rolling hour range")
            @RequestParam(value = "hours", defaultValue = "1") int hours,
            @Parameter(description = "Minimum alert count threshold")
            @RequestParam(value = "min_count", defaultValue = "10") int minCount,
            @Parameter(description = "Maximum number of items to return")
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.alertStormSources(tenantId, hours, minCount, limit))));
    }

    // ===== Phase-2 insights =====================================================

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Flapping Alert Sources", description = "Flag the tenant's alert sources that toggle confirm and recovery state repeatedly inside a rolling hour window above a count threshold. Use to find noisy, oscillating alerts that may need suppression.")
    @GetMapping("/alert/flapping")
    public Mono<R<List<FlappingSourceVO>>> alertFlapping(@Parameter(description = "Rolling hour range") @RequestParam(value = "hours", defaultValue = "6") int hours,
                                                         @Parameter(description = "Minimum alert count threshold")
                                                         @RequestParam(value = "min_count", defaultValue = "5") int minCount,
                                                         @Parameter(description = "Maximum number of items to return")
                                                         @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.alertFlapping(tenantId, hours, minCount, limit))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Alert Correlation", description = "Return alert source pairs that fire within a tight time window of each other over a rolling hour range, scored by co-occurrence. Use to discover alerts that share an upstream root cause.")
    @GetMapping("/alert/correlation")
    public Mono<R<List<CorrelationPairVO>>> alertCorrelation(
            @Parameter(description = "Rolling hour range")
            @RequestParam(value = "hours", defaultValue = "24") int hours,
            @Parameter(description = "Correlation window in seconds")
            @RequestParam(value = "window_sec", defaultValue = "30") int windowSec,
            @Parameter(description = "Maximum number of items to return")
            @RequestParam(value = "limit", defaultValue = "15") int limit) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.alertCorrelation(tenantId, hours, windowSec, limit))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Peer Deviation Alerts", description = "Compare each peer entity's alert rate against the cohort baseline over a rolling day range and flag statistically significant deviations. Use to catch entities that alert far more than their peers.")
    @GetMapping("/alert/peer_deviation")
    public Mono<R<List<PeerDeviationVO>>> alertPeerDeviation(
            @Parameter(description = "Rolling day range")
            @RequestParam(value = "days", defaultValue = "7") int days) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.alertPeerDeviation(tenantId, days))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Alert Aging Backlog", description = "Bucket the tenant's currently unconfirmed alerts by elapsed age for the current tenant. Use to surface stale alerts that are still awaiting acknowledgement.")
    @GetMapping("/alert/aging")
    public Mono<R<AgingBacklogVO>> alertAging() {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.alertAgingBacklog(tenantId))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get MTTA Trend", description = "Return the mean time to acknowledge alerts per day over a rolling day range for the current tenant. Use to track whether the team is getting faster at acknowledging alerts.")
    @GetMapping("/alert/mtta")
    public Mono<R<List<MttaTrendVO>>> alertMtta(@Parameter(description = "Rolling day range") @RequestParam(value = "days", defaultValue = "30") int days) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.alertMtta(tenantId, days))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Protocol Health", description = "Return per-protocol health and connectivity status for the tenant's driver fleet. Use to see which protocols are online, degraded, or offline.")
    @GetMapping("/protocol/health")
    public Mono<R<List<ProtocolHealthVO>>> protocolHealth() {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.protocolHealth(tenantId))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Change Impact", description = "Correlate the tenant's configuration changes with subsequent alert-volume shifts over a rolling day range, returning the top-impacted items. Use to confirm whether a config edit improved or hurt alert noise.")
    @GetMapping("/alert/change_impact")
    public Mono<R<List<ChangeImpactVO>>> changeImpact(@Parameter(description = "Rolling day range") @RequestParam(value = "days", defaultValue = "30") int days,
                                                      @Parameter(description = "Maximum number of items to return")
                                                      @RequestParam(value = "limit", defaultValue = "30") int limit) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.changeImpact(tenantId, days, limit))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Silent Data Sources", description = "Detect the tenant's points or devices that have produced no new data beyond a silence threshold, compared against a baseline day range. Use to catch sensors or devices that have stopped reporting.")
    @GetMapping("/silent/sources")
    public Mono<R<List<SilentSourceVO>>> silentSources(
            @Parameter(description = "Baseline rolling day range")
            @RequestParam(value = "baseline_days", defaultValue = "7") int baselineDays,
            @Parameter(description = "Silence threshold in minutes")
            @RequestParam(value = "silent_minutes", defaultValue = "15") int silentMinutes,
            @Parameter(description = "Maximum number of items to return")
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.silentSources(tenantId, baselineDays, silentMinutes, limit))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Coverage Gap", description = "Summarize the tenant's data-coverage gaps, listing points or devices missing expected recent readings. Use to find blind spots in collection coverage.")
    @GetMapping("/coverage/gap")
    public Mono<R<CoverageGapVO>> coverageGap(@Parameter(description = "Maximum number of items to return") @RequestParam(value = "limit", defaultValue = "100") int limit) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.coverageGap(tenantId, limit))));
    }

}
