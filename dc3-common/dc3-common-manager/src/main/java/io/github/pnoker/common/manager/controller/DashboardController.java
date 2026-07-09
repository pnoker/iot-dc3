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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Manager-side dashboard endpoints. Read-only GETs that power the home page's driver
 * / device distribution tabs, growth sparklines, and topology view:
 *
 * <ul>
 * <li>{@code GET /manager/dashboard/driver/stats}</li>
 * <li>{@code GET /manager/dashboard/device/stats?top_n=10}</li>
 * <li>{@code GET /manager/dashboard/growth?days=7}</li>
 * <li>{@code GET /manager/dashboard/topology?mode=cardinality}</li>
 * </ul>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.2
 */
@Tag(name = "dashboard", description = "Manager dashboard configuration: manage device-management dashboard layouts, widgets, and display preferences")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.DASHBOARD_URL_PREFIX)
@RequiredArgsConstructor
public class DashboardController implements BaseController {

    private final DashboardService dashboardService;

    /**
     * Aggregate driver statistics for the dashboard home view.
     *
     * @return DriverStatsVO with driver counts grouped by enable status and protocol type
     */
    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Driver Statistics", description = "Aggregate driver statistics for the current tenant for the dashboard home view. " +
            "Returns driver counts grouped by enable status and protocol type; use to surface driver distribution across the tenant.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/driver/stats")
    public Mono<R<DriverStatsVO>> driverStats() {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.driverStats(tenantId))));
    }

    /**
     * Aggregate device statistics for the dashboard home view.
     *
     * @param topN number of top devices to return, ranked by point-value volume (clamped server-side)
     * @return DeviceStatsVO with device counts plus the top-N most active devices
     */
    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Device Statistics", description = "Aggregate device statistics for the current tenant for the dashboard home view. " +
            "Returns device counts plus the top-N devices by point-value volume, where N is set by the top_n parameter (default 10); " +
            "use to surface device distribution and the most active devices.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/device/stats")
    public Mono<R<DeviceStatsVO>> deviceStats(@Parameter(description = "Number of top devices to return, ranked by point-value volume; clamped server-side.", example = "10") @RequestParam(value = "top_n", defaultValue = "10") int topN) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.deviceStats(tenantId, topN))));
    }

    /**
     * Daily new-row counts for driver / device / point / profile tables over the last
     * {@code days} days. Backs the stat-card sparklines. Returns fixed-length zero-padded
     * arrays so the frontend never has to reason about missing days.
     *
     * @param days trailing day window for the trend (output is zero-padded to this length)
     * @return GrowthVO of fixed-length zero-padded daily counts per resource
     */
    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Resource Growth Trend", description = "Return daily new-row counts for driver, device, point and profile tables over the trailing days window (default 7). " +
            "Tenant-scoped and zero-padded to a fixed length per resource so missing days appear as zero; backs the stat-card sparklines.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/growth")
    public Mono<R<GrowthVO>> dailyGrowth(@Parameter(description = "Trailing day window for the growth trend; output is zero-padded to this length.", example = "7") @RequestParam(value = "days", defaultValue = "7") int days) {
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
     *
     * @param mode     aggregation mode: cardinality counts relationships per edge (default), volume weights by point-value samples
     * @param rangeKey preset time window for volume mode (today, 24h, 7d, 30d); ignored in cardinality mode
     * @return TopologyVO Sankey graph with server-side top-N cropping and others pseudo-nodes
     */
    @PreAuthorize("@perm.can('dashboard', 'get')")
    @Operation(summary = "Get Topology", description = "Build the Driver → Device → Profile → Point topology for the current tenant. " +
            "Cardinality mode (default) counts relationships along each edge; the range_key parameter selects a preset time window. " +
            "Returns a Sankey graph with server-side top-N cropping and others pseudo-nodes carrying hidden children for drill-in.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/topology")
    public Mono<R<TopologyVO>> topology(@Parameter(description = "Topology aggregation mode: cardinality counts relationships per edge (default), volume weights edges by point-value sample counts over the range window.", example = "cardinality") @RequestParam(value = "mode", defaultValue = "cardinality") String mode,
                                        @Parameter(description = "Preset time window for volume mode: today, 24h, 7d, or 30d. Ignored in cardinality mode.", example = "7d")
                                        @RequestParam(value = "range_key", required = false) String rangeKey) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(dashboardService.topology(tenantId, mode, rangeKey))));
    }

}
