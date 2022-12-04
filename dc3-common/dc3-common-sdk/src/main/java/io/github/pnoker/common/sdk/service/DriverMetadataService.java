/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.sdk.service;

import io.github.pnoker.common.model.*;

/**
 * @author pnoker
 * @since 2022.1.0
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
     * @param id ID
     */
    void deleteProfile(String id);

    /**
     * 向 DeviceDriver 中添加设备
     *
     * @param device Device
     */
    void upsertDevice(Device device);

    /**
     * 删除 DeviceDriver 中设备
     *
     * @param id ID
     */
    void deleteDevice(String id);

    /**
     * 向 DeviceDriver 中添加位号
     *
     * @param point Point
     */
    void upsertPoint(Point point);

    /**
     * 删除 DeviceDriver 中位号
     *
     * @param profileId 模板ID
     * @param id        ID
     */
    void deletePoint(String profileId, String id);

    /**
     * 向 DeviceDriver 中添加驱动配置信息
     *
     * @param driverInfo DriverInfo
     */
    void upsertDriverInfo(DriverInfo driverInfo);

    /**
     * 删除 DeviceDriver 中添加驱动配置信息
     *
     * @param deviceId    Device ID
     * @param attributeId Attribute ID
     */
    void deleteDriverInfo(String deviceId, String attributeId);

    /**
     * 向 DeviceDriver 中添加位号配置信息
     *
     * @param pointInfo PointInfo
     */
    void upsertPointInfo(PointInfo pointInfo);

    /**
     * 删除 DeviceDriver 中添加位号配置信息
     *
     * @param deviceId    Device ID
     * @param pointId     Point ID
     * @param attributeId Attribute ID
     */
    void deletePointInfo(String deviceId, String pointId, String attributeId);
}
