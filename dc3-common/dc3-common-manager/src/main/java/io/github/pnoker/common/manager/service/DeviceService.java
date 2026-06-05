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
 * Business service for device operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface DeviceService extends BaseService<DeviceBO, DeviceQuery> {

    /**
     * Name TenantId
     *
     * @param name     Device Name
     * @param tenantId Tenant ID
     * @return {@link DeviceBO}
     */
    DeviceBO getByName(String name, Long tenantId);

    /**
     * Name TenantId
     *
     * @param code     Device ID
     * @param tenantId Tenant ID
     * @return {@link DeviceBO}
     */
    DeviceBO getByCode(String code, Long tenantId);

    /**
     * Driver ID TenantId
     *
     * @param driverId Driver ID
     * @param tenantId Tenant ID
     * @return {@link DeviceBO}
     */
    List<DeviceBO> listByDriverId(Long driverId, Long tenantId);

    /**
     * Driver ID TenantId Device ID
     *
     * @param driverId Driver ID
     * @param tenantId Tenant ID
     * @return {@link DeviceBO}
     */
    List<Long> listIdsByDriverId(Long driverId, Long tenantId);

    /**
     * Profile ID TenantId
     *
     * @param profileId Profile ID
     * @param tenantId  Tenant ID
     * @return {@link DeviceBO}
     */
    List<DeviceBO> listByProfileId(Long profileId, Long tenantId);

    /**
     * Device ID
     *
     * @param ids Device ID
     * @return {@link DeviceBO}
     */
    List<DeviceBO> listByIds(List<Long> ids);

    /**
     * @param entityBO      {@link DeviceBO}
     * @param multipartFile {@link File}
     */
    void importDevice(DeviceBO entityBO, File multipartFile);

    /**
     * @param entityBO {@link DeviceBO}
     * @return File Path
     */
    Path generateImportTemplate(DeviceBO entityBO);

}
