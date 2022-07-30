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
import io.github.pnoker.common.dto.ProfileDto;
import io.github.pnoker.common.model.Profile;

import java.util.List;
import java.util.Set;

/**
 * Profile Interface
 *
 * @author pnoker
 */
public interface ProfileService extends Service<Profile, ProfileDto> {

    /**
     * 根据 模板Name 查询模版
     *
     * @param name Profile Name
     * @param type Profile Type
     * @return Profile
     */
    Profile selectByNameAndType(String name, Short type, String tenantId);

    /**
     * 根据 模版Id集 查询模版
     *
     * @param ids Profile Id Set
     * @return Profile Array
     */
    List<Profile> selectByIds(Set<String> ids);

    /**
     * 根据 设备Id 查询模版
     *
     * @param deviceId Device Id
     * @return Profile Array
     */
    List<Profile> selectByDeviceId(String deviceId);

}
