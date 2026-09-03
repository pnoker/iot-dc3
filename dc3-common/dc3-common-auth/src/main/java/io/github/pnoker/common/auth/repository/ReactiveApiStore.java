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

import io.github.pnoker.common.auth.entity.bo.ApiBO;
import io.github.pnoker.common.auth.entity.model.ApiDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;
/** Reactive persistence port for api records. */

public interface ReactiveApiStore {
    /** Resolve the api by its id. */
    Mono<ApiDO> getById(Long id);

    /** Page apis matching the tenant-scoped filters. */
    Mono<OffsetPage<ApiDO>> list(ApiFilter filter);

    /** Insert one api and emit the stored row. */
    Mono<ApiDO> insert(ApiBO api);

    /** Update one api and emit the updated row. */
    Mono<ApiDO> update(ApiBO api);

    /** Delete the api, reporting whether a row was removed. */
    Mono<Boolean> delete(Long id, Long operatorId, String operatorName);

    /** Check whether a duplicate row already exists. */
    Mono<Boolean> existsDuplicate(ApiBO api);
}
