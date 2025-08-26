/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.base.service.BaseService;
import io.github.pnoker.common.manager.entity.bo.ProfileBindBO;
import io.github.pnoker.common.manager.entity.query.ProfileBindQuery;

import java.util.List;

/**
 * ProfileBind Interface
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public interface ProfileBindService extends BaseService<ProfileBindBO, ProfileBindQuery> {

    /**
     * 根据 设备ID 删除关联的模版映射
     *
     * @param deviceId 设备ID
     * @return 是否删除
     */
    Boolean removeByDeviceId(Long deviceId);

    /**
     * 根据 设备ID 和 模版ID 删除关联的模版映射
     *
     * @param deviceId  设备ID
     * @param profileId 位号ID
     * @return 是否删除
     */
    Boolean removeByDeviceIdAndProfileId(Long deviceId, Long profileId);

    /**
     * 根据 设备ID 和 模版ID 查询关联的模版映射
     *
     * @param deviceId  设备ID
     * @param profileId 位号ID
     * @return ProfileBind
     */
    ProfileBindBO selectByDeviceIdAndProfileId(Long deviceId, Long profileId);

    /**
     * 根据 模版ID 查询关联的 设备ID 集合
     *
     * @param profileId 位号ID
     * @return 设备ID集
     */
    List<Long> selectDeviceIdsByProfileId(Long profileId);

    /**
     * 根据 设备ID 查询关联的 模版ID 集合
     *
     * @param deviceId 设备ID
     * @return 模版ID集
     */
    List<Long> selectProfileIdsByDeviceId(Long deviceId);

}
