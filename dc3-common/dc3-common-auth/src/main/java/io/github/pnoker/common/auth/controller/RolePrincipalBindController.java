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

package io.github.pnoker.common.auth.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.bo.RolePrincipalBindBO;
import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.entity.builder.RoleBuilder;
import io.github.pnoker.common.auth.entity.builder.RolePrincipalBindBuilder;
import io.github.pnoker.common.auth.entity.builder.UserBuilder;
import io.github.pnoker.common.auth.entity.query.RolePrincipalBindQuery;
import io.github.pnoker.common.auth.entity.vo.RolePrincipalBindVO;
import io.github.pnoker.common.auth.entity.vo.RoleVO;
import io.github.pnoker.common.auth.entity.vo.UserVO;
import io.github.pnoker.common.auth.service.RolePrincipalBindService;
import io.github.pnoker.common.auth.service.RoleService;
import io.github.pnoker.common.auth.service.TenantMembershipService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.SuccessCode;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.valid.Add;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * REST controller exposing role-principal binding management endpoints.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2016.10.1
 */
@Tag(name = "role_principal_bind", description = "Role-to-principal bindings: assign roles to users or service accounts, enabling or revoking permission sets for principals")
@Slf4j
@RestController
@RequestMapping(AuthConstant.ROLE_PRINCIPAL_URL_PREFIX)
@RequiredArgsConstructor
public class RolePrincipalBindController implements BaseController {

    private final RolePrincipalBindBuilder rolePrincipalBindBuilder;

    private final RolePrincipalBindService rolePrincipalBindService;

    private final RoleBuilder roleBuilder;

    private final UserBuilder userBuilder;

    private final RoleService roleService;

    private final TenantMembershipService tenantMembershipService;

    /**
     * Assign a role to a principal within the current tenant.
     *
     * @param entityVO role-principal binding payload to create
     * @return add-success status
     */
    @PreAuthorize("@perm.can('role_principal_bind', 'add')")
    @Operation(summary = "Bind Principal to Role", description = "Assign a role to a principal (user or service account) within the current tenant. " +
            "Both the role and the principal must already belong to the tenant; returns an add-success response.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody RolePrincipalBindVO entityVO) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            RolePrincipalBindBO entityBO = rolePrincipalBindBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(header.getTenantId());
            requireTenant(header.getTenantId(), roleService.getById(entityBO.getRoleId()));
            tenantMembershipService.requireTenantMember(header.getTenantId(), entityBO.getPrincipalId());
            fillCreateAudit(entityBO, header);
            rolePrincipalBindService.add(entityBO);
            return R.ok(SuccessCode.ADD);
        }));
    }

    /**
     * Remove a role-principal binding by record ID after verifying tenant ownership.
     *
     * @param id id of the binding to delete
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('role_principal_bind', 'delete')")
    @Operation(summary = "Delete Role-principal Binding", description = "Remove a role-principal binding by its record ID. " +
            "Verifies the binding belongs to the current tenant before deleting; use to revoke a role assignment.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RolePrincipalBindBO entityBO = rolePrincipalBindService.getById(id);
            if (!Objects.equals(tenantId, entityBO.getTenantId())) {
                throw new NotFoundException("Resource does not exist");
            }
            rolePrincipalBindService.delete(id);
            return R.ok(SuccessCode.DELETE);
        }));
    }

    /**
     * Page through role-principal bindings for the current tenant.
     *
     * @param entityQuery optional binding query filters (tenant id is pinned server-side)
     * @return a page of RolePrincipalBindVO matching the query
     */
    @PreAuthorize("@perm.can('role_principal_bind', 'list')")
    @Operation(summary = "List Role-principal Bindings", description = "Page through role-principal bindings for the current tenant with optional query filters. " +
            "Returns a page of bindings; use to browse which principals hold which roles.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<R<Page<RolePrincipalBindVO>>> list(@RequestBody(required = false) RolePrincipalBindQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RolePrincipalBindQuery query = Objects.isNull(entityQuery) ? new RolePrincipalBindQuery() : entityQuery;
            Page<RolePrincipalBindBO> entityPageBO = rolePrincipalBindService.list(query, tenantId);
            return R.ok(rolePrincipalBindBuilder.buildVOPageByBOPage(entityPageBO));
        }));
    }

    /**
     * Return the roles assigned to one principal within the current tenant.
     *
     * @param principalId id of the principal whose roles are to be listed
     * @return a list of RoleVO assigned to the principal
     */
    @PreAuthorize("@perm.can('role_principal_bind', 'list')")
    @Operation(summary = "List Roles by Principal", description = "Return the roles assigned to one principal within the current tenant. " +
            "Accepts a principal ID (must be a tenant member); use to see what permissions a user or service account has.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_role_by_principal")
    public Mono<R<List<RoleVO>>> listRoleByPrincipal(@Parameter(description = "ID of the principal (user or service account) whose roles are to be listed; must be a member of the current tenant.", example = "1024") @NotNull @RequestParam(value = "principal_id") Long principalId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            tenantMembershipService.requireTenantMember(tenantId, principalId);
            List<RoleBO> roles = rolePrincipalBindService.listRoleByTenantIdAndPrincipalId(tenantId, principalId);
            return R.ok(roleBuilder.buildVOListByBOList(roles));
        }));
    }

    /**
     * Return the human users currently bound to a role, filtered to tenant members.
     *
     * @param roleId id of the role whose bound users are to be listed
     * @return a list of UserVO bound to the role within the tenant
     */
    @PreAuthorize("@perm.can('role_principal_bind', 'list')")
    @Operation(summary = "List Users by Role", description = "Return the human users currently bound to a role (the role must belong to the current tenant). " +
            "Results are filtered to tenant members; use to see who holds a given role.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_user_by_role")
    public Mono<R<List<UserVO>>> listUserByRole(@Parameter(description = "ID of the role whose bound users are to be listed; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "role_id") Long roleId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, roleService.getById(roleId));
            List<UserBO> users = rolePrincipalBindService.listUserByRoleId(roleId)
                    .stream()
                    .filter(user -> tenantMembershipService.isTenantMember(tenantId, user.getPrincipalId()))
                    .toList();
            return R.ok(userBuilder.buildVOListByBOList(users));
        }));
    }

    /**
     * Stamp the creator and operator audit fields from the authenticated principal.
     *
     * @param entityBO the binding to stamp
     * @param header   the authenticated principal header
     */
    private void fillCreateAudit(RolePrincipalBindBO entityBO, RequestHeader.PrincipalHeader header) {
        entityBO.setCreatorId(header.getUserId());
        entityBO.setCreatorName(header.getNickName());
        entityBO.setOperatorId(header.getUserId());
        entityBO.setOperatorName(header.getNickName());
    }

}
