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

import io.github.pnoker.common.facade.entity.bo.FacadeTenantBO;

/**
 * Protocol-neutral tenant facade. Mirrors {@code api.center.auth.TenantApi}.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface TenantFacade {

    /**
     * @return the tenant, or {@code null} when no tenant carries the given code.
     */
    FacadeTenantBO getByCode(String code);

}
