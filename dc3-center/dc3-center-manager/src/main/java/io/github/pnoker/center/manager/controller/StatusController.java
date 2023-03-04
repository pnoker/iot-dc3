/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.manager.controller;

import io.github.pnoker.center.manager.entity.query.DevicePageQuery;
import io.github.pnoker.center.manager.entity.query.DriverPageQuery;
import io.github.pnoker.center.manager.service.StatusService;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.entity.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 设备 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(ManagerServiceConstant.STATUS_URL_PREFIX)
public class StatusController {

    @Resource
    private StatusService statusService;

    /**
     * 查询 Driver 服务状态
     * ONLINE, OFFLINE
     *
     * @param driverPageQuery 驱动和分页参数
     * @param tenantId        租户ID
     * @return Map String:String
     */
    @PostMapping("/driver")
    public R<Map<String, String>> driverStatus(@RequestBody(required = false) DriverPageQuery driverPageQuery,
                                               @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId) {
        try {
            driverPageQuery.setTenantId(tenantId);
            Map<String, String> statuses = statusService.driver(driverPageQuery);
            return R.ok(statuses);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 查询 Device 服务状态
     * ONLINE, OFFLINE, MAINTAIN, FAULT
     *
     * @param devicePageQuery Device Dto
     * @param tenantId        租户ID
     * @return Map String:String
     */
    @PostMapping("/device")
    public R<Map<String, String>> deviceStatus(@RequestBody(required = false) DevicePageQuery devicePageQuery,
                                               @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId) {
        try {
            devicePageQuery.setTenantId(tenantId);
            Map<String, String> statuses = statusService.device(devicePageQuery);
            return R.ok(statuses);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 驱动ID 查询 Device 服务状态
     * ONLINE, OFFLINE, MAINTAIN, FAULT
     *
     * @param driverId Driver ID
     * @return Map String:String
     */
    @GetMapping("/device/driver_id/{driverId}")
    public R<Map<String, String>> deviceStatusByDriverId(@NotNull @PathVariable(value = "driverId") String driverId) {
        try {
            DevicePageQuery devicePageQuery = new DevicePageQuery();
            devicePageQuery.setDriverId(driverId);
            Map<String, String> statuses = statusService.device(devicePageQuery);
            return R.ok(statuses);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 模板ID 查询 Device 服务状态
     * ONLINE, OFFLINE, MAINTAIN, FAULT
     *
     * @param profileId Profile ID
     * @return Map String:String
     */
    @GetMapping("/device/profile_id/{profileId}")
    public R<Map<String, String>> deviceStatusByProfileId(@NotNull @PathVariable(value = "profileId") String profileId) {
        try {
            Map<String, String> statuses = statusService.deviceByProfileId(profileId);
            return R.ok(statuses);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

}
