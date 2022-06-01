/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
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
import com.dc3.common.dto.ProfileBindDto;
import com.dc3.common.model.ProfileBind;

import java.util.List;
import java.util.Set;

/**
 * ProfileBind Interface
 *
 * @author pnoker
 */
public interface ProfileBindService extends Service<ProfileBind, ProfileBindDto> {

    /**
     * 根据 设备ID 新增关联的模版映射
     *
     * @param deviceId   Device Id
     * @param profileIds Profile Id Set
     * @return ProfileBind Array
     */
    List<ProfileBind> addByDeviceId(String deviceId, Set<String> profileIds);

    /**
     * 根据 设备ID 删除关联的模版映射
     *
     * @param deviceId Device Id
     * @return boolean
     */
    boolean deleteByDeviceId(String deviceId);

    /**
     * 根据 设备ID 和 模版ID 删除关联的模版映射
     *
     * @param deviceId  Device Id
     * @param profileId Profile Id
     * @return boolean
     */
    boolean deleteByProfileIdAndDeviceId(String deviceId, String profileId);

    /**
     * 根据 设备ID 和 模版ID 查询关联的模版映射
     *
     * @param deviceId  Device Id
     * @param profileId Profile Id
     * @return ProfileBind
     */
    ProfileBind selectByDeviceIdAndProfileId(String deviceId, String profileId);

    /**
     * 根据 模版ID 查询关联的 设备ID 集合
     *
     * @param profileId Profile Id
     * @return Device Id Set
     */
    Set<String> selectDeviceIdByProfileId(String profileId);

    /**
     * 根据 设备ID 查询关联的 模版ID 集合
     *
     * @param deviceId Device Id
     * @return Profile Id Set
     */
    Set<String> selectProfileIdByDeviceId(String deviceId);

}
