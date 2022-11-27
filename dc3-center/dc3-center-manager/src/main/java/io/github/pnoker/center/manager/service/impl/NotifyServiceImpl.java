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

package io.github.pnoker.center.manager.service.impl;

import io.github.pnoker.center.manager.service.DriverService;
import io.github.pnoker.center.manager.service.NotifyService;
import io.github.pnoker.common.bean.driver.DriverConfiguration;
import io.github.pnoker.common.constant.RabbitConstant;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * NotifyService Impl
 *
 * @author pnoker
 * @since 2022.1.0
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
        try {
            List<Driver> drivers = driverService.selectByProfileId(profile.getId());
            drivers.forEach(driver -> {
                DriverConfiguration operation = new DriverConfiguration().setType(PrefixConstant.PROFILE).setCommand(command).setContent(profile);
                notifyDriver(driver, operation);
            });
        } catch (Exception e) {
            log.warn("Notify driver {} profile error: {}", command, e.getMessage());
        }
    }

    @Override
    public void notifyDriverPoint(String command, Point point) {
        try {
            List<Driver> drivers = driverService.selectByProfileId(point.getProfileId());
            drivers.forEach(driver -> {
                DriverConfiguration operation = new DriverConfiguration().setType(PrefixConstant.POINT).setCommand(command).setContent(point);
                notifyDriver(driver, operation);
            });
        } catch (Exception e) {
            log.error("Notify driver {} point: {}", command, e.getMessage());
        }
    }

    @Override
    public void notifyDriverDevice(String command, Device device) {
        try {
            Driver driver = driverService.selectById(device.getDriverId());
            DriverConfiguration operation = new DriverConfiguration().setType(PrefixConstant.DEVICE).setCommand(command).setContent(device);
            notifyDriver(driver, operation);
        } catch (Exception e) {
            log.error("Notify driver {} device: {}", command, e.getMessage());
        }
    }

    @Override
    public void notifyDriverDriverInfo(String command, DriverInfo driverInfo) {
        try {
            Driver driver = driverService.selectByDeviceId(driverInfo.getDeviceId());
            DriverConfiguration operation = new DriverConfiguration().setType(PrefixConstant.DRIVER_INFO).setCommand(command).setContent(driverInfo);
            notifyDriver(driver, operation);
        } catch (Exception e) {
            log.error("Notify driver {} driverInfo: {}", command, e.getMessage());
        }
    }

    @Override
    public void notifyDriverPointInfo(String command, PointInfo pointInfo) {
        try {
            Driver driver = driverService.selectByDeviceId(pointInfo.getDeviceId());
            DriverConfiguration operation = new DriverConfiguration().setType(PrefixConstant.POINT_INFO).setCommand(command).setContent(pointInfo);
            notifyDriver(driver, operation);
        } catch (Exception e) {
            log.error("Notify driver {} pointInfo: {}", command, e.getMessage());
        }
    }

    /**
     * notify driver
     *
     * @param driver              Driver
     * @param driverConfiguration DriverConfiguration
     */
    private void notifyDriver(Driver driver, DriverConfiguration driverConfiguration) {
        log.info("Notify driver[{}] : {}", driver.getServiceName(), driverConfiguration);
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_METADATA, RabbitConstant.ROUTING_DRIVER_METADATA_PREFIX + driver.getServiceName(), driverConfiguration);
    }

}
