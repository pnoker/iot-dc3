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

import io.github.pnoker.common.auth.entity.bo.ServiceAccountBO;
import io.github.pnoker.common.auth.entity.model.ServiceAccountDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Non-blocking persistence port for service-account aggregates. */
public interface ReactiveServiceAccountStore {
    /** Resolve the service account by its id. */
    Mono<ServiceAccountDO> getById(Long tenantId, Long id);

    /** Page service accounts matching the tenant-scoped filters. */
    Mono<OffsetPage<ServiceAccountDO>> list(ServiceAccountFilter filter);

    /** Insert one service account and emit the stored row. */
    Mono<ServiceAccountDO> insert(ServiceAccountBO account);

    /** Update one service account and emit the updated row. */
    Mono<ServiceAccountDO> update(Long tenantId, ServiceAccountBO account);

    /** Delete the service account, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName);
}
