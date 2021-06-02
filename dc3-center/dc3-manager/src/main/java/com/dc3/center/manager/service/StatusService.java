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

import com.dc3.common.dto.DeviceDto;
import com.dc3.common.dto.DriverDto;

import java.util.Map;

/**
 * <p>Device Interface
 *
 * @author pnoker
 */
public interface StatusService {

    /**
     * 根据 驱动ServiceName 查询 Driver 服务状态
     *
     * @param serviceName Driver ServiceName
     * @return String
     */
    String driver(String serviceName);

    /**
     * 分页查询 Driver 服务状态，同驱动分页查询配套使用
     *
     * @param driverDto Driver Dto
     * @return Map<String, String>
     */
    Map<Long, String> driver(DriverDto driverDto);

    /**
     * 根据 设备Id 查询 Device 服务状态
     *
     * @param id Device Id
     * @return String
     */
    String device(Long id);

    /**
     * 分页查询 Device 服务状态，同设备分页查询配套使用
     *
     * @param deviceDto Device Dto
     * @return Map<Long, String>
     */
    Map<Long, String> device(DeviceDto deviceDto);
}
