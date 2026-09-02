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
package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.TenantMembershipBO;
import io.github.pnoker.common.auth.entity.builder.TenantMembershipBuilder;
import io.github.pnoker.common.auth.repository.ReactiveTenantMembershipCommandStore;
import io.github.pnoker.common.auth.service.ReactiveTenantMembershipCommandService;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Default non-blocking tenant membership command service. */
@Service
@RequiredArgsConstructor
public class ReactiveTenantMembershipCommandServiceImpl implements ReactiveTenantMembershipCommandService {

    private final ReactiveTenantMembershipCommandStore store;
    private final TenantMembershipBuilder builder;

    @Override
    public Mono<TenantMembershipBO> add(TenantMembershipBO membership) {
        return Mono.defer(() -> {
            if (membership == null
                    || membership.getTenantId() == null
                    || membership.getTenantId() <= 0
                    || membership.getPrincipalId() == null
                    || membership.getPrincipalId() <= 0) {
                return Mono.error(new RequestException("Tenant and principal are required"));
            }
            return store.insert(builder.buildDOByBO(membership))
                    .map(builder::buildBOByDO)
                    .switchIfEmpty(Mono.error(new RequestException("Failed to create tenant membership")))
                    .onErrorMap(
                            DataIntegrityViolationException.class,
                            error -> new DuplicateException("Tenant membership already exists"));
        });
    }

    @Override
    public Mono<Void> delete(Long tenantId, Long id, Long operatorId, String operatorName) {
        if (tenantId == null || tenantId <= 0 || id == null || id <= 0) {
            return Mono.error(new RequestException("Tenant membership IDs are required"));
        }
        return store.delete(tenantId, id, operatorId, operatorName)
                .flatMap(deleted -> Boolean.TRUE.equals(deleted)
                        ? Mono.<Void>empty()
                        : Mono.error(new NotFoundException("Tenant membership does not exist")));
    }
}
