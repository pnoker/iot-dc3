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

package io.github.pnoker.common.manager.controller;

import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.manager.entity.vo.dashboard.DeviceStatsVO;
import io.github.pnoker.common.manager.entity.vo.dashboard.DriverStatsVO;
import io.github.pnoker.common.manager.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Manager-side dashboard endpoints. Two read-only GETs that power the
 * home page's driver / device distribution tabs:
 *
 * <ul>
 *   <li>{@code GET /manager/dashboard/driver/stats}</li>
 *   <li>{@code GET /manager/dashboard/device/stats?topN=10}</li>
 * </ul>
 *
 * @author pnoker
 * @since 2026.5.2
 */
@Slf4j
@RestController
@RequestMapping(ManagerConstant.DASHBOARD_URL_PREFIX)
public class DashboardController implements BaseController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/driver/stats")
    public Mono<R<DriverStatsVO>> driverStats() {
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.driverStats(tenantId)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @GetMapping("/device/stats")
    public Mono<R<DeviceStatsVO>> deviceStats(
            @RequestParam(value = "topN", defaultValue = "10") int topN) {
        return getTenantId().flatMap(tenantId -> {
            try {
                return Mono.just(R.ok(dashboardService.deviceStats(tenantId, topN)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }
}
