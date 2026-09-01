package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.bo.RolePrincipalBindBO;
import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.entity.builder.RolePrincipalBindBuilder;
import io.github.pnoker.common.auth.repository.ReactiveRolePrincipalBindStore;
import io.github.pnoker.common.auth.repository.RolePrincipalBindFilter;
import io.github.pnoker.common.auth.service.ReactiveRolePrincipalBindService;
import io.github.pnoker.common.auth.service.ReactiveRoleService;
import io.github.pnoker.common.auth.service.ReactiveTenantMembershipService;
import io.github.pnoker.common.auth.service.ReactiveUserService;
import io.github.pnoker.common.auth.security.PermissionCacheInvalidator;
import io.github.pnoker.common.enums.PrincipalTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Default non-blocking role-principal assignment service. */
@Service
@RequiredArgsConstructor
public class ReactiveRolePrincipalBindServiceImpl implements ReactiveRolePrincipalBindService {
    private final ReactiveRolePrincipalBindStore store;
    private final RolePrincipalBindBuilder builder;
    private final ReactiveRoleService roleService;
    private final ReactiveTenantMembershipService membershipService;
    private final ReactiveUserService userService;
    private PermissionCacheInvalidator permissionCacheInvalidator;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setPermissionCacheInvalidator(PermissionCacheInvalidator invalidator) {
        this.permissionCacheInvalidator = invalidator;
    }

    @Override
    public Mono<RolePrincipalBindBO> add(RolePrincipalBindBO binding) {
        if (binding == null || !valid(binding.getTenantId()) || !valid(binding.getRoleId()) || !valid(binding.getPrincipalId())) {
            return Mono.error(new RequestException("Role principal binding identifiers are required"));
        }
        if (binding.getPrincipalType() == null) binding.setPrincipalType(PrincipalTypeEnum.USER);
        return roleService.getById(binding.getTenantId(), binding.getRoleId())
                .then(membershipService.requireTenantMember(binding.getTenantId(), binding.getPrincipalId()))
                .flatMap(member -> {
                    if (member.getPrincipalType() != null && member.getPrincipalType() != binding.getPrincipalType()) {
                        return Mono.error(new RequestException("Principal type does not match tenant membership"));
                    }
                    return store.exists(binding.getTenantId(), binding.getRoleId(), binding.getPrincipalId(), null);
                })
                .flatMap(duplicate -> Boolean.TRUE.equals(duplicate)
                        ? Mono.error(new DuplicateException("Role principal bind has been duplicated"))
                        : store.insert(binding))
                .doOnSuccess(saved -> invalidate(binding.getTenantId(), binding.getPrincipalId()))
                .map(builder::buildBOByDO);
    }

    @Override
    public Mono<Void> delete(Long tenantId, Long id, Long operatorId, String operatorName) {
        if (!valid(tenantId) || !valid(id)) return Mono.error(new RequestException("Role principal binding ID is required"));
        return store.delete(tenantId, id, operatorId, operatorName)
                .flatMap(deleted -> Boolean.TRUE.equals(deleted) ? Mono.<Void>empty() : Mono.error(new NotFoundException("Role principal bind")))
                .doOnSuccess(ignored -> invalidateTenant(tenantId));
    }

    @Override
    public Mono<OffsetPage<RolePrincipalBindBO>> list(RolePrincipalBindFilter filter) {
        return store.list(filter).map(page -> OffsetPage.of(page.items().stream().map(builder::buildBOByDO).toList(),
                page.offset(), page.limit(), page.total()));
    }

    @Override
    public Flux<RoleBO> listRolesByPrincipal(Long tenantId, Long principalId) {
        return membershipService.requireTenantMember(tenantId, principalId)
                .thenMany(store.listRoleIds(tenantId, principalId))
                .distinct()
                .flatMap(id -> roleService.getById(tenantId, id)
                        .filter(role -> role.getEnableFlag() == null || role.getEnableFlag() == EnableFlagEnum.ENABLE));
    }

    @Override
    public Flux<UserBO> listUsersByRole(Long tenantId, Long roleId) {
        return roleService.getById(tenantId, roleId)
                .thenMany(store.listPrincipalIds(tenantId, roleId, PrincipalTypeEnum.USER.getValue()))
                .distinct()
                .flatMap(id -> userService.getByPrincipalId(tenantId, id)
                        .filter(user -> user.getEnableFlag() == null || user.getEnableFlag() == EnableFlagEnum.ENABLE)
                        .onErrorResume(NotFoundException.class, error -> Mono.empty()));
    }

    private boolean valid(Long value) { return value != null && value > 0; }

    private void invalidate(Long tenantId, Long principalId) {
        if (permissionCacheInvalidator != null) permissionCacheInvalidator.invalidate(tenantId, principalId);
    }

    private void invalidateTenant(Long tenantId) {
        if (permissionCacheInvalidator != null) permissionCacheInvalidator.invalidateTenant(tenantId);
    }
}
