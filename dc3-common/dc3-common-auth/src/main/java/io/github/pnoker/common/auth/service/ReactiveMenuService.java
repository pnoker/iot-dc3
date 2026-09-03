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
package io.github.pnoker.common.auth.service;

import io.github.pnoker.common.auth.entity.bo.MenuBO;
import io.github.pnoker.common.auth.entity.bo.MenuTreeBO;
import io.github.pnoker.common.auth.repository.MenuFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
/** Business service covering menu use cases. */

public interface ReactiveMenuService {
    /** Resolve the menu by its id. */
    Mono<MenuBO> getById(Long id);

    /** Page menus matching the tenant-scoped filters. */
    Mono<OffsetPage<MenuBO>> list(MenuFilter filter);

    /** Emit the menu tree for the tenant. */
    Flux<MenuTreeBO> listTree(MenuFilter filter);

    /** Add one menu. */
    Mono<MenuBO> add(MenuBO menu);

    /** Update one menu and emit the updated row. */
    Mono<MenuBO> update(MenuBO menu);

    /** Delete the menu. */
    Mono<Void> delete(Long id, Long operatorId, String operatorName);
}
