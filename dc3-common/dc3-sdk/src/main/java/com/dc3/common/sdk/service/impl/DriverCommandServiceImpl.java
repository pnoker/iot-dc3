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

package com.dc3.common.sdk.service.impl;

import com.dc3.common.sdk.service.DriverService;
import com.dc3.common.bean.driver.PointValue;
import com.dc3.common.model.Device;
import com.dc3.common.sdk.bean.AttributeInfo;
import com.dc3.common.sdk.bean.DriverContext;
import com.dc3.common.sdk.service.DriverCommandService;
import com.dc3.common.sdk.service.rabbit.PointValueService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class DriverCommandServiceImpl implements DriverCommandService {
    @Resource
    private PointValueService pointValueService;
    @Resource
    private DriverContext driverContext;
    @Resource
    private DriverService driverService;

    @Override
    @SneakyThrows
    public PointValue read(Long deviceId, Long pointId) {
        Device device = driverContext.getDevice(deviceId);
        String rawValue = driverService.read(driverContext.getProfileDriverInfo(device.getProfileId()), driverContext.getDevicePointInfo(deviceId, pointId), device, driverContext.getDevicePoint(deviceId, pointId));
        PointValue pointValue = pointValueService.convertValue(deviceId, pointId, rawValue);
        pointValueService.pointValueSender(pointValue);
        return pointValue;
    }

    @Override
    @SneakyThrows
    public Boolean write(Long deviceId, Long pointId, String value) {
        Device device = driverContext.getDevice(deviceId);
        return driverService.write(driverContext.getProfileDriverInfo(device.getProfileId()), driverContext.getDevicePointInfo(deviceId, pointId), device, new AttributeInfo(value, driverContext.getDevicePoint(deviceId, pointId).getType()));
    }

}
