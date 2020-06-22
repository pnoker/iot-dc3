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
import com.dc3.common.dto.DeviceDto;
import com.dc3.common.model.Device;

import java.util.Map;

/**
 * <p>Device Interface
 *
 * @author pnoker
 */
public interface DeviceService extends Service<Device, DeviceDto> {

    /**
     * 根据设备 NAME 和分组 ID 查询
     *
     * @param groupId Device Group Id
     * @param name    Device Name
     * @return Device
     */
    Device selectDeviceByNameAndGroup(long groupId, String name);

    /**
     * 查询 Device 服务状态
     *
     * @param deviceDto Device Dto
     * @return Map<Long, String>
     */
    Map<Long, String> deviceStatus(DeviceDto deviceDto);
}
