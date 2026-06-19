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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * REST controller exposing driver status management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "driver_status", description = "Driver operational status: query current connected, disconnected, and error states of protocol driver instances")
@Slf4j
@RestController
@RequestMapping(DataConstant.DRIVER_STATUS_URL_PREFIX)
@RequiredArgsConstructor
public class DriverStatusController implements BaseController {

    private final DriverStatusService driverStatusService;

    /**
     * Query the current ONLINE/OFFLINE status of each driver instance for the tenant.
     *
     * @param entityQuery optional driver filter and pagination; a default empty query is used when null
     * @return a map of driver id to current ONLINE/OFFLINE status for the matching drivers
     */
    @PreAuthorize("@perm.can('driver_status', 'list')")
    @Operation(summary = "List Driver Status", description = "Return the current ONLINE/OFFLINE status of each driver instance for the current tenant, keyed by driver id. Page through driver and pagination filters; results are tenant-scoped.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
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
     * Count the devices currently online under one driver for the tenant.
     *
     * @param driverId identifier of the driver whose online device count is queried; must belong to the current tenant
     * @return the number of devices currently online under the driver
     */
    @PreAuthorize("@perm.can('driver_status', 'get')")
    @Operation(summary = "Count Online Devices by Driver", description = "Count devices currently online under one driver for the current tenant. Pass the driver id; returns a single count.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_device_online_by_driver_id")
    public Mono<R<Long>> getDeviceOnlineByDriverId(@Parameter(description = "Identifier of the driver whose online device count is queried; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "driver_id") Long driverId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            Long result = driverStatusService.getDeviceOnlineByDriverId(tenantId, driverId);
            return R.ok(result);
        }));
    }

    /**
     * Count the devices currently offline under one driver for the tenant.
     *
     * @param driverId identifier of the driver whose offline device count is queried; must belong to the current tenant
     * @return the number of devices currently offline under the driver
     */
    @PreAuthorize("@perm.can('driver_status', 'get')")
    @Operation(summary = "Count Offline Devices by Driver", description = "Count devices currently offline under one driver for the current tenant. Pass the driver id; returns a single count.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_device_offline_by_driver_id")
    public Mono<R<Long>> getDeviceOfflineByDriverId(@Parameter(description = "Identifier of the driver whose offline device count is queried; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "driver_id") Long driverId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            Long result = driverStatusService.getDeviceOfflineByDriverId(tenantId, driverId);
            return R.ok(result);
        }));
    }

}
