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

import io.github.pnoker.common.auth.entity.bo.ApiBO;
import io.github.pnoker.common.auth.repository.ApiFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;
/** Business service covering api use cases. */

public interface ReactiveApiService {
    /** Resolve the api by its id. */
    Mono<ApiBO> getById(Long id);

    /** Page apis matching the tenant-scoped filters. */
    Mono<OffsetPage<ApiBO>> list(ApiFilter filter);

    /** Add one api. */
    Mono<ApiBO> add(ApiBO api);

    /** Update one api and emit the updated row. */
    Mono<ApiBO> update(ApiBO api);

    /** Delete the api. */
    Mono<Void> delete(Long id, Long operatorId, String operatorName);
}
