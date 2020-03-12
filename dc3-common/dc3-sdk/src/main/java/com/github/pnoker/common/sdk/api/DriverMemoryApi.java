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

package com.github.pnoker.common.sdk.api;

import com.github.pnoker.common.bean.driver.DriverOperation;
import com.github.pnoker.common.constant.Common;
import com.github.pnoker.common.constant.Operation;
import com.github.pnoker.common.sdk.service.DriverCommonService;
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
        try {
            switch (operation.getCommand()) {
                case Operation.Profile.ADD:
                    driverCommonService.addProfile(operation.getId());
                    break;
                case Operation.Profile.DELETE:
                    driverCommonService.deleteProfile(operation.getId());
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
                case Operation.Point.ADD:
                    driverCommonService.addPoint(operation.getId());
                    break;
                case Operation.Point.DELETE:
                    driverCommonService.deletePoint(operation.getId(), operation.getParentId());
                    break;
                case Operation.Point.UPDATE:
                    driverCommonService.updatePoint(operation.getId());
                    break;
                case Operation.DriverInfo.ADD:
                    driverCommonService.addDriverInfo(operation.getId());
                    break;
                case Operation.DriverInfo.DELETE:
                    driverCommonService.deleteDriverInfo(operation.getAttributeId(), operation.getParentId());
                    break;
                case Operation.DriverInfo.UPDATE:
                    driverCommonService.updateDriverInfo(operation.getId());
                    break;
                case Operation.PointInfo.ADD:
                    driverCommonService.addPointInfo(operation.getId());
                    break;
                case Operation.PointInfo.DELETE:
                    driverCommonService.deletePointInfo(operation.getId(), operation.getAttributeId(), operation.getParentId());
                    break;
                case Operation.PointInfo.UPDATE:
                    driverCommonService.updatePointInfo(operation.getId());
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
