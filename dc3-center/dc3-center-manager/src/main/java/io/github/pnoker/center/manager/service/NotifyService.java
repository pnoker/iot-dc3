/*
 * Copyright 2016-present the original author or authors.
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

import io.github.pnoker.common.enums.MetadataCommandTypeEnum;
import io.github.pnoker.common.model.*;

/**
 * Notify Interface
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface NotifyService {

    /**
     * 通知驱动 新增模板(ADD) / 删除模板(DELETE) / 修改模板(UPDATE)
     *
     * @param command Operation Type
     * @param profile Profile
     */
    void notifyDriverProfile(MetadataCommandTypeEnum command, Profile profile);

    /**
     * 通知驱动 新增位号(ADD) / 删除位号(DELETE) / 修改位号(UPDATE)
     *
     * @param command Operation Type
     * @param point   Point
     */
    void notifyDriverPoint(MetadataCommandTypeEnum command, Point point);

    /**
     * 通知驱动 新增设备(ADD) / 删除设备(DELETE) / 修改设备(UPDATE)
     *
     * @param command Operation Type
     * @param device  Device
     */
    void notifyDriverDevice(MetadataCommandTypeEnum command, Device device);

    /**
     * 通知驱动 新增驱动配置(ADD) / 删除驱动配置(DELETE) / 更新驱动配置(UPDATE)
     *
     * @param command               Operation Type
     * @param driverAttributeConfig Driver Attribute Config
     */
    void notifyDriverDriverAttributeConfig(MetadataCommandTypeEnum command, DriverAttributeConfig driverAttributeConfig);

    /**
     * 通知驱动 新增位号配置(ADD) / 删除位号配置(DELETE) / 更新位号配置(UPDATE)
     *
     * @param command              Operation Type
     * @param pointAttributeConfig PointInfo
     */
    void notifyDriverPointInfo(MetadataCommandTypeEnum command, PointAttributeConfig pointAttributeConfig);

}
