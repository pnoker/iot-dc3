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

package com.dc3.center.manager.service;

import com.dc3.common.base.Service;
import com.dc3.common.dto.DriverDto;
import com.dc3.common.model.Driver;

import java.util.List;
import java.util.Set;

/**
 * <p>Driver Interface
 *
 * @author pnoker
 */
public interface DriverService extends Service<Driver, DriverDto> {

    /**
     * 根据 驱动ServiceName 查询 驱动
     *
     * @param serviceName Driver Service Name
     * @return Driver
     */
    Driver selectByServiceName(String serviceName);

    /**
     * 根据 驱动 Host & Port 查询 驱动
     *
     * @param type Driver Type
     * @param host Driver Service Host
     * @param port Driver Service Port
     * @return Driver
     */
    Driver selectByHostPort(String type, String host, Integer port, Long tenantId);

    /**
     * 根据 驱动Id 查询 驱动
     *
     * @param deviceId Device Id
     * @return Driver
     */
    Driver selectByDeviceId(Long deviceId);

    /**
     * 根据 驱动Id集 查询 驱动集
     *
     * @param ids Driver Id Array
     * @return Driver Array
     */
    List<Driver> selectByIds(Set<Long> ids);

    /**
     * 根据 模版Id 查询 驱动集
     *
     * @param profileId Profile Id
     * @return Driver Array
     */
    List<Driver> selectByProfileId(Long profileId);

}
