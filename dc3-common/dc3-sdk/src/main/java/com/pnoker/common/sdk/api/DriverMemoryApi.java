/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pnoker.common.sdk.api;

import com.pnoker.common.bean.driver.DriverOperation;
import com.pnoker.common.constant.Common;
import com.pnoker.common.constant.Operation;
import com.pnoker.common.sdk.service.DriverCommonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 驱动 memory 操作 Rest Api
 *
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(Common.Service.DC3_DRIVER_URL_PREFIX)
public class DriverMemoryApi {
    @Resource
    private DriverCommonService driverCommonService;


    @PostMapping("/memory")
    public void memory(@RequestBody DriverOperation operation) {
        switch (operation.getCommand()) {
            case Operation.Profile.ADD:
                driverCommonService.addProfile(operation.getId());
                break;
            case Operation.Profile.DELETE:
                driverCommonService.deleteProfile(operation.getId());
                break;
            case Operation.Profile.UPDATE:
                driverCommonService.updateProfile(operation.getId());
                break;
            case Operation.Device.ADD:
                driverCommonService.addDevice(operation.getId());
                break;
            case Operation.Device.DELETE:
                driverCommonService.deleteDevice(operation.getId());
                break;
            case Operation.Device.UPDATE:
                driverCommonService.updateDevice(operation.getId());
                break;
            default:
                break;
        }
    }
}
