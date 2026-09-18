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

import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.bo.RoleTreeBO;
import io.github.pnoker.common.auth.entity.builder.RoleBuilder;
import io.github.pnoker.common.auth.repository.ReactiveRoleStore;
import io.github.pnoker.common.auth.repository.RoleFilter;
import io.github.pnoker.common.auth.security.PermissionCacheInvalidator;
import io.github.pnoker.common.auth.service.ReactiveRoleService;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Default role service implementation. */
@Service
@RequiredArgsConstructor
public class ReactiveRoleServiceImpl implements ReactiveRoleService {
    private final ReactiveRoleStore store;
    private final RoleBuilder builder;
    private PermissionCacheInvalidator permissionCacheInvalidator;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setPermissionCacheInvalidator(PermissionCacheInvalidator invalidator) {
        this.permissionCacheInvalidator = invalidator;
    }

    @Override
    public Mono<RoleBO> getById(Long tenantId, Long id) {
        if (!valid(tenantId, id)) return Mono.error(new RequestException("Role ID is required"));
        return store.getById(tenantId, id)
                .map(builder::buildBOByDO)
                .switchIfEmpty(Mono.error(new NotFoundException("Role")));
    }

    @Override
    public Mono<OffsetPage<RoleBO>> list(RoleFilter filter) {
        return store.list(filter)
                .map(p -> OffsetPage.of(
                        p.items().stream().map(builder::buildBOByDO).toList(), p.offset(), p.limit(), p.total()));
    }

    @Override
    public Flux<RoleTreeBO> listTree(RoleFilter filter) {
        return store.listTree(filter)
                .map(builder::buildBOByDO)
                .map(RoleTreeBO::fromBO)
                .collectList()
                .flatMapMany(this::tree);
    }

    private Flux<RoleTreeBO> tree(List<RoleTreeBO> rows) {
        Map<String, RoleTreeBO> byId = new HashMap<>();
        rows.forEach(r -> byId.put(String.valueOf(r.getId()), r));
        List<RoleTreeBO> roots = new ArrayList<>();
        rows.forEach(r -> {
            RoleTreeBO p = byId.get(r.getParentRoleId());
            if (p == null || "0".equals(r.getParentRoleId())) roots.add(r);
            else p.addChild(r);
        });
        Comparator<RoleTreeBO> c =
                Comparator.comparing(RoleTreeBO::getRoleName, Comparator.nullsLast(String::compareTo));
        sort(roots, c);
        return Flux.fromIterable(roots);
    }

    private void sort(List<RoleTreeBO> n, Comparator<RoleTreeBO> c) {
        n.sort(c);
        n.forEach(x -> sort(x.getChildren(), c));
    }

    @Override
    public Mono<RoleBO> add(RoleBO role) {
        if (role == null || role.getTenantId() == null || role.getTenantId() <= 0)
            return Mono.error(new RequestException("Tenant ID is required"));
        return requireParent(role.getTenantId(), role.getParentRoleId())
                .then(store.insert(role))
                .doOnSuccess(saved -> invalidateTenant(role.getTenantId()))
                .map(builder::buildBOByDO);
    }

    @Override
    public Mono<RoleBO> update(Long tenantId, RoleBO role) {
        if (role == null || !valid(tenantId, role.getId()))
            return Mono.error(new RequestException("Role update is invalid"));
        role.setTenantId(tenantId);
        if (String.valueOf(role.getId()).equals(role.getParentRoleId()))
            return Mono.error(new RequestException("Role cannot be its own parent"));
        return requireParent(tenantId, role.getParentRoleId())
                .then(store.update(tenantId, role))
                .doOnSuccess(saved -> invalidateTenant(tenantId))
                .map(builder::buildBOByDO)
                .switchIfEmpty(Mono.error(new NotFoundException("Role")));
    }

    @Override
    public Mono<Void> delete(Long tenantId, Long id, Long operatorId, String operatorName) {
        if (!valid(tenantId, id)) return Mono.error(new RequestException("Role ID is required"));
        return store.delete(tenantId, id, operatorId, operatorName)
                .flatMap(ok -> Boolean.TRUE.equals(ok) ? Mono.<Void>empty() : Mono.error(new NotFoundException("Role")))
                .doOnSuccess(ignored -> invalidateTenant(tenantId));
    }

    private boolean valid(Long t, Long id) {
        return t != null && t > 0 && id != null && id > 0;
    }

    private Mono<Void> requireParent(Long tenantId, String parentId) {
        if (parentId == null || parentId.isBlank() || "0".equals(parentId)) return Mono.empty();
        try {
            long id = Long.parseLong(parentId);
            if (id <= 0) return Mono.error(new RequestException("Parent role ID is invalid"));
            return store.getById(tenantId, id)
                    .switchIfEmpty(Mono.error(new NotFoundException("Parent role")))
                    .then();
        } catch (NumberFormatException e) {
            return Mono.error(new RequestException("Parent role ID is invalid"));
        }
    }

    private void invalidateTenant(Long tenantId) {
        if (permissionCacheInvalidator != null) permissionCacheInvalidator.invalidateTenant(tenantId);
    }
}
