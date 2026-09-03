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

import io.github.pnoker.common.auth.entity.model.PrincipalDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.Collection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Persistence port for the platform principal catalog. */
public interface ReactivePrincipalStore {

    /** Resolve the principal by its id. */
    Mono<PrincipalDO> getById(Long tenantId, Long id);

    /** Page principals matching the tenant-scoped filters. */
    Mono<OffsetPage<PrincipalDO>> list(Long tenantId, PrincipalFilter filter);

    /** List principals matched by ids. */
    Flux<PrincipalDO> listByIds(Long tenantId, Collection<Long> ids);

    /** Update one enable flag and emit the updated row. */
    Mono<PrincipalDO> updateEnableFlag(Long tenantId, Long id, byte enableFlag, Long operatorId, String operatorName);

    /** Stamp the principal's last-login time, reporting whether it exists. */
    Mono<Boolean> touchLastLogin(Long id);
}
