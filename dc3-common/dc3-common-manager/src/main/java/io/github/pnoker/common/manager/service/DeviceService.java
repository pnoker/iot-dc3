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
     * Query a device by its device name, scoped to a tenant.
     *
     * @param name     Device name
     * @param tenantId Tenant ID
     * @return {@link DeviceBO}
     */
    DeviceBO getByName(String name, Long tenantId);

    /**
     * Query a device by its device code, scoped to a tenant.
     *
     * @param code     Device code
     * @param tenantId Tenant ID
     * @return {@link DeviceBO}
     */
    DeviceBO getByCode(String code, Long tenantId);

    /**
     * Query devices served by a driver, scoped to a tenant.
     *
     * @param driverId Driver ID
     * @param tenantId Tenant ID
     * @return {@link DeviceBO} list
     */
    List<DeviceBO> listByDriverId(Long driverId, Long tenantId);

    /**
     * Query the device IDs served by a driver, scoped to a tenant.
     *
     * @param driverId Driver ID
     * @param tenantId Tenant ID
     * @return device ID list
     */
    List<Long> listIdsByDriverId(Long driverId, Long tenantId);

    /**
     * Query devices sharing a profile, scoped to a tenant.
     *
     * @param profileId Profile ID
     * @param tenantId  Tenant ID
     * @return {@link DeviceBO} list
     */
    List<DeviceBO> listByProfileId(Long profileId, Long tenantId);

    /**
     * Query devices by a set of device IDs.
     *
     * @param ids Device ID list
     * @return {@link DeviceBO} list
     */
    List<DeviceBO> listByIds(List<Long> ids);

    /**
     * Import devices from an uploaded file into the given profile and driver context.
     *
     * @param entityBO device context carrying tenant, profile, and driver associations
     * @param file     the uploaded import file
     */
    void importDevice(DeviceBO entityBO, File file);

    /**
     * Generate a populated import template file for the given device context.
     *
     * @param entityBO device context carrying tenant, profile, and driver associations
     * @return path to the generated template file
     */
    Path generateImportTemplate(DeviceBO entityBO);

}
