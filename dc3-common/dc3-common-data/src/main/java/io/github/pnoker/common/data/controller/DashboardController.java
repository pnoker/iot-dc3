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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "dashboard", description = "仪表盘")
@Slf4j
@RestController
@RequestMapping(DataConstant.DASHBOARD_URL_PREFIX)
@RequiredArgsConstructor
public class DashboardController implements BaseController {

    private final DashboardService dashboardService;

    private final io.github.pnoker.common.data.biz.SystemHealthService systemHealthService;

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "仪表盘 - today", description = "仪表盘 - today")
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
    @Operation(summary = "仪表盘 - timeseries", description = "仪表盘 - timeseries")
    @GetMapping("/stats/timeseries")
    public Mono<R<List<TimeseriesPointVO>>> timeseries(
            @Parameter(description = "granularity", required = true)
            @RequestParam(value = "granularity", defaultValue = "hour") String granularity,
            @Parameter(description = "range hours", required = true)
            @RequestParam(value = "range_hours", defaultValue = "24") int rangeHours,
            @Parameter(description = "range key", required = true)
            @RequestParam(value = "range_key", required = false) String rangeKey) {
        int effectiveHours = resolveEffectiveHours(rangeKey, rangeHours);
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.timeseries(tenantId, granularity, effectiveHours))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "查询仪表盘排行", description = "查询仪表盘排行榜")
    @GetMapping("/top")
    public Mono<R<List<TopEntityVO>>> top(@RequestParam(value = "dimension", defaultValue = "device") String dimension,
                                          @Parameter(description = "range hours", required = true)
                                          @RequestParam(value = "range_hours", defaultValue = "24") int rangeHours,
                                          @Parameter(description = "range key", required = true)
                                          @RequestParam(value = "range_key", required = false) String rangeKey,
                                          @Parameter(description = "limit", required = true)
                                          @RequestParam(value = "limit", defaultValue = "10") int limit) {
        int effectiveHours = resolveEffectiveHours(rangeKey, rangeHours);
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.top(tenantId, dimension, effectiveHours, limit))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "查询仪表盘流", description = "查询仪表盘实时流数据")
    @GetMapping("/stream")
    public Mono<R<List<LatestPointValueVO>>> stream(@RequestParam(value = "size", defaultValue = "20") int size) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.latestStream(tenantId, size))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "查询仪表盘统计", description = "查询仪表盘统计数据")
    @GetMapping("/alert/stats")
    public Mono<R<AlertStatsVO>> alertStats() {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.alertStats(tenantId))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "查询仪表盘告警", description = "查询仪表盘告警数据")
    @GetMapping("/alert/latest")
    public Mono<R<List<AlertItemVO>>> alertLatest(@RequestParam(value = "size", defaultValue = "10") int size) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.alertLatest(tenantId, size))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "查询仪表盘延迟", description = "查询仪表盘延迟统计")
    @GetMapping("/stats/latency")
    public Mono<R<List<LatencyBucketVO>>> latencyHistogram(
            @Parameter(description = "range hours", required = true)
            @RequestParam(value = "range_hours", defaultValue = "24") int rangeHours,
            @Parameter(description = "range key", required = true)
            @RequestParam(value = "range_key", required = false) String rangeKey) {
        int effectiveHours = resolveEffectiveHours(rangeKey, rangeHours);
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.latencyHistogram(tenantId, effectiveHours))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "查询仪表盘活跃度", description = "查询仪表盘活跃度统计")
    @GetMapping("/stats/activity")
    public Mono<R<List<ActivityCellVO>>> hourlyActivity(
            @Parameter(description = "range hours", required = true)
            @RequestParam(value = "range_hours", defaultValue = "168") int rangeHours,
            @Parameter(description = "range key", required = true)
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
    @Operation(summary = "查询仪表盘健康状态", description = "查询仪表盘系统健康状态")
    @GetMapping("/system/health")
    public Mono<R<SystemHealthVO>> systemHealth() {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(systemHealthService.snapshot(tenantId))));
    }

    @PreAuthorize("@perm.can('dashboard', 'list')")
    @Operation(summary = "查询仪表盘告警", description = "查询仪表盘告警数据")
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
    @Operation(summary = "查询仪表盘告警", description = "查询仪表盘告警数据")
    @PostMapping("/alert/confirm")
    public Mono<R<Boolean>> alertConfirm(@RequestParam String source,
                                         @RequestParam Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.confirmAlert(tenantId, source, id))));
    }

    @PreAuthorize("@perm.can('dashboard', 'list')")
    @Operation(summary = "查询仪表盘告警", description = "查询仪表盘告警数据")
    @PostMapping("/alert/unconfirm")
    public Mono<R<Boolean>> alertUnconfirm(@RequestParam String source,
                                           @RequestParam Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.unconfirmAlert(tenantId, source, id))));
    }

    /**
     * Bulk confirm or unconfirm. Body = { confirm: true|false, items: [{source, id}, ...]
     * }. Returns the number of rows actually changed.
     */
    @PreAuthorize("@perm.can('dashboard', 'list')")
    @Operation(summary = "查询仪表盘告警", description = "查询仪表盘告警数据")
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
    @Operation(summary = "查询仪表盘告警", description = "查询仪表盘告警数据")
    @GetMapping("/alert/trend")
    public Mono<R<List<AlertTrendVO>>> alertTrend(@RequestParam(value = "days", defaultValue = "30") int days,
                                                  @Parameter(description = "range key", required = true)
                                                  @RequestParam(value = "range_key", required = false) String rangeKey) {
        int effectiveDays = resolveEffectiveDays(rangeKey, days);
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.alertTrend(tenantId, effectiveDays))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "查询仪表盘排行", description = "查询仪表盘排行榜")
    @GetMapping("/alert/top_sources")
    public Mono<R<List<AlertTopSourceVO>>> alertTopSources(@RequestParam(value = "days", defaultValue = "30") int days,
                                                           @Parameter(description = "range key", required = true)
                                                           @RequestParam(value = "range_key", required = false) String rangeKey,
                                                           @Parameter(description = "limit", required = true)
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
    @Operation(summary = "查询仪表盘告警", description = "查询仪表盘告警数据")
    @GetMapping("/alert/activity")
    public Mono<R<List<AlertActivityCellVO>>> alertActivity(@RequestParam(value = "days", defaultValue = "7") int days,
                                                            @Parameter(description = "range key", required = true)
                                                            @RequestParam(value = "range_key", required = false) String rangeKey) {
        int effectiveDays = resolveEffectiveDays(rangeKey, days);
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.alertActivity(tenantId, effectiveDays))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "查询仪表盘告警", description = "查询仪表盘告警数据")
    @GetMapping("/alert/type_distribution")
    public Mono<R<List<AlertTypeBucketVO>>> alertTypeDistribution(
            @Parameter(description = "days", required = true)
            @RequestParam(value = "days", defaultValue = "30") int days,
            @Parameter(description = "range key", required = true)
            @RequestParam(value = "range_key", required = false) String rangeKey) {
        int effectiveDays = resolveEffectiveDays(rangeKey, days);
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.alertTypeDistribution(tenantId, effectiveDays))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "查询仪表盘告警", description = "查询仪表盘告警数据")
    @GetMapping("/alert/storm_sources")
    public Mono<R<List<AlertTopSourceVO>>> alertStormSources(
            @Parameter(description = "hours", required = true)
            @RequestParam(value = "hours", defaultValue = "1") int hours,
            @Parameter(description = "min count", required = true)
            @RequestParam(value = "min_count", defaultValue = "10") int minCount,
            @Parameter(description = "limit", required = true)
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.alertStormSources(tenantId, hours, minCount, limit))));
    }

    // ===== Phase-2 insights =====================================================

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "查询仪表盘告警", description = "查询仪表盘告警数据")
    @GetMapping("/alert/flapping")
    public Mono<R<List<FlappingSourceVO>>> alertFlapping(@RequestParam(value = "hours", defaultValue = "6") int hours,
                                                         @Parameter(description = "min count", required = true)
                                                         @RequestParam(value = "min_count", defaultValue = "5") int minCount,
                                                         @Parameter(description = "limit", required = true)
                                                         @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.alertFlapping(tenantId, hours, minCount, limit))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "查询仪表盘告警", description = "查询仪表盘告警数据")
    @GetMapping("/alert/correlation")
    public Mono<R<List<CorrelationPairVO>>> alertCorrelation(
            @Parameter(description = "hours", required = true)
            @RequestParam(value = "hours", defaultValue = "24") int hours,
            @Parameter(description = "window sec", required = true)
            @RequestParam(value = "window_sec", defaultValue = "30") int windowSec,
            @Parameter(description = "limit", required = true)
            @RequestParam(value = "limit", defaultValue = "15") int limit) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.alertCorrelation(tenantId, hours, windowSec, limit))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "查询仪表盘告警", description = "查询仪表盘告警数据")
    @GetMapping("/alert/peer_deviation")
    public Mono<R<List<PeerDeviationVO>>> alertPeerDeviation(
            @Parameter(description = "days", required = true)
            @RequestParam(value = "days", defaultValue = "7") int days) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.alertPeerDeviation(tenantId, days))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "查询仪表盘告警", description = "查询仪表盘告警数据")
    @GetMapping("/alert/aging")
    public Mono<R<AgingBacklogVO>> alertAging() {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.alertAgingBacklog(tenantId))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "查询仪表盘告警", description = "查询仪表盘告警数据")
    @GetMapping("/alert/mtta")
    public Mono<R<List<MttaTrendVO>>> alertMtta(@RequestParam(value = "days", defaultValue = "30") int days) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.alertMtta(tenantId, days))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "查询仪表盘健康状态", description = "查询仪表盘系统健康状态")
    @GetMapping("/protocol/health")
    public Mono<R<List<ProtocolHealthVO>>> protocolHealth() {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.protocolHealth(tenantId))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "仪表盘 - change impact", description = "仪表盘 - change impact")
    @GetMapping("/alert/change_impact")
    public Mono<R<List<ChangeImpactVO>>> changeImpact(@RequestParam(value = "days", defaultValue = "30") int days,
                                                      @Parameter(description = "limit", required = true)
                                                      @RequestParam(value = "limit", defaultValue = "30") int limit) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.changeImpact(tenantId, days, limit))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "仪表盘 - silent sources", description = "仪表盘 - silent sources")
    @GetMapping("/silent/sources")
    public Mono<R<List<SilentSourceVO>>> silentSources(
            @Parameter(description = "baseline days", required = true)
            @RequestParam(value = "baseline_days", defaultValue = "7") int baselineDays,
            @Parameter(description = "silent minutes", required = true)
            @RequestParam(value = "silent_minutes", defaultValue = "15") int silentMinutes,
            @Parameter(description = "limit", required = true)
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.silentSources(tenantId, baselineDays, silentMinutes, limit))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "仪表盘 - coverage gap", description = "仪表盘 - coverage gap")
    @GetMapping("/coverage/gap")
    public Mono<R<CoverageGapVO>> coverageGap(@RequestParam(value = "limit", defaultValue = "100") int limit) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.coverageGap(tenantId, limit))));
    }

}
