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

import io.github.pnoker.common.auth.entity.bo.ResourceBO;
import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.bo.RoleResourceBindBO;
import io.github.pnoker.common.auth.entity.builder.ResourceBuilder;
import io.github.pnoker.common.auth.entity.builder.RoleResourceBindBuilder;
import io.github.pnoker.common.auth.repository.ReactiveResourceLookupStore;
import io.github.pnoker.common.auth.repository.ReactiveRoleResourceBindStore;
import io.github.pnoker.common.auth.repository.RoleResourceBindFilter;
import io.github.pnoker.common.auth.security.AuthPermissionProvider;
import io.github.pnoker.common.auth.security.PermissionCacheInvalidator;
import io.github.pnoker.common.auth.service.ReactiveRoleResourceBindService;
import io.github.pnoker.common.auth.service.ReactiveRoleService;
import io.github.pnoker.common.auth.service.ReactiveTenantMembershipService;
import io.github.pnoker.common.exception.AccessDeniedException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.security.PermissionMethods;
import io.github.pnoker.db.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Default non-blocking role-resource assignment service. */
@Service
@RequiredArgsConstructor
public class ReactiveRoleResourceBindServiceImpl implements ReactiveRoleResourceBindService {
    private final ReactiveRoleResourceBindStore store;
    private final ReactiveResourceLookupStore resourceStore;
    private final RoleResourceBindBuilder bindingBuilder;
    private final ResourceBuilder resourceBuilder;
    private final ReactiveRoleService roleService;
    private final ReactiveTenantMembershipService membershipService;
    private final AuthPermissionProvider permissionProvider;
    private PermissionCacheInvalidator permissionCacheInvalidator;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setPermissionCacheInvalidator(PermissionCacheInvalidator invalidator) {
        this.permissionCacheInvalidator = invalidator;
    }

    @Override
    public Mono<RoleResourceBindBO> add(RoleResourceBindBO binding, Long tenantId, Long callerPrincipalId) {
        if (binding == null || !valid(tenantId) || !valid(binding.getRoleId()) || !valid(binding.getResourceId())) {
            return Mono.error(new RequestException("Role resource binding identifiers are required"));
        }
        return roleService
                .getById(tenantId, binding.getRoleId())
                .then(resourceStore
                        .listEnabledByIds(java.util.List.of(binding.getResourceId()))
                        .hasElements()
                        .flatMap(enabled ->
                                enabled ? Mono.<Void>empty() : Mono.error(new NotFoundException("Resource"))))
                .then(Mono.defer(() -> ensureGrantable(tenantId, callerPrincipalId, binding.getResourceId())))
                .then(Mono.defer(() -> store.exists(tenantId, binding.getRoleId(), binding.getResourceId())))
                .flatMap(duplicate -> Boolean.TRUE.equals(duplicate)
                        ? Mono.error(new DuplicateException("Role resource bind has been duplicated"))
                        : store.insert(binding))
                .doOnSuccess(saved -> invalidateTenant(tenantId))
                .map(bindingBuilder::buildBOByDO);
    }

    /**
     * No-privilege-escalation guard: a resource may only be granted to a role by a
     * caller who already holds that resource code or the wildcard authority.
     * Without this, {@code role_resource_bind:add} alone lets a caller attach the
     * wildcard resource to a role they hold — self-elevation to tenant superuser.
     *
     * @param tenantId tenant scope of the binding
     * @param callerPrincipalId principal invoking the grant
     * @param resourceId resource being granted
     * @return empty when the grant is allowed
     */
    private Mono<Void> ensureGrantable(Long tenantId, Long callerPrincipalId, Long resourceId) {
        if (!valid(callerPrincipalId)) {
            return Mono.error(new AccessDeniedException("Role resource binding requires an authenticated caller"));
        }
        return permissionProvider
                .listPermissionCodes(tenantId, callerPrincipalId)
                .flatMap(
                        codes -> codes.contains(PermissionMethods.WILDCARD)
                                ? Mono.<Void>empty()
                                : resourceStore
                                        .listEnabledByIds(java.util.List.of(resourceId))
                                        .next()
                                        .map(resource -> resource.getResourceCode())
                                        .flatMap(
                                                code -> codes.contains(code)
                                                        ? Mono.<Void>empty()
                                                        : Mono.error(
                                                                new AccessDeniedException(
                                                                        "Resources can only be granted by callers who hold them or hold the wildcard permission"))));
    }

    @Override
    public Mono<Void> delete(Long tenantId, Long id, Long operatorId, String operatorName) {
        if (!valid(tenantId) || !valid(id))
            return Mono.error(new RequestException("Role resource binding ID is required"));
        return store.delete(tenantId, id, operatorId, operatorName)
                .flatMap(deleted -> Boolean.TRUE.equals(deleted)
                        ? Mono.<Void>empty()
                        : Mono.error(new NotFoundException("Role resource bind")))
                .doOnSuccess(ignored -> invalidateTenant(tenantId));
    }

    @Override
    public Mono<OffsetPage<RoleResourceBindBO>> list(RoleResourceBindFilter filter) {
        return store.list(filter)
                .map(page -> OffsetPage.of(
                        page.items().stream().map(bindingBuilder::buildBOByDO).toList(),
                        page.offset(),
                        page.limit(),
                        page.total()));
    }

    @Override
    public Flux<ResourceBO> listResourcesByRole(Long tenantId, Long roleId) {
        return roleService
                .getById(tenantId, roleId)
                .thenMany(store.listResourceIds(tenantId, roleId))
                .distinct()
                .collectList()
                .flatMapMany(resourceStore::listEnabledByIds)
                .map(resourceBuilder::buildBOByDO);
    }

    @Override
    public Flux<ResourceBO> listResourcesByPrincipal(Long tenantId, Long principalId) {
        return membershipService
                .requireTenantMember(tenantId, principalId)
                .thenMany(store.listResourceIdsByPrincipal(tenantId, principalId))
                .distinct()
                .collectList()
                .flatMapMany(resourceStore::listEnabledByIds)
                .map(resourceBuilder::buildBOByDO);
    }

    @Override
    public Flux<RoleBO> listRolesByResource(Long tenantId, Long resourceId) {
        if (!valid(tenantId) || !valid(resourceId))
            return Flux.error(new RequestException("Tenant and resource IDs are required"));
        return resourceStore
                .listEnabledByIds(java.util.List.of(resourceId))
                .hasElements()
                .flatMapMany(enabled -> enabled
                        ? store.listRoleIdsByResource(tenantId, resourceId)
                                .distinct()
                                .flatMap(id -> roleService.getById(tenantId, id))
                        : Flux.error(new NotFoundException("Resource")));
    }

    private boolean valid(Long value) {
        return value != null && value > 0;
    }

    private void invalidateTenant(Long tenantId) {
        if (permissionCacheInvalidator != null) permissionCacheInvalidator.invalidateTenant(tenantId);
    }
}
