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
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.biz.DashboardService;
import io.github.pnoker.common.data.entity.vo.dashboard.*;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.utils.TimeRangeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
@Slf4j
@RestController
@RequestMapping(DataConstant.DASHBOARD_URL_PREFIX)
public class DashboardController implements BaseController {

    private final DashboardService dashboardService;

    private final io.github.pnoker.common.data.biz.SystemHealthService systemHealthService;

    public DashboardController(DashboardService dashboardService,
                               io.github.pnoker.common.data.biz.SystemHealthService systemHealthService) {
        this.dashboardService = dashboardService;
        this.systemHealthService = systemHealthService;
    }

    @GetMapping("/stats/today")
    public Mono<R<Map<String, Object>>> today() {
        return getTenantId().flatMap(tenantId -> {
            try {
                long today = dashboardService.countToday(tenantId);
                long yesterday = dashboardService.countYesterday(tenantId);
                long total = dashboardService.countTotal(tenantId);
                Map<String, Object> out = new HashMap<>();
                out.put("today", today);
                out.put("yesterday", yesterday);
                out.put("total", total);
                // Convenience delta for the UI: +12% (positive) / -3% (negative) / 0.
                if (yesterday > 0) {
                    out.put("percentChange", Math.round(((double) (today - yesterday) * 100.0) / yesterday));
                } else {
                    out.put("percentChange", today > 0 ? 100 : 0);
                }
                return Mono.just(R.ok(out));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/stats/timeseries")
    public Mono<R<List<TimeseriesPointVO>>> timeseries(
            @RequestParam(value = "granularity", defaultValue = "hour") String granularity,
            @RequestParam(value = "rangeHours", defaultValue = "24") int rangeHours,
            @RequestParam(value = "rangeKey", required = false) String rangeKey) {
        int effectiveHours = resolveEffectiveHours(rangeKey, rangeHours);
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.timeseries(tenantId, granularity, effectiveHours)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/top")
    public Mono<R<List<TopEntityVO>>> top(@RequestParam(value = "dimension", defaultValue = "device") String dimension,
                                          @RequestParam(value = "rangeHours", defaultValue = "24") int rangeHours,
                                          @RequestParam(value = "rangeKey", required = false) String rangeKey,
                                          @RequestParam(value = "limit", defaultValue = "10") int limit) {
        int effectiveHours = resolveEffectiveHours(rangeKey, rangeHours);
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.top(tenantId, dimension, effectiveHours, limit)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/stream")
    public Mono<R<List<LatestPointValueVO>>> stream(@RequestParam(value = "size", defaultValue = "20") int size) {
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.latestStream(tenantId, size)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/alert/stats")
    public Mono<R<AlertStatsVO>> alertStats() {
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.alertStats(tenantId)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/alert/latest")
    public Mono<R<List<AlertItemVO>>> alertLatest(@RequestParam(value = "size", defaultValue = "10") int size) {
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.alertLatest(tenantId, size)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/stats/latency")
    public Mono<R<List<LatencyBucketVO>>> latencyHistogram(
            @RequestParam(value = "rangeHours", defaultValue = "24") int rangeHours,
            @RequestParam(value = "rangeKey", required = false) String rangeKey) {
        int effectiveHours = resolveEffectiveHours(rangeKey, rangeHours);
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.latencyHistogram(tenantId, effectiveHours)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/stats/activity")
    public Mono<R<List<ActivityCellVO>>> hourlyActivity(
            @RequestParam(value = "rangeHours", defaultValue = "168") int rangeHours,
            @RequestParam(value = "rangeKey", required = false) String rangeKey) {
        int effectiveHours = resolveEffectiveHours(rangeKey, rangeHours);
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.hourlyActivity(tenantId, effectiveHours)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     * System-wide health snapshot for the home banner. Infra / center probes are
     * platform-wide but driver / device fleet summaries are tenant-scoped, so we thread
     * tenantId through to match the gRPC facades' tenant filter.
     */
    @GetMapping("/system/health")
    public Mono<R<SystemHealthVO>> systemHealth() {
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(systemHealthService.snapshot(tenantId)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @org.springframework.web.bind.annotation.PostMapping("/alert/page")
    public Mono<R<com.baomidou.mybatisplus.extension.plugins.pagination.Page<AlertItemVO>>> alertPage(
            @org.springframework.web.bind.annotation.RequestBody(
                    required = false) io.github.pnoker.common.data.entity.query.AlertPageQuery query) {
        return getTenantId().flatMap(tenantId -> {
            try {
                io.github.pnoker.common.data.entity.query.AlertPageQuery q = query == null
                        ? new io.github.pnoker.common.data.entity.query.AlertPageQuery() : query;
                java.time.LocalDateTime from = TimeRangeUtil.resolveFrom(q.getRangeKey(), q.getRangeHours());
                long current = q.getCurrent() == null ? 1L : q.getCurrent();
                long size = q.getSize() == null ? 20L : q.getSize();
                return Mono.just(R.ok(dashboardService.alertPage(tenantId, q.getSource(), q.getEventTypeFlag(),
                        q.getConfirmFlag(), from, current, size)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @org.springframework.web.bind.annotation.PostMapping("/alert/confirm/{source}/{id}")
    public Mono<R<Boolean>> alertConfirm(@org.springframework.web.bind.annotation.PathVariable String source,
                                         @org.springframework.web.bind.annotation.PathVariable Long id) {
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.confirmAlert(tenantId, source, id)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @org.springframework.web.bind.annotation.PostMapping("/alert/unconfirm/{source}/{id}")
    public Mono<R<Boolean>> alertUnconfirm(@org.springframework.web.bind.annotation.PathVariable String source,
                                           @org.springframework.web.bind.annotation.PathVariable Long id) {
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.unconfirmAlert(tenantId, source, id)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     * Bulk confirm or unconfirm. Body = { confirm: true|false, items: [{source, id}, ...]
     * }. Returns the number of rows actually changed.
     */
    @org.springframework.web.bind.annotation.PostMapping("/alert/bulk-confirm")
    public Mono<R<Integer>> alertBulkConfirm(
            @org.springframework.web.bind.annotation.RequestBody Map<String, Object> body) {
        return getTenantId().flatMap(tenantId -> {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) body.getOrDefault("items",
                        java.util.Collections.emptyList());
                boolean confirm = body.get("confirm") == null || Boolean.parseBoolean(body.get("confirm").toString());
                return Mono.just(R.ok(dashboardService.bulkConfirmAlert(tenantId, items, confirm)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/alert/trend")
    public Mono<R<List<AlertTrendVO>>> alertTrend(@RequestParam(value = "days", defaultValue = "30") int days,
                                                  @RequestParam(value = "rangeKey", required = false) String rangeKey) {
        int effectiveDays = resolveEffectiveDays(rangeKey, days);
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.alertTrend(tenantId, effectiveDays)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/alert/top-sources")
    public Mono<R<List<AlertTopSourceVO>>> alertTopSources(@RequestParam(value = "days", defaultValue = "30") int days,
                                                           @RequestParam(value = "rangeKey", required = false) String rangeKey,
                                                           @RequestParam(value = "limit", defaultValue = "10") int limit) {
        int effectiveDays = resolveEffectiveDays(rangeKey, days);
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.alertTopSources(tenantId, effectiveDays, limit)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     * Resolve the UI's {@code rangeKey} (preferred) against the legacy {@code rangeHours}
     * integer. {@code TODAY} is mapped to the hours elapsed since local midnight so
     * bucketed queries cover the full day so far; other keys use the canonical 24 / 168 /
     * 720 hour spans.
     */
    private int resolveEffectiveHours(String rangeKey, int rangeHours) {
        Integer resolved = TimeRangeUtil.resolveHours(rangeKey, rangeHours);
        return resolved != null ? resolved : rangeHours;
    }

    /**
     * Resolve the UI's {@code rangeKey} against the legacy {@code days} integer for
     * day-bucketed endpoints. {@code TODAY} / {@code H24} both collapse to 1 day.
     */
    private int resolveEffectiveDays(String rangeKey, int days) {
        Integer resolved = TimeRangeUtil.resolveDays(rangeKey, days);
        return resolved != null ? resolved : days;
    }

    @GetMapping("/alert/activity")
    public Mono<R<List<AlertActivityCellVO>>> alertActivity(@RequestParam(value = "days", defaultValue = "7") int days,
                                                            @RequestParam(value = "rangeKey", required = false) String rangeKey) {
        int effectiveDays = resolveEffectiveDays(rangeKey, days);
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.alertActivity(tenantId, effectiveDays)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/alert/type-distribution")
    public Mono<R<List<AlertTypeBucketVO>>> alertTypeDistribution(
            @RequestParam(value = "days", defaultValue = "30") int days,
            @RequestParam(value = "rangeKey", required = false) String rangeKey) {
        int effectiveDays = resolveEffectiveDays(rangeKey, days);
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.alertTypeDistribution(tenantId, effectiveDays)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/alert/storm-sources")
    public Mono<R<List<AlertTopSourceVO>>> alertStormSources(
            @RequestParam(value = "hours", defaultValue = "1") int hours,
            @RequestParam(value = "minCount", defaultValue = "10") int minCount,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.alertStormSources(tenantId, hours, minCount, limit)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    // ===== Phase-2 insights =====================================================

    @GetMapping("/alert/flapping")
    public Mono<R<List<FlappingSourceVO>>> alertFlapping(@RequestParam(value = "hours", defaultValue = "6") int hours,
                                                         @RequestParam(value = "minCount", defaultValue = "5") int minCount,
                                                         @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.alertFlapping(tenantId, hours, minCount, limit)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/alert/correlation")
    public Mono<R<List<CorrelationPairVO>>> alertCorrelation(
            @RequestParam(value = "hours", defaultValue = "24") int hours,
            @RequestParam(value = "windowSec", defaultValue = "30") int windowSec,
            @RequestParam(value = "limit", defaultValue = "15") int limit) {
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.alertCorrelation(tenantId, hours, windowSec, limit)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/alert/peer-deviation")
    public Mono<R<List<PeerDeviationVO>>> alertPeerDeviation(
            @RequestParam(value = "days", defaultValue = "7") int days) {
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.alertPeerDeviation(tenantId, days)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/alert/aging")
    public Mono<R<AgingBacklogVO>> alertAging() {
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.alertAgingBacklog(tenantId)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/alert/mtta")
    public Mono<R<List<MttaTrendVO>>> alertMtta(@RequestParam(value = "days", defaultValue = "30") int days) {
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.alertMtta(tenantId, days)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/protocol/health")
    public Mono<R<List<ProtocolHealthVO>>> protocolHealth() {
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.protocolHealth(tenantId)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/alert/change-impact")
    public Mono<R<List<ChangeImpactVO>>> changeImpact(@RequestParam(value = "days", defaultValue = "30") int days,
                                                      @RequestParam(value = "limit", defaultValue = "30") int limit) {
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.changeImpact(tenantId, days, limit)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/silent/sources")
    public Mono<R<List<SilentSourceVO>>> silentSources(
            @RequestParam(value = "baselineDays", defaultValue = "7") int baselineDays,
            @RequestParam(value = "silentMinutes", defaultValue = "15") int silentMinutes,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.silentSources(tenantId, baselineDays, silentMinutes, limit)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/coverage/gap")
    public Mono<R<CoverageGapVO>> coverageGap(@RequestParam(value = "limit", defaultValue = "100") int limit) {
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.coverageGap(tenantId, limit)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

}
