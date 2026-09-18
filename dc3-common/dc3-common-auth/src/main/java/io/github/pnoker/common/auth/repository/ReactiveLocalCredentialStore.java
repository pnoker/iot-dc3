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

import io.github.pnoker.common.auth.entity.model.LocalCredentialDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Non-blocking persistence port for tenant-scoped local credentials. */
public interface ReactiveLocalCredentialStore {
    /** Resolve the local credential by its id. */
    Mono<LocalCredentialDO> getById(Long tenantId, Long id);

    /** Resolve the local credential by its login name. */
    Mono<LocalCredentialDO> getByLoginName(Long tenantId, String loginNameNormalized);

    /** Check whether a record exists for the given login name. */
    Mono<Boolean> existsByLoginName(Long tenantId, String loginNameNormalized);

    /** Page local credentials matching the tenant-scoped filters. */
    Mono<OffsetPage<LocalCredentialDO>> list(LocalCredentialFilter filter);
}
