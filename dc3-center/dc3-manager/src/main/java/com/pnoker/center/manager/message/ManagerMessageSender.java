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

package com.pnoker.center.manager.message;

import com.pnoker.center.manager.service.DeviceService;
import com.pnoker.center.manager.service.ProfileService;
import com.pnoker.common.bean.driver.DriverOperation;
import com.pnoker.common.constant.Operation;
import com.pnoker.common.model.Device;
import com.pnoker.common.model.Profile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.messaging.support.MessageBuilder;

import javax.annotation.Resource;

/**
 * @author pnoker
 */
@Slf4j
@EnableBinding(TopicOutput.class)
public class ManagerMessageSender {

    @Resource
    private TopicOutput topicOutput;
    @Resource
    private ProfileService profileService;
    @Resource
    private DeviceService deviceService;

    public void notifyDriver(String command, Long id) {
        Long driverId;
        switch (command) {
            case Operation.Device.ADD:
            case Operation.Device.DELETE:
            case Operation.Device.UPDATE:
                driverId = getDriverByDevice(id);
                break;
            case Operation.Profile.ADD:
            case Operation.Profile.DELETE:
            case Operation.Profile.UPDATE:
                driverId = getDriverByProfile(id);
                break;
            case Operation.Schedule.ADD:
            case Operation.Schedule.DELETE:
            case Operation.Schedule.UPDATE:
            default:
                return;
        }
        if (null != driverId) {
            DriverOperation operation = new DriverOperation(command, id, driverId);
            topicOutput.driverOutput().send(MessageBuilder.withPayload(operation).build());
        }
    }

    public Long getDriverByDevice(Long id) {
        Device device = deviceService.selectById(id);
        if (null != device) {
            Profile profile = profileService.selectById(device.getProfileId());
            if (null != profile) {
                return profile.getDriverId();
            }
        }
        return null;
    }

    public Long getDriverByProfile(Long id) {
        Profile profile = profileService.selectById(id);
        if (null != profile) {
            return profile.getDriverId();
        }
        return null;
    }
}
