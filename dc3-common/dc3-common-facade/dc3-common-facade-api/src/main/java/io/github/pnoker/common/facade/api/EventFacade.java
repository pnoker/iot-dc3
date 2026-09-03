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
import io.github.pnoker.common.facade.entity.query.FacadeEventOffsetQuery;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.Collection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Protocol-neutral event facade. Single-record and bulk lookups are tenant-scoped.
 *
 * @author pnoker
 * @since 2016.10.1
 */
public interface EventFacade {

    /** Resolve the event by its id. */
    Mono<FacadeEventBO> getById(Long tenantId, Long id);

    /** List events matched by ids. */
    Flux<FacadeEventBO> listByIds(Long tenantId, Collection<Long> ids);

    /** Page events matching the tenant-scoped filters. */
    Mono<OffsetPage<FacadeEventBO>> list(FacadeEventOffsetQuery query);
}
