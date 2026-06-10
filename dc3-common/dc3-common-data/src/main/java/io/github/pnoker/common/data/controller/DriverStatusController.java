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
import io.github.pnoker.common.data.biz.DriverStatusService;
import io.github.pnoker.common.data.entity.query.DriverQuery;
import io.github.pnoker.common.entity.R;
import jakarta.validation.constraints.NotNull;
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

import java.util.Map;
import java.util.Objects;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing driver status management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "driver_status", description = "驱动状态")
@Slf4j
@RestController
@RequestMapping(DataConstant.DRIVER_STATUS_URL_PREFIX)
@RequiredArgsConstructor
public class DriverStatusController implements BaseController {

    private final DriverStatusService driverStatusService;

    /**
     * Query driver statuses ONLINE, OFFLINE
     *
     * @param entityQuery Driver and pagination parameters
     * @return Map String:String
     */
    @PreAuthorize("@perm.can('driver_status', 'list')")
    @Operation(summary = "查询驱动状态列表", description = "分页查询驱动状态映射")
    @PostMapping("/list")
    public Mono<R<Map<Long, String>>> driverStatus(@RequestBody(required = false) DriverQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverQuery query = Objects.isNull(entityQuery) ? new DriverQuery() : entityQuery;
            query.setTenantId(tenantId);
            Map<Long, String> statuses = driverStatusService.getStatusByPage(query);
            return R.ok(statuses);
        }));
    }

    /**
     * Query the number of devices currently online under the Driver
     *
     * @param driverId Driver ID
     * @return Number of devices currently online
     */
    @PreAuthorize("@perm.can('driver_status', 'get')")
    @Operation(summary = "统计驱动在线设备数", description = "根据驱动ID统计当前在线设备数量")
    @GetMapping("/get_device_online_by_driver_id")
    public Mono<R<Long>> getDeviceOnlineByDriverId(@NotNull @RequestParam(value = "driver_id") Long driverId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            Long result = driverStatusService.getDeviceOnlineByDriverId(tenantId, driverId);
            return R.ok(result);
        }));
    }

    /**
     * Query the number of devices currently offline under the Driver
     *
     * @param driverId Driver ID
     * @return Number of devices currently offline
     */
    @PreAuthorize("@perm.can('driver_status', 'get')")
    @Operation(summary = "统计驱动离线设备数", description = "根据驱动ID统计当前离线设备数量")
    @GetMapping("/get_device_offline_by_driver_id")
    public Mono<R<Long>> getDeviceOfflineByDriverId(@NotNull @RequestParam(value = "driver_id") Long driverId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            Long result = driverStatusService.getDeviceOfflineByDriverId(tenantId, driverId);
            return R.ok(result);
        }));
    }

}
