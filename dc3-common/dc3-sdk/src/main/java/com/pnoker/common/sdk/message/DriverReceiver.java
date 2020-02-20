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

package com.pnoker.common.sdk.message;

import com.pnoker.common.bean.driver.DriverOperation;
import com.pnoker.common.constant.Common;
import com.pnoker.common.constant.Operation;
import com.pnoker.common.sdk.init.DeviceDriver;
import com.pnoker.common.sdk.service.DriverSdkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;

import javax.annotation.Resource;

/**
 * @author pnoker
 */
@Slf4j
@EnableBinding(TopicInput.class)
public class DriverReceiver {
    @Resource
    private DeviceDriver deviceDriver;
    @Resource
    private DriverSdkService driverSdkService;

    @StreamListener(Common.Topic.DRIVER_TOPIC)
    public void driverReceive(DriverOperation operation) {
        if (operation.getDriverId() == deviceDriver.getDriverId()) {
            switch (operation.getCommand()) {
                case Operation.Profile.ADD:
                    driverSdkService.addProfile(operation.getId());
                    break;
                case Operation.Profile.DELETE:
                    driverSdkService.deleteProfile(operation.getId());
                    break;
                case Operation.Profile.UPDATE:
                    driverSdkService.updateProfile(operation.getId());
                    break;
                case Operation.Device.ADD:
                    driverSdkService.addDevice(operation.getId());
                    break;
                case Operation.Device.DELETE:
                    driverSdkService.deleteDevice(operation.getId());
                    break;
                case Operation.Device.UPDATE:
                    driverSdkService.updateDevice(operation.getId());
                    break;
                default:
                    break;
            }
        }
    }
}
