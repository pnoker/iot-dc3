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

import io.github.pnoker.common.auth.entity.bo.TenantBO;
import io.github.pnoker.common.auth.entity.builder.TenantBuilder;
import io.github.pnoker.common.auth.entity.model.TenantDO;
import io.github.pnoker.common.auth.repository.ReactiveTenantStore;
import io.github.pnoker.common.auth.repository.TenantFilter;
import io.github.pnoker.common.auth.service.ReactiveTenantService;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.utils.CodeUtil;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Default non-blocking tenant application service. */
@Service
@RequiredArgsConstructor
public class ReactiveTenantServiceImpl implements ReactiveTenantService {

    private final ReactiveTenantStore tenantStore;
    private final TenantBuilder tenantBuilder;

    @Override
    public Mono<TenantBO> getById(Long id) {
        if (id == null || id <= 0) return Mono.error(new RequestException("Tenant ID is required"));
        return tenantStore
                .getById(id)
                .map(tenantBuilder::buildBOByDO)
                .switchIfEmpty(Mono.error(new NotFoundException("Tenant")));
    }

    @Override
    public Mono<TenantBO> getByCode(String code) {
        if (code == null || code.isBlank()) return Mono.error(new RequestException("Tenant code is required"));
        return tenantStore.getEnabledByCode(code.trim()).map(tenantBuilder::buildBOByDO);
    }

    @Override
    public Mono<OffsetPage<TenantBO>> list(TenantFilter filter) {
        return tenantStore
                .list(filter)
                .map(page -> OffsetPage.of(
                        page.items().stream().map(tenantBuilder::buildBOByDO).toList(),
                        page.offset(),
                        page.limit(),
                        page.total()));
    }

    @Override
    public Mono<TenantBO> add(TenantBO tenant) {
        return Mono.defer(() -> {
            validate(tenant, false);
            if (tenant.getTenantCode() == null || tenant.getTenantCode().isBlank()) {
                tenant.setTenantCode(CodeUtil.getCode());
            }
            return tenantStore
                    .getByNameAndCode(tenant.getTenantName(), tenant.getTenantCode())
                    .flatMap(existing -> Mono.<TenantBO>error(new DuplicateException("Tenant has been duplicated")))
                    .switchIfEmpty(Mono.defer(() -> tenantStore
                            .insert(tenantBuilder.buildDOByBO(tenant))
                            .map(tenantBuilder::buildBOByDO)))
                    .onErrorMap(
                            DataIntegrityViolationException.class,
                            error -> new DuplicateException("Tenant code is already in use"));
        });
    }

    @Override
    public Mono<TenantBO> update(TenantBO tenant) {
        return Mono.defer(() -> {
            validate(tenant, true);
            return tenantStore
                    .getById(tenant.getId())
                    .switchIfEmpty(Mono.error(new NotFoundException("Tenant")))
                    .flatMap(current -> tenantStore
                            .getByNameAndCode(tenant.getTenantName(), tenant.getTenantCode())
                            .filter(existing -> !existing.getId().equals(tenant.getId()))
                            .flatMap(existing ->
                                    Mono.<TenantDO>error(new DuplicateException("Tenant has been duplicated")))
                            .switchIfEmpty(Mono.defer(() -> tenantStore.update(tenantBuilder.buildDOByBO(tenant))))
                            .map(tenantBuilder::buildBOByDO))
                    .switchIfEmpty(Mono.error(new RequestException("Tenant update failed")))
                    .onErrorMap(
                            DataIntegrityViolationException.class,
                            error -> new DuplicateException("Tenant code is already in use"));
        });
    }

    @Override
    public Mono<Boolean> delete(Long id, Long operatorId, String operatorName) {
        return getById(id)
                .then(tenantStore
                        .delete(id, operatorId, operatorName)
                        .filter(Boolean.TRUE::equals)
                        .switchIfEmpty(Mono.error(new RequestException("Tenant was already deleted"))));
    }

    private void validate(TenantBO tenant, boolean update) {
        if (tenant == null
                || (update && (tenant.getId() == null || tenant.getId() <= 0))
                || tenant.getTenantName() == null
                || tenant.getTenantName().isBlank()
                || tenant.getTenantCode() == null
                || tenant.getTenantCode().isBlank()) {
            throw new RequestException("Tenant name and code are required");
        }
        tenant.setTenantName(tenant.getTenantName().trim());
        tenant.setTenantCode(tenant.getTenantCode().trim());
    }
}
