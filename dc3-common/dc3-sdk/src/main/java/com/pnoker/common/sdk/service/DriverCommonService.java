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

package com.pnoker.common.sdk.service;

/**
 * @author pnoker
 */
public interface DriverCommonService {
    /**
     * 初始化 SDK
     *
     * @return
     */
    void initial();

    /**
     * 向 DeviceDriver 中添加设备
     *
     * @param id
     * @return
     */
    void addDevice(Long id);

    /**
     * 删除 DeviceDriver 中设备
     *
     * @param id
     * @return
     */
    void deleteDevice(Long id);

    /**
     * 更新 DeviceDriver 中设备
     *
     * @param id
     */
    void updateDevice(Long id);

    /**
     * 向 DeviceDriver 中添加模板
     *
     * @param id
     * @return
     */
    void addProfile(Long id);

    /**
     * 删除 DeviceDriver 中模板
     *
     * @param id
     * @return
     */
    void deleteProfile(Long id);

    /**
     * 更新 DeviceDriver 中模板
     *
     * @param id
     */
    void updateProfile(Long id);

}
