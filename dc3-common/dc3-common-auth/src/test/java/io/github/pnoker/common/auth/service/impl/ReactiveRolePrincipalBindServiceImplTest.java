package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.RolePrincipalBindBO;
import io.github.pnoker.common.auth.entity.bo.TenantMembershipBO;
import io.github.pnoker.common.auth.entity.builder.RolePrincipalBindBuilder;
import io.github.pnoker.common.auth.repository.ReactiveRolePrincipalBindStore;
import io.github.pnoker.common.auth.service.ReactiveRoleService;
import io.github.pnoker.common.auth.service.ReactiveTenantMembershipService;
import io.github.pnoker.common.auth.service.ReactiveUserService;
import io.github.pnoker.common.enums.PrincipalTypeEnum;
import io.github.pnoker.common.exception.DuplicateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveRolePrincipalBindServiceImplTest {
    @Mock ReactiveRolePrincipalBindStore store;
    @Mock RolePrincipalBindBuilder builder;
    @Mock ReactiveRoleService roleService;
    @Mock ReactiveTenantMembershipService membershipService;
    @Mock ReactiveUserService userService;

    @Test
    void addRejectsPrincipalTypeSpoofing() {
        RolePrincipalBindBO binding = binding();
        binding.setPrincipalType(PrincipalTypeEnum.SERVICE_ACCOUNT);
        TenantMembershipBO membership = new TenantMembershipBO();
        membership.setPrincipalType(PrincipalTypeEnum.USER);
        when(roleService.getById(7L, 11L)).thenReturn(Mono.just(new io.github.pnoker.common.auth.entity.bo.RoleBO()));
        when(membershipService.requireTenantMember(7L, 13L)).thenReturn(Mono.just(membership));

        StepVerifier.create(service().add(binding))
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
        when(store.exists(7L, 11L, 13L, null)).thenReturn(Mono.just(true));

        StepVerifier.create(service().add(binding)).expectError(DuplicateException.class).verify();
    }

    private RolePrincipalBindBO binding() {
        RolePrincipalBindBO binding = new RolePrincipalBindBO();
        binding.setTenantId(7L); binding.setRoleId(11L); binding.setPrincipalId(13L);
        binding.setPrincipalType(PrincipalTypeEnum.USER);
        return binding;
    }

    private ReactiveRolePrincipalBindServiceImpl service() {
        return new ReactiveRolePrincipalBindServiceImpl(store, builder, roleService, membershipService, userService);
    }
}
