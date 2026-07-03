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

import io.github.pnoker.common.facade.entity.bo.FacadeEventBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeEventQuery;

import java.util.Collection;
import java.util.List;

/**
 * Protocol-neutral event facade. Single-record and bulk lookups are tenant-scoped.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface EventFacade {

    /**
     * Tenant-scoped single lookup. Returns {@code null} when the event is missing or
     * belongs to another tenant.
     */
    FacadeEventBO getById(Long tenantId, Long id);

    /**
     * Tenant-scoped bulk lookup. Missing or cross-tenant events are omitted.
     */
    List<FacadeEventBO> listByIds(Long tenantId, Collection<Long> ids);

    /**
     * @return a page of events (never {@code null}; empty page when nothing matches).
     */
    FacadePage<FacadeEventBO> listByPage(FacadeEventQuery query);

}
