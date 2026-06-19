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
import io.github.pnoker.common.data.biz.DeviceStatusService;
import io.github.pnoker.common.data.entity.query.DeviceQuery;
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
 * REST controller exposing device status management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "device_status", description = "Device operational status: query current online, offline, fault, and maintenance states of registered industrial devices")
@Slf4j
@RestController
@RequestMapping(DataConstant.DEVICE_STATUS_URL_PREFIX)
@RequiredArgsConstructor
public class DeviceStatusController implements BaseController {

    private final DeviceStatusService deviceStatusService;

    /**
     * Query the current status (ONLINE, OFFLINE, MAINTAIN, FAULT) of the tenant's devices.
     *
     * @param deviceQuery optional device filter (driver, profile, other dimensions) and pagination; a default empty query is used when null
     * @return a map of device id to current status for the matching devices
     */
    @PreAuthorize("@perm.can('device_status', 'list')")
    @Operation(summary = "List Device Status", description = "Return the current status (ONLINE, OFFLINE, MAINTAIN, FAULT) of the current tenant's devices as a device-id-to-status map. Use to read live connectivity state; pass a DeviceQuery to filter by driver, profile or other dimensions.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<R<Map<Long, String>>> deviceStatus(@RequestBody(required = false) DeviceQuery deviceQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DeviceQuery query = Objects.isNull(deviceQuery) ? new DeviceQuery() : deviceQuery;
            query.setTenantId(tenantId);
            Map<Long, String> statuses = deviceStatusService.getStatusByPage(query);
            return R.ok(statuses);
        }));
    }

    /**
     * Query the current status (ONLINE, OFFLINE, MAINTAIN, FAULT) of every device the
     * tenant runs under a given driver.
     *
     * @param driverId identifier of the driver; must belong to the current tenant
     * @return a map of device id to current status for the devices managed by the driver
     */
    @PreAuthorize("@perm.can('device_status', 'list')")
    @Operation(summary = "List Device Status by Driver", description = "Return the current status (ONLINE, OFFLINE, MAINTAIN, FAULT) of every device the current tenant runs under the given driver. Use to inspect connectivity across the whole fleet a single driver manages; results are a device-id-to-status map.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_by_driver_id")
    public Mono<R<Map<Long, String>>> deviceStatusByDriverId(@Parameter(description = "Identifier of the driver; must belong to the current tenant. Only devices managed by this driver are returned.", example = "1024") @NotNull @RequestParam(value = "driver_id") Long driverId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DeviceQuery deviceQuery = new DeviceQuery();
            deviceQuery.setDriverId(driverId);
            deviceQuery.setTenantId(tenantId);
            Map<Long, String> statuses = deviceStatusService.getStatusByPage(deviceQuery);
            return R.ok(statuses);
        }));
    }

    /**
     * Query the current status (ONLINE, OFFLINE, MAINTAIN, FAULT) of every device the
     * tenant associates with a given profile.
     *
     * @param profileId identifier of the profile template; must belong to the current tenant
     * @return a map of device id to current status for the devices bound to the profile
     */
    @PreAuthorize("@perm.can('device_status', 'list')")
    @Operation(summary = "List Device Status by Profile", description = "Return the current status (ONLINE, OFFLINE, MAINTAIN, FAULT) of every device the current tenant associates with the given profile. Use to check connectivity of all devices sharing a profile template; results are a device-id-to-status map.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_by_profile_id")
    public Mono<R<Map<Long, String>>> deviceStatusByProfileId(
            @Parameter(description = "Identifier of the profile template; must belong to the current tenant. Only devices bound to this profile are returned.", example = "1024") @NotNull @RequestParam(value = "profile_id") Long profileId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DeviceQuery deviceQuery = new DeviceQuery();
            deviceQuery.setProfileId(profileId);
            deviceQuery.setTenantId(tenantId);
            Map<Long, String> statuses = deviceStatusService.getStatusByPage(deviceQuery);
            return R.ok(statuses);
        }));
    }

}
