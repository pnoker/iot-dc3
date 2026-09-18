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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.auth.entity.bo.RolePrincipalBindBO;
import io.github.pnoker.common.auth.entity.bo.TenantMembershipBO;
import io.github.pnoker.common.auth.entity.builder.RolePrincipalBindBuilder;
import io.github.pnoker.common.auth.entity.model.RolePrincipalBindDO;
import io.github.pnoker.common.auth.repository.ReactiveRolePrincipalBindStore;
import io.github.pnoker.common.auth.security.AuthPermissionProvider;
import io.github.pnoker.common.auth.service.ReactiveRoleService;
import io.github.pnoker.common.auth.service.ReactiveTenantMembershipService;
import io.github.pnoker.common.auth.service.ReactiveUserService;
import io.github.pnoker.common.enums.PrincipalTypeEnum;
import io.github.pnoker.common.exception.AccessDeniedException;
import io.github.pnoker.common.exception.DuplicateException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReactiveRolePrincipalBindServiceImplTest {
    private static final Long CALLER = 99L;

    @Mock
    ReactiveRolePrincipalBindStore store;

    @Mock
    RolePrincipalBindBuilder builder;

    @Mock
    ReactiveRoleService roleService;

    @Mock
    ReactiveTenantMembershipService membershipService;

    @Mock
    ReactiveUserService userService;

    @Mock
    AuthPermissionProvider permissionProvider;

    @Test
    void addRejectsPrincipalTypeSpoofing() {
        RolePrincipalBindBO binding = binding();
        binding.setPrincipalType(PrincipalTypeEnum.SERVICE_ACCOUNT);
        TenantMembershipBO membership = new TenantMembershipBO();
        membership.setPrincipalType(PrincipalTypeEnum.USER);
        when(roleService.getById(7L, 11L)).thenReturn(Mono.just(new io.github.pnoker.common.auth.entity.bo.RoleBO()));
        when(membershipService.requireTenantMember(7L, 13L)).thenReturn(Mono.just(membership));

        StepVerifier.create(service().add(binding, CALLER))
                .expectErrorMessage("Principal type does not match tenant membership")
                .verify();
    }

    @Test
    void addRejectsDuplicateBindingBeforeInsert() {
        RolePrincipalBindBO binding = binding();
        TenantMembershipBO membership = new TenantMembershipBO();
        membership.setPrincipalType(PrincipalTypeEnum.USER);
        when(roleService.getById(7L, 11L)).thenReturn(Mono.just(new io.github.pnoker.common.auth.entity.bo.RoleBO()));
        when(membershipService.requireTenantMember(7L, 13L)).thenReturn(Mono.just(membership));
        when(permissionProvider.listPermissionCodes(7L, CALLER)).thenReturn(Mono.just(Set.of("*")));
        when(store.exists(7L, 11L, 13L, null)).thenReturn(Mono.just(true));

        StepVerifier.create(service().add(binding, CALLER))
                .expectError(DuplicateException.class)
                .verify();
    }

    @Test
    void addRejectsRoleTheCallerDoesNotHold() {
        RolePrincipalBindBO binding = binding();
        TenantMembershipBO membership = new TenantMembershipBO();
        membership.setPrincipalType(PrincipalTypeEnum.USER);
        when(roleService.getById(7L, 11L)).thenReturn(Mono.just(new io.github.pnoker.common.auth.entity.bo.RoleBO()));
        when(membershipService.requireTenantMember(7L, 13L)).thenReturn(Mono.just(membership));
        when(permissionProvider.listPermissionCodes(7L, CALLER))
                .thenReturn(Mono.just(Set.of("dc3-center-auth:role:list")));
        when(store.exists(7L, 11L, CALLER, null)).thenReturn(Mono.just(false));

        StepVerifier.create(service().add(binding, CALLER))
                .expectError(AccessDeniedException.class)
                .verify();
    }

    @Test
    void addAllowsGrantWhenCallerHoldsTheRole() {
        RolePrincipalBindBO binding = binding();
        TenantMembershipBO membership = new TenantMembershipBO();
        membership.setPrincipalType(PrincipalTypeEnum.USER);
        when(roleService.getById(7L, 11L)).thenReturn(Mono.just(new io.github.pnoker.common.auth.entity.bo.RoleBO()));
        when(membershipService.requireTenantMember(7L, 13L)).thenReturn(Mono.just(membership));
        when(permissionProvider.listPermissionCodes(7L, CALLER))
                .thenReturn(Mono.just(Set.of("dc3-center-auth:role:list")));
        when(store.exists(7L, 11L, CALLER, null)).thenReturn(Mono.just(true));
        when(store.exists(7L, 11L, 13L, null)).thenReturn(Mono.just(false));
        when(store.insert(binding)).thenReturn(Mono.just(new RolePrincipalBindDO()));
        when(builder.buildBOByDO(any(RolePrincipalBindDO.class))).thenReturn(new RolePrincipalBindBO());

        StepVerifier.create(service().add(binding, CALLER)).expectNextCount(1).verifyComplete();
    }

    private RolePrincipalBindBO binding() {
        RolePrincipalBindBO binding = new RolePrincipalBindBO();
        binding.setTenantId(7L);
        binding.setRoleId(11L);
        binding.setPrincipalId(13L);
        binding.setPrincipalType(PrincipalTypeEnum.USER);
        return binding;
    }

    private ReactiveRolePrincipalBindServiceImpl service() {
        return new ReactiveRolePrincipalBindServiceImpl(
                store, builder, roleService, membershipService, userService, permissionProvider);
    }
}
