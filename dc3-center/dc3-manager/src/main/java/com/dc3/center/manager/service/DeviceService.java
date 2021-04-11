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
import com.dc3.common.dto.DeviceDto;
import com.dc3.common.model.Device;

import java.util.List;

/**
 * <p>Device Interface
 *
 * @author pnoker
 */
public interface DeviceService extends Service<Device, DeviceDto> {

    /**
     * 根据设备 NAME 和分组 ID 查询
     *
     * @param name    Name
     * @param groupId Group Id
     * @return Device
     */
    Device selectDeviceByNameAndGroupId(String name, Long groupId);

    /**
     * 根据模版 ID 查询
     *
     * @param profileId Profile Id
     * @return Device Array
     */
    List<Device> selectDeviceByProfileId(Long profileId);

    /**
     * 根据分组 ID 查询
     *
     * @param groupId Group Id
     * @return Device Array
     */
    List<Device> selectDeviceByGroupId(Long groupId);

}
