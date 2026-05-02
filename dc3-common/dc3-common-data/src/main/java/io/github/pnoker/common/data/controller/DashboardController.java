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
                Map<String, Object> out = new HashMap<>();
                out.put("today", today);
                out.put("yesterday", yesterday);
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
            @RequestParam(value = "rangeHours", defaultValue = "24") int rangeHours) {
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.timeseries(tenantId, granularity, rangeHours)));
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
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.top(tenantId, dimension, rangeHours, limit)));
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
            @RequestParam(value = "rangeHours", defaultValue = "24") int rangeHours) {
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.latencyHistogram(tenantId, rangeHours)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/stats/activity")
    public Mono<R<List<ActivityCellVO>>> hourlyActivity(
            @RequestParam(value = "rangeHours", defaultValue = "168") int rangeHours) {
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.hourlyActivity(tenantId, rangeHours)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     * System-wide health snapshot for the home banner. Not tenant-scoped
     * because the status of shared infra (DB / MQ / gateway) and sibling
     * services is a platform-wide concern.
     */
    @GetMapping("/system/health")
    public Mono<R<SystemHealthVO>> systemHealth() {
        try {
            return Mono.just(R.ok(systemHealthService.snapshot()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }
}
