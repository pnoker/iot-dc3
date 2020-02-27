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

package com.pnoker.center.manager.service;

/**
 * <p>Notify Interface
 *
 * @author pnoker
 */
public interface NotifyService {

    /**
     * 通知驱动 新增设备
     *
     * @param deviceId
     * @param profileId
     */
    void notifyDriverAddDevice(Long deviceId, Long profileId);

    /**
     * 通知驱动 删除设备
     *
     * @param deviceId
     * @param profileId
     */
    void notifyDriverDelDevice(Long deviceId, Long profileId);

    /**
     * 通知驱动 新增模板
     *
     * @param profileId
     */
    void notifyDriverAddProfile(Long profileId);

    /**
     * 通知驱动 删除模板
     *
     * @param profileId
     */
    void notifyDriverDelProfile(Long profileId);

    /**
     * 通知驱动 修改/新增/删除 驱动配置
     *
     * @param profileId
     */
    void notifyDriverUpdateDriverInfo(Long profileId);

    /**
     * 通知驱动 修改/新增/删除 位号配置
     *
     * @param deviceId
     */
    void notifyDriverUpdatePointInfo(Long deviceId);
}
