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

package io.github.pnoker.center.manager.service;

import io.github.pnoker.common.base.Service;
import io.github.pnoker.common.dto.DeviceDto;
import io.github.pnoker.common.model.Device;

import java.util.List;
import java.util.Set;

/**
 * Device Interface
 *
 * @author pnoker
 */
public interface DeviceService extends Service<Device, DeviceDto> {

    /**
     * 根据 设备Name 和 租户Id 查询设备
     *
     * @param name     Device Name
     * @param tenantId Tenant Id
     * @return Device
     */
    Device selectByName(String name, String tenantId);

    /**
     * 根据 驱动Id 查询该驱动下的全部设备
     *
     * @param driverId Driver Id
     * @return Device Array
     */
    List<Device> selectByDriverId(String driverId);

    /**
     * 根据 模板Id 查询该驱动下的全部设备
     *
     * @param profileId Profile Id
     * @return Device Array
     */
    List<Device> selectByProfileId(String profileId);

    /**
     * 根据 设备Id集 查询设备
     *
     * @param ids Device Id Set
     * @return Device Array
     */
    List<Device> selectByIds(Set<String> ids);

}
