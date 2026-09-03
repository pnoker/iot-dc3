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

import io.github.pnoker.common.manager.entity.bo.GroupBO;
import io.github.pnoker.common.manager.repository.GroupFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Reactive application service for groups. */
public interface ReactiveGroupService {
    /** Add one group. */
    Mono<GroupBO> add(GroupBO group);

    /** Update one group and emit the updated row. */
    Mono<GroupBO> update(GroupBO group);

    /** Delete the group, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName);

    /** Resolve the group by its id. */
    Mono<GroupBO> getById(Long tenantId, Long id);

    /** Page groups matching the tenant-scoped filters. */
    Mono<OffsetPage<GroupBO>> list(GroupFilter filter);
}
