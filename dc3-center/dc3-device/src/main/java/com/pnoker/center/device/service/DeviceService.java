/*
 * Copyright 2019 Pnoker. All Rights Reserved.
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

package com.pnoker.center.device.service;

import com.pnoker.common.base.Service;
import com.pnoker.common.dto.DeviceDto;
import com.pnoker.common.model.Device;

/**
 * <p>Device Interface
 *
 * @author pnoker
 */
public interface DeviceService extends Service<Device, DeviceDto> {

    /**
     * 根据设备 CODE 查询设备
     *
     * @param code
     * @return
     */
    Device selectByCode(String code);

    /**
     * 根据设备 NAME 和分组 ID 查询设备
     *
     * @param groupId
     * @param name
     * @return
     */
    Device selectDeviceByNameAndGroup(long groupId, String name);

}
