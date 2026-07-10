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

package io.github.pnoker.common.facade.api;

import java.util.Set;

/**
 * Protocol-neutral permission facade. Mirrors {@code api.center.auth.PermissionApi}.
 *
 * @author pnoker
 * @version 2026.6.0
 * @since 2026.6.0
 */
public interface PermissionFacade {

    /**
     * List all resource codes granted to the given principal in the tenant.
     *
     * @param tenantId    tenant scope
     * @param principalId target principal
     * @return full resource code set; empty when the principal has no grants
     */
    Set<String> listPermissionCodes(Long tenantId, Long principalId);

}
