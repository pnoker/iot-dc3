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

package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.entity.common.TenantOwned;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.service.DeviceService;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.EntityTenantService;
import io.github.pnoker.common.manager.service.PointService;
import io.github.pnoker.common.manager.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Tenant guard for manager entities referenced by polymorphic bindings.
 *
 * @author pnoker
 * @version 2026.6.10
 * @since 2026.6.10
 */
@Service
@RequiredArgsConstructor
public class EntityTenantServiceImpl implements EntityTenantService {

    private final DriverService driverService;

    private final ProfileService profileService;

    private final PointService pointService;

    private final DeviceService deviceService;

    @Override
    public void requireEntityTenant(Long tenantId, EntityTypeEnum entityTypeFlag, Long entityId) {
        if (Objects.isNull(entityTypeFlag)) {
            throw new NotFoundException("Resource does not exist");
        }
        switch (entityTypeFlag) {
            case DRIVER -> requireTenant(tenantId, driverService.getById(entityId));
            case PROFILE -> requireTenant(tenantId, profileService.getById(entityId));
            case POINT -> requireTenant(tenantId, pointService.getById(entityId));
            case DEVICE -> requireTenant(tenantId, deviceService.getById(entityId));
            default -> throw new NotFoundException("Resource does not exist");
        }
    }

    private <T extends TenantOwned> void requireTenant(Long tenantId, T entity) {
        if (Objects.isNull(entity) || !Objects.equals(tenantId, entity.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
    }

}
