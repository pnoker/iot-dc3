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
import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.common.manager.entity.query.PointAttributeQuery;

import java.util.List;

/**
 * 位号属性Interface
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public interface PointAttributeService extends BaseService<PointAttributeBO, PointAttributeQuery> {
    /**
     * 根据 位号配置属性编号 和 驱动ID 查询
     *
     * @param name     属性编号
     * @param driverId 驱动ID
     * @return PointAttribute
     */
    PointAttributeBO selectByNameAndDriverId(String name, Long driverId);

    /**
     * 根据 驱动ID 查询
     *
     * @param driverId 驱动ID
     * @return 位号属性Array
     */
    List<PointAttributeBO> selectByDriverId(Long driverId);
}
