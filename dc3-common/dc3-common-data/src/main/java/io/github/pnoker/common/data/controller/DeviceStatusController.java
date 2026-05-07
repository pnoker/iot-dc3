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
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

/**
 * Device Status Controller
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(DataConstant.DEVICE_STATUS_URL_PREFIX)
public class DeviceStatusController implements BaseController {

    private final DeviceStatusService deviceStatusService;

    public DeviceStatusController(DeviceStatusService deviceStatusService) {
        this.deviceStatusService = deviceStatusService;
    }

    /**
     * Query device statuses ONLINE, OFFLINE, MAINTAIN, FAULT
     *
     * @param deviceQuery Device Dto
     * @return Map String:String
     */
    @PostMapping("/device")
    public Mono<R<Map<Long, String>>> deviceStatus(@RequestBody(required = false) DeviceQuery deviceQuery) {
        return getTenantId().flatMap(tenantId -> {
            try {
                DeviceQuery query = Objects.isNull(deviceQuery) ? new DeviceQuery() : deviceQuery;
                query.setTenantId(tenantId);
                Map<Long, String> statuses = deviceStatusService.selectByPage(query);
                return Mono.just(R.ok(statuses));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     * Query device statuses by driver ID ONLINE, OFFLINE, MAINTAIN, FAULT
     *
     * @param driverId Driver ID
     * @return Map String:String
     */
    @GetMapping("/device/driver_id/{driverId}")
    public Mono<R<Map<Long, String>>> deviceStatusByDriverId(@NotNull @PathVariable(value = "driverId") Long driverId) {
        try {
            DeviceQuery deviceQuery = new DeviceQuery();
            deviceQuery.setDriverId(driverId);
            Map<Long, String> statuses = deviceStatusService.selectByPage(deviceQuery);
            return Mono.just(R.ok(statuses));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * Query device statuses by profile ID ONLINE, OFFLINE, MAINTAIN, FAULT
     *
     * @param profileId Profile ID
     * @return Map String:String
     */
    @GetMapping("/device/profile_id/{profileId}")
    public Mono<R<Map<Long, String>>> deviceStatusByProfileId(
            @NotNull @PathVariable(value = "profileId") Long profileId) {
        try {
            Map<Long, String> statuses = deviceStatusService.selectByProfileId(profileId);
            return Mono.just(R.ok(statuses));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

}
