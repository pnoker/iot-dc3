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

package io.github.pnoker.common.sdk.service.impl;

import io.github.pnoker.common.bean.driver.AttributeInfo;
import io.github.pnoker.common.bean.point.PointValue;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.Device;
import io.github.pnoker.common.sdk.bean.driver.DriverContext;
import io.github.pnoker.common.sdk.service.DriverCommandService;
import io.github.pnoker.common.sdk.service.DriverCustomService;
import io.github.pnoker.common.sdk.service.DriverService;
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
    private DriverContext driverContext;
    @Resource
    private DriverService driverService;
    @Resource
    private DriverCustomService driverCustomService;

    @Override
    public PointValue read(String deviceId, String pointId) {
        Device device = driverContext.getDeviceByDeviceId(deviceId);

        try {
            String rawValue = driverCustomService.read(
                    driverContext.getDriverInfoByDeviceId(deviceId),
                    driverContext.getPointInfoByDeviceIdAndPointId(deviceId, pointId),
                    device,
                    driverContext.getPointByDeviceIdAndPointId(deviceId, pointId)
            );

            PointValue pointValue = new PointValue(deviceId, pointId, rawValue, driverService.convertValue(deviceId, pointId, rawValue));
            driverService.pointValueSender(pointValue);
            return pointValue;
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }

    @Override
    public Boolean write(String deviceId, String pointId, String value) {
        Device device = driverContext.getDeviceByDeviceId(deviceId);
        try {
            return driverCustomService.write(
                    driverContext.getDriverInfoByDeviceId(deviceId),
                    driverContext.getPointInfoByDeviceIdAndPointId(deviceId, pointId),
                    device,
                    new AttributeInfo(value, driverContext.getPointByDeviceIdAndPointId(deviceId, pointId).getType())
            );
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }

}
