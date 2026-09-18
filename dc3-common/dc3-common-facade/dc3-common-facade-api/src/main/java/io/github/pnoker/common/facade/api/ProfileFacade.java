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

import io.github.pnoker.common.facade.entity.bo.FacadeProfileBO;
import io.github.pnoker.common.facade.entity.query.FacadeProfileOffsetQuery;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.Collection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Protocol-neutral profile/template facade. Single-record and bulk lookups are
 * tenant-scoped.
 *
 * @author pnoker
 * @since 2026.5.14
 */
public interface ProfileFacade {

    /** Resolve the profile by its id. */
    Mono<FacadeProfileBO> getByIdReactive(Long tenantId, Long id);

    /** List profiles matched by ids. */
    Flux<FacadeProfileBO> listByIdsReactive(Long tenantId, Collection<Long> ids);

    /** List profiles matched by device id. */
    Flux<FacadeProfileBO> listByDeviceIdReactive(Long tenantId, Long deviceId);

    /** Page profiles matching the tenant-scoped filters. */
    Mono<OffsetPage<FacadeProfileBO>> listReactive(FacadeProfileOffsetQuery query);
}
