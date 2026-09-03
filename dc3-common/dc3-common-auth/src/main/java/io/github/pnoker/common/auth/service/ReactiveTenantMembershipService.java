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

import io.github.pnoker.common.auth.entity.bo.TenantMembershipBO;
import io.github.pnoker.common.auth.repository.TenantMembershipFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive tenant-membership security boundary. */
public interface ReactiveTenantMembershipService {
    /** Resolve the tenant membership by its id. */
    Mono<TenantMembershipBO> getById(Long tenantId, Long id);

    /** Resolve the tenant membership by its tenant and principal. */
    Mono<TenantMembershipBO> getByTenantAndPrincipal(Long tenantId, Long principalId);

    /** Stream principal ids matching the request. */
    Flux<Long> listPrincipalIds(Long tenantId);

    /** Page tenant memberships matching the tenant-scoped filters. */
    Mono<OffsetPage<TenantMembershipBO>> list(TenantMembershipFilter filter);

    /** Report whether the principal belongs to the tenant. */
    Mono<Boolean> isTenantMember(Long tenantId, Long principalId);

    /** Emit the membership when the principal belongs to the tenant, failing otherwise. */
    Mono<TenantMembershipBO> requireTenantMember(Long tenantId, Long principalId);
}
