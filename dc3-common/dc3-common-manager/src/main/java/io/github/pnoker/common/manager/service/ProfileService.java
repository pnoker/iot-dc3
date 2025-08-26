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
import io.github.pnoker.common.enums.ProfileTypeFlagEnum;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.entity.query.ProfileQuery;

import java.util.List;
import java.util.Set;

/**
 * Profile Interface
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public interface ProfileService extends BaseService<ProfileBO, ProfileQuery> {

    /**
     * 根据 模板名称和模版类型 查询模版
     *
     * @param tenantId 租户ID
     * @param name     模板名称
     * @param type     模板类型 {@link ProfileTypeFlagEnum}
     * @return ProfileBO
     */
    ProfileBO selectByNameAndType(Long tenantId, String name, ProfileTypeFlagEnum type);

    /**
     * 根据 设备ID 查询模版
     *
     * @param deviceId 设备ID
     * @return ProfileBO 集合
     */
    List<ProfileBO> selectByDeviceId(Long deviceId);

    /**
     * 根据 模版ID集 查询模版
     *
     * @param ids 模版ID集
     * @return ProfileBO 集合
     */
    List<ProfileBO> selectByIds(Set<Long> ids);

}
