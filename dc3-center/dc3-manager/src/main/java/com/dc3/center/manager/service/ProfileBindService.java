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
import com.dc3.common.dto.ProfileBindDto;
import com.dc3.common.model.ProfileBind;

import java.util.Set;

/**
 * <p>ProfileBind Interface
 *
 * @author pnoker
 */
public interface ProfileBindService extends Service<ProfileBind, ProfileBindDto> {

    /**
     * 根据 设备Id 删除关联的模版映射
     *
     * @param deviceId Device Id
     */
    boolean deleteByDeviceId(Long deviceId);

    /**
     * 根据 模版ID和设备Id 删除关联的模版映射
     *
     * @param profileId Profile Id
     * @param deviceId  Device Id
     */
    boolean deleteByProfileIdAndDeviceId(Long profileId, Long deviceId);

    /**
     * 根据 模版ID 查询关联的 设备ID 集合
     *
     * @param profileId Profile Id
     * @return Device Id Set
     */
    Set<Long> selectDeviceIdByProfileId(Long profileId);

    /**
     * 根据 设备ID 查询关联的 模版ID 集合
     *
     * @param deviceId Device Id
     * @return Profile Id Set
     */
    Set<Long> selectProfileIdByDeviceId(Long deviceId);

}
