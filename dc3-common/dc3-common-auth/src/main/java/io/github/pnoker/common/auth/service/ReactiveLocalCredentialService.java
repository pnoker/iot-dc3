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

import io.github.pnoker.common.auth.entity.bo.LocalCredentialBO;
import io.github.pnoker.common.auth.repository.LocalCredentialFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Non-blocking local credential queries and authentication primitives. */
public interface ReactiveLocalCredentialService {
    /** Resolve the local credential by its id. */
    Mono<LocalCredentialBO> getById(Long tenantId, Long id);

    /** Resolve the local credential by its login name. */
    Mono<LocalCredentialBO> getByLoginName(Long tenantId, String loginName);

    /** Page local credentials matching the tenant-scoped filters. */
    Mono<OffsetPage<LocalCredentialBO>> list(LocalCredentialFilter filter);

    /** Report whether the login name is available in the tenant. */
    Mono<Boolean> isLoginNameAvailable(Long tenantId, String loginName);

    /** Verify the raw password against the credential's hash. */
    Mono<Boolean> verifyPassword(LocalCredentialBO credential, String rawPassword);
}
