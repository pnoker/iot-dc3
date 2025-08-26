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
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.query.DriverQuery;

import java.util.List;
import java.util.Set;

/**
 * Driver Interface
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public interface DriverService extends BaseService<DriverBO, DriverQuery> {

    /**
     * 根据 驱动ServiceName 查询 驱动
     *
     * @param serviceName 驱动服务名称
     * @param tenantId    租户ID
     * @return Driver
     */
    DriverBO selectByServiceName(String serviceName, Long tenantId);

    /**
     * 根据 模版ID 查询 驱动集
     *
     * @param profileId 模版ID
     * @return Driver 集合
     */
    List<DriverBO> selectByProfileId(Long profileId);

    /**
     * 根据 位号ID 查询 驱动集
     *
     * @param pointId 位号ID
     * @return Driver 集合
     */
    List<DriverBO> selectByPointId(Long pointId);

    /**
     * 根据 驱动ID 查询 驱动
     *
     * @param deviceId 设备ID
     * @return Driver
     */
    DriverBO selectByDeviceId(Long deviceId);

    /**
     * 根据 驱动ID集 查询 驱动集
     *
     * @param ids 驱动ID 集合
     * @return Driver 集合
     */
    List<DriverBO> selectByIds(Set<Long> ids);
}
