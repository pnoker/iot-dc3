/*
 * Copyright 2022 Pnoker All Rights Reserved
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

package io.github.pnoker.api.center.manager.feign;

import io.github.pnoker.api.center.manager.fallback.StatusClientFallback;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.ServiceConstant;
import io.github.pnoker.common.dto.DeviceDto;
import io.github.pnoker.common.dto.DriverDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 设备 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = ServiceConstant.Manager.STATUS_URL_PREFIX, name = ServiceConstant.Manager.SERVICE_NAME, fallbackFactory = StatusClientFallback.class)
public interface StatusClient {

    /**
     * 查询 Driver 服务状态
     * ONLINE, OFFLINE
     *
     * @param driverDto Driver Dto
     * @return Map<String, String>
     */
    @PostMapping("/driver")
    R<Map<String, String>> driverStatus(@RequestBody(required = false) DriverDto driverDto, @RequestHeader(value = ServiceConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

    /**
     * 查询 Device 服务状态
     * ONLINE, OFFLINE, MAINTAIN, FAULT
     *
     * @param deviceDto Device Dto
     * @return Map<String, String>
     */
    @PostMapping("/device")
    R<Map<String, String>> deviceStatus(@RequestBody(required = false) DeviceDto deviceDto, @RequestHeader(value = ServiceConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

    /**
     * 根据 驱动ID 查询 Device 服务状态
     * ONLINE, OFFLINE, MAINTAIN, FAULT
     *
     * @param driverId Driver Id
     * @return Map<String, String>
     */
    @GetMapping("/device/driver_id/{driverId}")
    R<Map<String, String>> deviceStatusByDriverId(@NotNull @PathVariable(value = "driverId") String driverId);

    /**
     * 根据 模板ID 查询 Device 服务状态
     * ONLINE, OFFLINE, MAINTAIN, FAULT
     *
     * @param profileId Profile Id
     * @return Map<String, String>
     */
    @GetMapping("/device/profile_id/{profileId}")
    R<Map<String, String>> deviceStatusByProfileId(@NotNull @PathVariable(value = "profileId") String profileId);

}
