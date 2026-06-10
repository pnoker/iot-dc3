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
 * REST controller exposing device status management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "device_status", description = "设备状态")
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
    @Operation(summary = "查询设备状态列表", description = "分页查询设备状态映射")
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
    @Operation(summary = "查询驱动下设备状态", description = "根据驱动ID查询设备状态映射")
    @GetMapping("/list_by_driver_id")
    public Mono<R<Map<Long, String>>> deviceStatusByDriverId(@NotNull @RequestParam(value = "driver_id") Long driverId) {
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
    @Operation(summary = "查询模板下设备状态", description = "根据模板ID查询设备状态映射")
    @GetMapping("/list_by_profile_id")
    public Mono<R<Map<Long, String>>> deviceStatusByProfileId(
            @NotNull @RequestParam(value = "profile_id") Long profileId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DeviceQuery deviceQuery = new DeviceQuery();
            deviceQuery.setProfileId(profileId);
            deviceQuery.setTenantId(tenantId);
            Map<Long, String> statuses = deviceStatusService.getStatusByPage(deviceQuery);
            return R.ok(statuses);
        }));
    }

}
