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

package io.github.pnoker.common.auth.security;

import io.github.pnoker.common.auth.entity.bo.TenantBO;
import io.github.pnoker.common.auth.service.TenantService;
import io.github.pnoker.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Centralized system-administrator authorization check.
 * <p>
 * System-global entities (resources, menus, APIs) should only be created,
 * updated, or deleted by users belonging to the {@code default} tenant.
 * Controllers call {@link #assertSystemAdmin(Long)} inside business methods
 * to enforce this rule before mutating global metadata.
 *
 * @author pnoker
 * @version 2026.6.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminChecker {

    private final TenantService tenantService;

    public void assertSystemAdmin(Long tenantId) {
        TenantBO userTenant = tenantService.getById(tenantId);
        if (!"default".equals(userTenant.getTenantCode())) {
            throw new ServiceException(
                    "Only system administrators can manage system-global entities (resources, menus, APIs)"
            );
        }
    }
}
