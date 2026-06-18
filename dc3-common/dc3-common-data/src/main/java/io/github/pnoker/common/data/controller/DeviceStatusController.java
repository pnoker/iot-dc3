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
     * Query device statuses ONLINE, OFFLINE, MAINTAIN, FAULT
     *
     * @param deviceQuery Device Dto
     * @return Map String:String
     */
    @PreAuthorize("@perm.can('device_status', 'list')")
    @Operation(summary = "List Device Status", description = "Return the current status (ONLINE, OFFLINE, MAINTAIN, FAULT) of the current tenant's devices as a device-id-to-status map. Use to read live connectivity state; pass a DeviceQuery to filter by driver, profile or other dimensions.")
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
     * Query device statuses by driver ID ONLINE, OFFLINE, MAINTAIN, FAULT
     *
     * @param driverId Driver ID
     * @return Map String:String
     */
    @PreAuthorize("@perm.can('device_status', 'list')")
    @Operation(summary = "List Device Status by Driver", description = "Return the current status (ONLINE, OFFLINE, MAINTAIN, FAULT) of every device the current tenant runs under the given driver. Use to inspect connectivity across the whole fleet a single driver manages; results are a device-id-to-status map.")
    @GetMapping("/list_by_driver_id")
    public Mono<R<Map<Long, String>>> deviceStatusByDriverId(@Parameter(description = "Driver ID") @NotNull @RequestParam(value = "driver_id") Long driverId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DeviceQuery deviceQuery = new DeviceQuery();
            deviceQuery.setDriverId(driverId);
            deviceQuery.setTenantId(tenantId);
            Map<Long, String> statuses = deviceStatusService.getStatusByPage(deviceQuery);
            return R.ok(statuses);
        }));
    }

    /**
     * Query device statuses by profile ID ONLINE, OFFLINE, MAINTAIN, FAULT
     *
     * @param profileId Profile ID
     * @return Map String:String
     */
    @PreAuthorize("@perm.can('device_status', 'list')")
    @Operation(summary = "List Device Status by Profile", description = "Return the current status (ONLINE, OFFLINE, MAINTAIN, FAULT) of every device the current tenant associates with the given profile. Use to check connectivity of all devices sharing a profile template; results are a device-id-to-status map.")
    @GetMapping("/list_by_profile_id")
    public Mono<R<Map<Long, String>>> deviceStatusByProfileId(
            @Parameter(description = "Profile ID") @NotNull @RequestParam(value = "profile_id") Long profileId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DeviceQuery deviceQuery = new DeviceQuery();
            deviceQuery.setProfileId(profileId);
            deviceQuery.setTenantId(tenantId);
            Map<Long, String> statuses = deviceStatusService.getStatusByPage(deviceQuery);
            return R.ok(statuses);
        }));
    }

}
