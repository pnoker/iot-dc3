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

package com.dc3.common.sdk.service;

import com.dc3.common.model.*;

/**
 * @author pnoker
 */
public interface DriverMetadataService {

    /**
     * 初始化 SDK
     */
    void initial();

    /**
     * 向 DeviceDriver 中添加模板
     *
     * @param profile Profile
     */
    void upsertProfile(Profile profile);

    /**
     * 删除 DeviceDriver 中模板
     *
     * @param id Id
     */
    void deleteProfile(Long id);

    /**
     * 向 DeviceDriver 中添加驱动配置信息
     *
     * @param driverInfo DriverInfo
     */
    void upsertDriverInfo(DriverInfo driverInfo);

    /**
     * 删除 DeviceDriver 中添加驱动配置信息
     *
     * @param attributeId Attribute Id
     * @param profileId   Profile Id
     */
    void deleteDriverInfo(Long attributeId, Long profileId);

    /**
     * 向 DeviceDriver 中添加设备
     *
     * @param device Device
     */
    void upsertDevice(Device device);

    /**
     * 删除 DeviceDriver 中设备
     *
     * @param id Id
     */
    void deleteDevice(Long id);

    /**
     * 向 DeviceDriver 中添加位号
     *
     * @param point Point
     */
    void upsertPoint(Point point);

    /**
     * 删除 DeviceDriver 中位号
     *
     * @param id        Id
     * @param profileId Profile Id
     */
    void deletePoint(Long id, Long profileId);

    /**
     * 向 DeviceDriver 中添加位号配置信息
     *
     * @param pointInfo PointInfo
     */
    void upsertPointInfo(PointInfo pointInfo);

    /**
     * 删除 DeviceDriver 中添加位号配置信息
     *
     * @param pointId     Point Id
     * @param attributeId Attribute Id
     * @param deviceId    Device Id
     */
    void deletePointInfo(Long pointId, Long attributeId, Long deviceId);
}
