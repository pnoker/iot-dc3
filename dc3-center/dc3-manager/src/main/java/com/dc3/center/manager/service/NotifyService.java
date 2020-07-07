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

import com.dc3.common.model.Driver;

/**
 * <p>Notify Interface
 *
 * @author pnoker
 */
public interface NotifyService {

    /**
     * 通知驱动 新增模板(ADD) / 删除模板(DELETE)
     *
     * @param driver        driver
     * @param profileId     Profile Id
     * @param operationType Operation Type
     */
    void notifyDriverProfile(Driver driver, Long profileId, String operationType);

    /**
     * 通知驱动 新增设备(ADD) / 删除设备(DELETE) / 修改设备(UPDATE)
     *
     * @param deviceId      Device Id
     * @param profileId     Profile Id
     * @param operationType Operation Type
     */
    void notifyDriverDevice(Long deviceId, Long profileId, String operationType);

    /**
     * 通知驱动 新增位号(ADD) / 删除位号(DELETE) / 修改位号(UPDATE)
     *
     * @param pointId       Point Id
     * @param profileId     Profile Id
     * @param operationType Operation Type
     */
    void notifyDriverPoint(Long pointId, Long profileId, String operationType);

    /**
     * 通知驱动 新增驱动配置(ADD) / 删除驱动配置(DELETE) / 更新驱动配置(UPDATE)
     *
     * @param driverInfoId  Driver Info Id
     * @param attributeId   Attribute Id
     * @param profileId     Profile Id
     * @param operationType Operation Type
     */
    void notifyDriverDriverInfo(Long driverInfoId, Long attributeId, Long profileId, String operationType);

    /**
     * 通知驱动 新增位号配置(ADD) / 删除位号配置(DELETE) / 更新位号配置(UPDATE)
     *
     * @param pointInfoId   Point Id
     * @param attributeId   Attribute Id
     * @param deviceId      Device Id
     * @param operationType Operation Type
     */
    void notifyDriverPointInfo(Long pointInfoId, Long attributeId, Long deviceId, String operationType);

}
