/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
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

package com.dc3.center.manager.service.impl;

import com.dc3.center.manager.service.DeviceService;
import com.dc3.center.manager.service.DriverService;
import com.dc3.center.manager.service.NotifyService;
import com.dc3.center.manager.service.ProfileService;
import com.dc3.common.bean.driver.DriverOperation;
import com.dc3.common.constant.Common;
import com.dc3.common.model.Device;
import com.dc3.common.model.Driver;
import com.dc3.common.model.Profile;
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
    private DeviceService deviceService;
    @Resource
    private ProfileService profileService;
    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public void notifyDriverProfile(Long profileId, String operationType) {
        Driver driver = getDriverByProfileId(profileId);
        DriverOperation operation = new DriverOperation().setCommand(operationType).setId(profileId);
        notifyDriver(driver, operation);
    }

    @Override
    public void notifyDriverDevice(Long deviceId, Long profileId, String operationType) {
        Driver driver = getDriverByProfileId(profileId);
        DriverOperation operation = new DriverOperation().setCommand(operationType).setId(deviceId);
        notifyDriver(driver, operation);
    }

    @Override
    public void notifyDriverPoint(Long pointId, Long profileId, String operationType) {
        Driver driver = getDriverByProfileId(profileId);
        DriverOperation operation = new DriverOperation().setCommand(operationType).setId(pointId).setParentId(profileId);
        notifyDriver(driver, operation);
    }


    @Override
    public void notifyDriverDriverInfo(Long driverInfoId, Long attributeId, Long profileId, String operationType) {
        Driver driver = getDriverByProfileId(profileId);
        DriverOperation operation = new DriverOperation().setCommand(operationType).setId(driverInfoId).setParentId(profileId).setAttributeId(attributeId);
        notifyDriver(driver, operation);
    }

    @Override
    public void notifyDriverPointInfo(Long pointInfoId, Long attributeId, Long deviceId, String operationType) {
        Driver driver = getDriverByDeviceId(deviceId);
        DriverOperation operation = new DriverOperation().setCommand(operationType).setId(pointInfoId).setParentId(deviceId).setAttributeId(attributeId);
        notifyDriver(driver, operation);
    }

    /**
     * notify driver
     *
     * @param driver    Driver
     * @param operation DriverOperation
     */
    private void notifyDriver(Driver driver, DriverOperation operation) {
        log.debug("Notify Driver {} : {}", driver.getServiceName(), operation);
        rabbitTemplate.convertAndSend(Common.Rabbit.TOPIC_EXCHANGE_NOTIFY, Common.Rabbit.ROUTING_KEY_PREFIX + driver.getServiceName(), operation);
    }


    /**
     * get driver by device id
     *
     * @param deviceId Device Id
     * @return Driver
     */
    private Driver getDriverByDeviceId(Long deviceId) {
        Device device = deviceService.selectById(deviceId);
        if (null != device) {
            Profile profile = profileService.selectById(device.getProfileId());
            if (null != profile) {
                return driverService.selectById(profile.getDriverId());
            }
        }
        return null;
    }

    /**
     * get driver by profile id
     *
     * @param profileId Profile Id
     * @return Driver
     */
    private Driver getDriverByProfileId(Long profileId) {
        Profile profile = profileService.selectById(profileId);
        if (null != profile) {
            return driverService.selectById(profile.getDriverId());
        }
        return null;
    }
}
