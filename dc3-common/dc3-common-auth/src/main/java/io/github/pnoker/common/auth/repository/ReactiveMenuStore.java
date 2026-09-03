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
package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.bo.MenuBO;
import io.github.pnoker.common.auth.entity.model.MenuDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
/** Reactive persistence port for menu records. */

public interface ReactiveMenuStore {
    /** Resolve the menu by its id. */
    Mono<MenuDO> getById(Long id);

    /** Page menus matching the tenant-scoped filters. */
    Mono<OffsetPage<MenuDO>> list(MenuFilter filter);

    /** Emit the menu tree for the tenant. */
    Flux<MenuDO> listTree(MenuFilter filter);

    /** Insert one menu and emit the stored row. */
    Mono<MenuDO> insert(MenuBO menu);

    /** Update one menu and emit the updated row. */
    Mono<MenuDO> update(MenuBO menu);

    /** Delete the menu, reporting whether a row was removed. */
    Mono<Boolean> delete(Long id, Long operatorId, String operatorName);

    /** Check whether a duplicate row already exists. */
    Mono<Boolean> existsDuplicate(MenuBO menu);

    /** Report whether the menu has children. */
    Mono<Boolean> hasChildren(Long id);

    /** Report whether the candidate descends from the root. */
    Mono<Boolean> isDescendant(Long rootId, Long candidateId);
}
