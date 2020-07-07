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

package com.dc3.center.manager.service;

import com.dc3.common.base.Service;
import com.dc3.common.dto.DriverDto;
import com.dc3.common.model.Driver;

import java.util.Map;

/**
 * <p>Driver Interface
 *
 * @author pnoker
 */
public interface DriverService extends Service<Driver, DriverDto> {
    /**
     * 根据驱动 SERVICE NAME 查询
     *
     * @param serviceName Driver Service Name
     * @return Driver
     */
    Driver selectByServiceName(String serviceName);

    /**
     * 根据驱动 HOST & PORT 查询
     *
     * @param host Driver Service Host
     * @param port Driver Service Port
     * @return Driver
     */
    Driver selectByHostPort(String host, Integer port);

    /**
     * 根据 DEVICE ID 查询
     *
     * @param deviceId Device Id
     * @return Driver
     */
    Driver selectByDeviceId(Long deviceId);

    /**
     * 根据 PROFILE ID 查询
     *
     * @param profileId Profile Id
     * @return Driver
     */
    Driver selectByProfileId(Long profileId);

    /**
     * 查询 Driver 服务状态
     *
     * @param driverDto Driver Dto
     * @return Map<String, Boolean>
     */
    Map<String, Boolean> driverStatus(DriverDto driverDto);
}
