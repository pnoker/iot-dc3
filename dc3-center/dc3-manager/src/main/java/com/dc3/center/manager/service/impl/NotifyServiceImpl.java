/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.center.manager.service.impl;

import com.dc3.center.manager.service.DriverService;
import com.dc3.center.manager.service.NotifyService;
import com.dc3.common.bean.driver.DriverConfiguration;
import com.dc3.common.constant.Common;
import com.dc3.common.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * NotifyService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class NotifyServiceImpl implements NotifyService {

    @Resource
    private DriverService driverService;
    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public void notifyDriverProfile(String command, Profile profile) {
        Driver driver = driverService.selectByProfileId(profile.getId());
        if (null != driver) {
            DriverConfiguration operation = new DriverConfiguration().setType(Common.Driver.Type.PROFILE).setCommand(command).setContent(profile);
            notifyDriver(driver, operation);
        }
    }

    @Override
    public void notifyDriverDevice(String command, Device device) {
        Driver driver = driverService.selectByProfileId(device.getProfileId());
        if (null != driver) {
            DriverConfiguration operation = new DriverConfiguration().setType(Common.Driver.Type.DEVICE).setCommand(command).setContent(device);
            notifyDriver(driver, operation);
        }
    }

    @Override
    public void notifyDriverPoint(String command, Point point) {
        Driver driver = driverService.selectByProfileId(point.getProfileId());
        if (null != driver) {
            DriverConfiguration operation = new DriverConfiguration().setType(Common.Driver.Type.POINT).setCommand(command).setContent(point);
            notifyDriver(driver, operation);
        }
    }


    @Override
    public void notifyDriverDriverInfo(String command, DriverInfo driverInfo) {
        Driver driver = driverService.selectByProfileId(driverInfo.getProfileId());
        if (null != driver) {
            DriverConfiguration operation = new DriverConfiguration().setType(Common.Driver.Type.DRIVER_INFO).setCommand(command).setContent(driverInfo);
            notifyDriver(driver, operation);
        }
    }

    @Override
    public void notifyDriverPointInfo(String command, PointInfo pointInfo) {
        Driver driver = driverService.selectByDeviceId(pointInfo.getDeviceId());
        if (null != driver) {
            DriverConfiguration operation = new DriverConfiguration().setType(Common.Driver.Type.POINT_INFO).setCommand(command).setContent(pointInfo);
            notifyDriver(driver, operation);
        }
    }

    /**
     * notify driver
     *
     * @param driver    Driver
     * @param driverConfiguration DriverConfiguration
     */
    private void notifyDriver(Driver driver, DriverConfiguration driverConfiguration) {
        log.debug("Notify Driver {} : {}", driver.getServiceName(), driverConfiguration);
        rabbitTemplate.convertAndSend(Common.Rabbit.TOPIC_EXCHANGE_METADATA, Common.Rabbit.ROUTING_DRIVER_METADATA_PREFIX + driver.getServiceName(), driverConfiguration);
    }

}
