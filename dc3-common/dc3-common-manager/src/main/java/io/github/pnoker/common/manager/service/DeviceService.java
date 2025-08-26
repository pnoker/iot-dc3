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
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.query.DeviceQuery;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * 设备 Interface
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public interface DeviceService extends BaseService<DeviceBO, DeviceQuery> {

    /**
     * 根据 设备Name 和 租户Id 查询设备
     *
     * @param name     设备名称
     * @param tenantId 租户ID
     * @return {@link DeviceBO}
     */
    DeviceBO selectByName(String name, Long tenantId);

    /**
     * 根据 设备Name 和 租户Id 查询设备
     *
     * @param code     设备编号
     * @param tenantId 租户ID
     * @return {@link DeviceBO}
     */
    DeviceBO selectByCode(String code, Long tenantId);

    /**
     * 根据 驱动ID 查询该驱动下的全部设备
     *
     * @param driverId 驱动ID
     * @return {@link DeviceBO} 集合
     */
    List<DeviceBO> selectByDriverId(Long driverId);

    /**
     * 根据 驱动ID 查询该驱动下的全部设备ID集合
     *
     * @param driverId 驱动ID
     * @return {@link DeviceBO} 集合
     */
    List<Long> selectIdsByDriverId(Long driverId);

    /**
     * 根据 模板ID 查询该驱动下的全部设备
     *
     * @param profileId 模版ID
     * @return {@link DeviceBO} 集合
     */
    List<DeviceBO> selectByProfileId(Long profileId);

    /**
     * 根据 设备ID集合 查询设备
     *
     * @param ids 设备ID集
     * @return {@link DeviceBO} 集合
     */
    List<DeviceBO> selectByIds(List<Long> ids);

    /**
     * 导入设备
     *
     * @param entityBO      {@link DeviceBO}
     * @param multipartFile {@link File}
     */
    void importDevice(DeviceBO entityBO, File multipartFile);

    /**
     * 生成导入模板
     *
     * @param entityBO {@link DeviceBO}
     * @return File Path
     */
    Path generateImportTemplate(DeviceBO entityBO);

}
