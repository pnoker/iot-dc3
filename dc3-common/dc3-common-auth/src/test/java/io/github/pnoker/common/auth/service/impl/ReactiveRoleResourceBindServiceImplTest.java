package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.RoleResourceBindBO;
import io.github.pnoker.common.auth.entity.builder.ResourceBuilder;
import io.github.pnoker.common.auth.entity.builder.RoleResourceBindBuilder;
import io.github.pnoker.common.auth.repository.ReactiveResourceLookupStore;
import io.github.pnoker.common.auth.repository.ReactiveRoleResourceBindStore;
import io.github.pnoker.common.auth.service.ReactiveRoleService;
import io.github.pnoker.common.auth.service.ReactiveTenantMembershipService;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveRoleResourceBindServiceImplTest {
    @Mock ReactiveRoleResourceBindStore store;
    @Mock ReactiveResourceLookupStore resourceStore;
    @Mock RoleResourceBindBuilder bindingBuilder;
    @Mock ResourceBuilder resourceBuilder;
    @Mock ReactiveRoleService roleService;
    @Mock ReactiveTenantMembershipService membershipService;

    @Test
    void addRejectsUnknownResource() {
        RoleResourceBindBO binding = binding();
        when(roleService.getById(7L, 11L)).thenReturn(Mono.just(new io.github.pnoker.common.auth.entity.bo.RoleBO()));
        when(resourceStore.listEnabledByIds(List.of(13L))).thenReturn(Flux.empty());
        StepVerifier.create(service().add(binding, 7L)).expectError(NotFoundException.class).verify();
    }

    @Test
    void addRejectsDuplicateBinding() {
        RoleResourceBindBO binding = binding();
        when(roleService.getById(7L, 11L)).thenReturn(Mono.just(new io.github.pnoker.common.auth.entity.bo.RoleBO()));
        when(resourceStore.listEnabledByIds(List.of(13L))).thenReturn(Flux.just(new io.github.pnoker.common.auth.entity.model.ResourceDO()));
        when(store.exists(7L, 11L, 13L)).thenReturn(Mono.just(true));
        StepVerifier.create(service().add(binding, 7L)).expectError(DuplicateException.class).verify();
    }

    private RoleResourceBindBO binding() {
        RoleResourceBindBO binding = new RoleResourceBindBO(); binding.setRoleId(11L); binding.setResourceId(13L); return binding;
    }

    private ReactiveRoleResourceBindServiceImpl service() {
        return new ReactiveRoleResourceBindServiceImpl(store, resourceStore, bindingBuilder, resourceBuilder, roleService, membershipService);
    }
}
