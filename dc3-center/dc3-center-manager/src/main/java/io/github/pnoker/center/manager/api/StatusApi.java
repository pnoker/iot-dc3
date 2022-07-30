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

package io.github.pnoker.center.manager.api;

import io.github.pnoker.api.center.manager.feign.StatusClient;
import io.github.pnoker.center.manager.service.StatusService;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.ServiceConstant;
import io.github.pnoker.common.dto.DeviceDto;
import io.github.pnoker.common.dto.DriverDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 设备 Client 接口实现
 *
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(ServiceConstant.Manager.STATUS_URL_PREFIX)
public class StatusApi implements StatusClient {

    @Resource
    private StatusService statusService;

    @Override
    public R<Map<String, String>> driverStatus(DriverDto driverDto, String tenantId) {
        try {
            driverDto.setTenantId(tenantId);
            Map<String, String> statuses = statusService.driver(driverDto);
            return R.ok(statuses);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<Map<String, String>> deviceStatus(DeviceDto deviceDto, String tenantId) {
        try {
            deviceDto.setTenantId(tenantId);
            Map<String, String> statuses = statusService.device(deviceDto);
            return R.ok(statuses);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<Map<String, String>> deviceStatusByDriverId(String driverId) {
        try {
            DeviceDto deviceDto = new DeviceDto();
            deviceDto.setDriverId(driverId);
            Map<String, String> statuses = statusService.device(deviceDto);
            return R.ok(statuses);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<Map<String, String>> deviceStatusByProfileId(String profileId) {
        try {
            Map<String, String> statuses = statusService.deviceByProfileId(profileId);
            return R.ok(statuses);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

}
