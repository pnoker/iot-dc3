/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * <p>Route summary (all GET, all under {@code /api/v3/data/dashboard}):</p>
 * <ul>
 *   <li>{@code /stats/today} — today total + yesterday total for delta</li>
 *   <li>{@code /stats/timeseries?granularity=hour|day&rangeHours=24}</li>
 *   <li>{@code /top?dimension=device|point|driver&rangeHours=24&limit=10}</li>
 *   <li>{@code /stream?size=20} — most recent rows (user-triggered refresh)</li>
 *   <li>{@code /alert/stats} — total + unconfirmed + by-type breakdown</li>
 *   <li>{@code /alert/latest?size=10} — most recent alerts</li>
 * </ul>
 *
 * @author pnoker
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
    public Mono<R<List<TopEntityVO>>> top(
            @RequestParam(value = "dimension", defaultValue = "device") String dimension,
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
    public Mono<R<List<LatestPointValueVO>>> stream(
            @RequestParam(value = "size", defaultValue = "20") int size) {
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
    public Mono<R<List<AlertItemVO>>> alertLatest(
            @RequestParam(value = "size", defaultValue = "10") int size) {
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
     * System-wide health snapshot for the home banner. Infra / center probes
     * are platform-wide but driver / device fleet summaries are tenant-scoped,
     * so we thread tenantId through to match the gRPC facades' tenant filter.
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
    public Mono<R<Map<String, Object>>> alertPage(
            @org.springframework.web.bind.annotation.RequestBody(required = false) Map<String, Object> body) {
        return getTenantId().flatMap(tenantId -> {
            try {
                Map<String, Object> b = body == null ? new HashMap<>() : body;
                String source = b.get("source") == null ? null : b.get("source").toString();
                Integer typeFlag = b.get("eventTypeFlag") == null ? null :
                        Integer.parseInt(b.get("eventTypeFlag").toString());
                Integer confirmFlag = b.get("confirmFlag") == null ? null :
                        Integer.parseInt(b.get("confirmFlag").toString());
                Integer rangeHours = b.get("rangeHours") == null ? null :
                        Integer.parseInt(b.get("rangeHours").toString());
                String rangeKey = b.get("rangeKey") == null ? null : b.get("rangeKey").toString();
                java.time.LocalDateTime from = TimeRangeUtil.resolveFrom(rangeKey, rangeHours);
                long current = b.get("current") == null ? 1L : Long.parseLong(b.get("current").toString());
                long size = b.get("size") == null ? 20L : Long.parseLong(b.get("size").toString());
                return Mono.just(R.ok(dashboardService.alertPage(tenantId, source, typeFlag, confirmFlag, from, current, size)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @org.springframework.web.bind.annotation.PostMapping("/alert/confirm/{source}/{id}")
    public Mono<R<Boolean>> alertConfirm(
            @org.springframework.web.bind.annotation.PathVariable String source,
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
    public Mono<R<Boolean>> alertUnconfirm(
            @org.springframework.web.bind.annotation.PathVariable String source,
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
     * Bulk confirm or unconfirm. Body = { confirm: true|false, items:
     * [{source, id}, ...] }. Returns the number of rows actually changed.
     */
    @org.springframework.web.bind.annotation.PostMapping("/alert/bulk-confirm")
    public Mono<R<Integer>> alertBulkConfirm(
            @org.springframework.web.bind.annotation.RequestBody Map<String, Object> body) {
        return getTenantId().flatMap(tenantId -> {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>)
                        body.getOrDefault("items", java.util.Collections.emptyList());
                boolean confirm = body.get("confirm") == null
                        || Boolean.parseBoolean(body.get("confirm").toString());
                return Mono.just(R.ok(dashboardService.bulkConfirmAlert(tenantId, items, confirm)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/alert/trend")
    public Mono<R<List<AlertTrendVO>>> alertTrend(
            @RequestParam(value = "days", defaultValue = "30") int days,
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
    public Mono<R<List<AlertTopSourceVO>>> alertTopSources(
            @RequestParam(value = "days", defaultValue = "30") int days,
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
     * Resolve the UI's {@code rangeKey} (preferred) against the legacy
     * {@code rangeHours} integer. {@code TODAY} is mapped to the hours
     * elapsed since local midnight so bucketed queries cover the full day
     * so far; other keys use the canonical 24 / 168 / 720 hour spans.
     */
    private int resolveEffectiveHours(String rangeKey, int rangeHours) {
        Integer resolved = TimeRangeUtil.resolveHours(rangeKey, rangeHours);
        return resolved != null ? resolved : rangeHours;
    }

    /**
     * Resolve the UI's {@code rangeKey} against the legacy {@code days}
     * integer for day-bucketed endpoints. {@code TODAY} / {@code H24} both
     * collapse to 1 day.
     */
    private int resolveEffectiveDays(String rangeKey, int days) {
        Integer resolved = TimeRangeUtil.resolveDays(rangeKey, days);
        return resolved != null ? resolved : days;
    }
}
