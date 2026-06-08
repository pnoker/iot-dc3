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

package io.github.pnoker.common.manager.controller;

import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.manager.entity.vo.dashboard.DeviceStatsVO;
import io.github.pnoker.common.manager.entity.vo.dashboard.DriverStatsVO;
import io.github.pnoker.common.manager.entity.vo.dashboard.GrowthVO;
import io.github.pnoker.common.manager.entity.vo.dashboard.TopologyVO;
import io.github.pnoker.common.manager.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Manager-side dashboard endpoints. Two read-only GETs that power the home page's driver
 * / device distribution tabs:
 *
 * <ul>
 * <li>{@code GET /manager/dashboard/driver/stats}</li>
 * <li>{@code GET /manager/dashboard/device/stats?topN=10}</li>
 * </ul>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.2
 */
@Tag(name = "dashboard", description = "仪表盘")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.DASHBOARD_URL_PREFIX)
@RequiredArgsConstructor
public class DashboardController implements BaseController {

    private final DashboardService dashboardService;

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "查询仪表盘统计", description = "查询仪表盘统计数据")
    @GetMapping("/driver/stats")
    public Mono<R<DriverStatsVO>> driverStats() {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.driverStats(tenantId))));
    }

    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "查询仪表盘统计", description = "查询仪表盘统计数据")
    @GetMapping("/device/stats")
    public Mono<R<DeviceStatsVO>> deviceStats(@RequestParam(value = "top_n", defaultValue = "10") int topN) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.deviceStats(tenantId, topN))));
    }

    /**
     * Daily new-row counts for driver / device / point / profile tables over the last
     * {@code days} days. Backs the stat-card sparklines. Returns fixed-length zero-padded
     * arrays so the frontend never has to reason about missing days.
     */
    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "仪表盘 - daily growth", description = "仪表盘 - daily growth")
    @GetMapping("/growth")
    public Mono<R<GrowthVO>> dailyGrowth(@RequestParam(value = "days", defaultValue = "7") int days) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.dailyGrowth(tenantId, days))));
    }

    /**
     * Four-column Sankey topology (Driver → Device → Profile → Point).
     * {@code mode=cardinality} is the Phase 0 default and counts relationships along each
     * edge; future {@code mode=volume} will weight edges by point_value sample counts
     * over {@code rangeKey}.
     *
     * <p>
     * Top-N cropping is server-side — each cropped slice becomes an {@code others:*}
     * pseudo-node with a {@code hiddenChildren} payload the frontend pops in a drill-in
     * dialog.
     * </p>
     */
    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "查询仪表盘排行", description = "查询仪表盘排行榜")
    @GetMapping("/topology")
    public Mono<R<TopologyVO>> topology(@RequestParam(value = "mode", defaultValue = "cardinality") String mode,
                                        @Parameter(description = "range key", required = true)
                                        @RequestParam(value = "range_key", required = false) String rangeKey) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.topology(tenantId, mode, rangeKey))));
    }

}
