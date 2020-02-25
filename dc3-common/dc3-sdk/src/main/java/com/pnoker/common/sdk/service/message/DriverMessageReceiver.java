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

package com.pnoker.common.sdk.service.message;

import com.pnoker.common.bean.driver.DriverOperation;
import com.pnoker.common.constant.Common;
import com.pnoker.common.constant.Operation;
import com.pnoker.common.sdk.bean.DriverContext;
import com.pnoker.common.sdk.service.DriverCommonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;

import javax.annotation.Resource;

/**
 * @author pnoker
 */
@Slf4j
@EnableBinding(TopicInput.class)
public class DriverMessageReceiver {
    @Resource
    private DriverContext driverContext;
    @Resource
    private DriverCommonService driverCommonService;

    @StreamListener(Common.Topic.DRIVER_TOPIC)
    public void driverReceive(DriverOperation operation) {
        if (operation.getDriverId() == driverContext.getDriverId()) {
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
}
