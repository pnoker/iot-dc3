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

import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDriverQuery;

import java.util.Collection;
import java.util.List;

/**
 * Protocol-neutral driver facade.
 * <p>
 * Mirrors the RPCs on {@code api.center.manager.DriverApi}. Single-record and bulk
 * lookups are tenant-scoped: the tenant id is carried on the gRPC query so the manager
 * center's tenant-line interceptor scopes the SQL, and the local implementation binds
 * it on the thread. Callers must supply the tenant id explicitly.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface DriverFacade {

    /**
     * Tenant-scoped single lookup. Returns {@code null} when the driver is missing or
     * belongs to another tenant.
     */
    FacadeDriverBO getById(Long tenantId, Long id);

    /**
     * Tenant-scoped bulk lookup. Missing or cross-tenant drivers are omitted.
     */
    List<FacadeDriverBO> listByIds(Long tenantId, Collection<Long> ids);

    /**
     * @return a page of drivers (never {@code null}; empty page when nothing matches).
     */
    FacadePage<FacadeDriverBO> listByPage(FacadeDriverQuery query);

    /**
     * Tenant-scoped owner lookup. Returns {@code null} when the owning driver is missing
     * or belongs to another tenant.
     */
    FacadeDriverBO getByDeviceId(Long tenantId, Long deviceId);

}
