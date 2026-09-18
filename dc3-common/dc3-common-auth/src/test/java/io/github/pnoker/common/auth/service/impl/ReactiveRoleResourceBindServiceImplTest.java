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

import io.github.pnoker.common.auth.entity.bo.RoleResourceBindBO;
import io.github.pnoker.common.auth.entity.builder.ResourceBuilder;
import io.github.pnoker.common.auth.entity.builder.RoleResourceBindBuilder;
import io.github.pnoker.common.auth.entity.model.ResourceDO;
import io.github.pnoker.common.auth.repository.ReactiveResourceLookupStore;
import io.github.pnoker.common.auth.repository.ReactiveRoleResourceBindStore;
import io.github.pnoker.common.auth.security.AuthPermissionProvider;
import io.github.pnoker.common.auth.service.ReactiveRoleService;
import io.github.pnoker.common.auth.service.ReactiveTenantMembershipService;
import io.github.pnoker.common.exception.AccessDeniedException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReactiveRoleResourceBindServiceImplTest {
    private static final Long CALLER = 99L;

    @Mock
    ReactiveRoleResourceBindStore store;

    @Mock
    ReactiveResourceLookupStore resourceStore;

    @Mock
    RoleResourceBindBuilder bindingBuilder;

    @Mock
    ResourceBuilder resourceBuilder;

    @Mock
    ReactiveRoleService roleService;

    @Mock
    ReactiveTenantMembershipService membershipService;

    @Mock
    AuthPermissionProvider permissionProvider;

    @Test
    void addRejectsUnknownResource() {
        RoleResourceBindBO binding = binding();
        when(roleService.getById(7L, 11L)).thenReturn(Mono.just(new io.github.pnoker.common.auth.entity.bo.RoleBO()));
        when(resourceStore.listEnabledByIds(List.of(13L))).thenReturn(Flux.empty());
        StepVerifier.create(service().add(binding, 7L, CALLER))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void addRejectsDuplicateBinding() {
        RoleResourceBindBO binding = binding();
        when(roleService.getById(7L, 11L)).thenReturn(Mono.just(new io.github.pnoker.common.auth.entity.bo.RoleBO()));
        when(resourceStore.listEnabledByIds(List.of(13L))).thenReturn(Flux.just(resource("*")));
        when(permissionProvider.listPermissionCodes(7L, CALLER)).thenReturn(Mono.just(Set.of("*")));
        when(store.exists(7L, 11L, 13L)).thenReturn(Mono.just(true));
        StepVerifier.create(service().add(binding, 7L, CALLER))
                .expectError(DuplicateException.class)
                .verify();
    }

    @Test
    void addRejectsResourceTheCallerDoesNotHold() {
        RoleResourceBindBO binding = binding();
        when(roleService.getById(7L, 11L)).thenReturn(Mono.just(new io.github.pnoker.common.auth.entity.bo.RoleBO()));
        when(resourceStore.listEnabledByIds(List.of(13L))).thenReturn(Flux.just(resource("dc3-center-auth:user:add")));
        when(permissionProvider.listPermissionCodes(7L, CALLER))
                .thenReturn(Mono.just(Set.of("dc3-center-auth:role:list")));
        StepVerifier.create(service().add(binding, 7L, CALLER))
                .expectError(AccessDeniedException.class)
                .verify();
    }

    @Test
    void addAllowsGrantWhenCallerHoldsTheResourceCode() {
        RoleResourceBindBO binding = binding();
        when(roleService.getById(7L, 11L)).thenReturn(Mono.just(new io.github.pnoker.common.auth.entity.bo.RoleBO()));
        when(resourceStore.listEnabledByIds(List.of(13L))).thenReturn(Flux.just(resource("dc3-center-auth:role:list")));
        when(permissionProvider.listPermissionCodes(7L, CALLER))
                .thenReturn(Mono.just(Set.of("dc3-center-auth:role:list")));
        when(store.exists(7L, 11L, 13L)).thenReturn(Mono.just(false));
        when(store.insert(binding))
                .thenReturn(Mono.just(new io.github.pnoker.common.auth.entity.model.RoleResourceBindDO()));
        when(bindingBuilder.buildBOByDO(any())).thenReturn(new RoleResourceBindBO());
        StepVerifier.create(service().add(binding, 7L, CALLER))
                .expectNextCount(1)
                .verifyComplete();
    }

    private ResourceDO resource(String code) {
        ResourceDO resource = new ResourceDO();
        resource.setResourceCode(code);
        return resource;
    }

    private RoleResourceBindBO binding() {
        RoleResourceBindBO binding = new RoleResourceBindBO();
        binding.setRoleId(11L);
        binding.setResourceId(13L);
        return binding;
    }

    private ReactiveRoleResourceBindServiceImpl service() {
        return new ReactiveRoleResourceBindServiceImpl(
                store,
                resourceStore,
                bindingBuilder,
                resourceBuilder,
                roleService,
                membershipService,
                permissionProvider);
    }
}
