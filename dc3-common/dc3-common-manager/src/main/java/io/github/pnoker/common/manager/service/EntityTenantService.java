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

import io.github.pnoker.common.enums.EntityTypeEnum;

/**
 * Tenant guard for manager entities referenced by polymorphic bindings.
 *
 * @author pnoker
 * @version 2026.6.10
 * @since 2026.6.10
 */
public interface EntityTenantService {

    /**
     * Verify that a manager entity exists and belongs to the given tenant.
     *
     * @param tenantId       Tenant ID
     * @param entityTypeFlag Entity type
     * @param entityId       Entity ID
     */
    void requireEntityTenant(Long tenantId, EntityTypeEnum entityTypeFlag, Long entityId);

}
