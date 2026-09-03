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
package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.GroupBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Reactive persistence port for tenant-scoped groups. */
public interface ReactiveGroupStore {
    /** Page groups matching the tenant-scoped filters. */
    Mono<OffsetPage<GroupBO>> list(GroupFilter filter);

    /** Load the group scoped to the tenant by id. */
    Mono<GroupBO> get(Long tenantId, Long id);

    /** Resolve the group by its name. */
    Mono<GroupBO> getByName(Long tenantId, byte type, Long parentId, String name);

    /** Report whether the group has children. */
    Mono<Boolean> hasChildren(Long tenantId, Long id);

    /** Report whether the group has active bindings. */
    Mono<Boolean> hasActiveBindings(Long tenantId, Long id);

    /** Insert one group and emit the stored row. */
    Mono<GroupBO> insert(GroupBO group);

    /** Update one group and emit the updated row. */
    Mono<GroupBO> update(GroupBO group);

    /** Delete the group, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName);
}
