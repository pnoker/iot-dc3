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
import io.github.pnoker.common.auth.entity.bo.ResourceBO;
import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.bo.RoleResourceBindBO;
import io.github.pnoker.common.auth.entity.builder.ResourceBuilder;
import io.github.pnoker.common.auth.entity.builder.RoleBuilder;
import io.github.pnoker.common.auth.entity.builder.RoleResourceBindBuilder;
import io.github.pnoker.common.auth.entity.query.RoleResourceBindQuery;
import io.github.pnoker.common.auth.entity.vo.ResourceVO;
import io.github.pnoker.common.auth.entity.vo.RoleResourceBindVO;
import io.github.pnoker.common.auth.entity.vo.RoleVO;
import io.github.pnoker.common.auth.service.RoleResourceBindService;
import io.github.pnoker.common.auth.service.RoleService;
import io.github.pnoker.common.auth.service.TenantMembershipService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.SuccessCode;
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
 * REST controller exposing role-resource binding management endpoints.
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2016.10.1
 */
@Tag(name = "role_resource_bind", description = "Role-to-resource permission bindings: grant or revoke access to specific API endpoints, menus, and secured artifacts for roles")
@Slf4j
@RestController
@RequestMapping(AuthConstant.ROLE_RESOURCE_URL_PREFIX)
@RequiredArgsConstructor
public class RoleResourceBindController implements BaseController {

    private final RoleResourceBindBuilder roleResourceBindBuilder;

    private final RoleResourceBindService roleResourceBindService;

    private final ResourceBuilder resourceBuilder;

    private final RoleBuilder roleBuilder;

    private final RoleService roleService;

    private final TenantMembershipService tenantMembershipService;

    /**
     * Bind a single resource (permission) to a role under the current tenant.
     *
     * @param entityVO role-resource binding payload to create
     * @return add-success status
     */
    @PreAuthorize("@perm.can('role_resource_bind', 'add')")
    @Operation(summary = "Add Role-Resource Binding", description = "Bind a single resource (permission) to a role under the current tenant. " +
            "The role must belong to the tenant; use to attach an individual permission to a role.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody RoleResourceBindVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RoleResourceBindBO entityBO = roleResourceBindBuilder.buildBOByVO(entityVO);
            requireTenant(tenantId, roleService.getById(entityBO.getRoleId()));
            roleResourceBindService.add(entityBO);
            return R.ok(SuccessCode.ADD);
        }));
    }

    /**
     * Remove a single role-resource binding by record ID after verifying tenant ownership.
     *
     * @param id id of the binding to delete
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('role_resource_bind', 'delete')")
    @Operation(summary = "Delete Role-Resource Binding", description = "Remove a single role-resource binding by its record ID. " +
            "The binding's role must belong to the current tenant; returns the deletion result.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RoleResourceBindBO entityBO = roleResourceBindService.getById(id);
            requireTenant(tenantId, roleService.getById(entityBO.getRoleId()));
            roleResourceBindService.delete(id);
            return R.ok(SuccessCode.DELETE);
        }));
    }

    /**
     * Page through role-resource binding records for the current tenant.
     *
     * @param entityQuery optional binding query filters (tenant id is pinned server-side)
     * @return a page of RoleResourceBindVO matching the query
     */
    @PreAuthorize("@perm.can('role_resource_bind', 'list')")
    @Operation(summary = "List Role-Resource Bindings", description = "Page through role-resource binding records for the current tenant. " +
            "Accepts filter criteria; returns a page of bindings showing which resources each role grants.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<R<Page<RoleResourceBindVO>>> list(@RequestBody(required = false) RoleResourceBindQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RoleResourceBindQuery query = Objects.isNull(entityQuery) ? new RoleResourceBindQuery() : entityQuery;
            Page<RoleResourceBindBO> entityPageBO = roleResourceBindService.list(query, tenantId);
            Page<RoleResourceBindVO> entityPageVO = roleResourceBindBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    /**
     * Return the resources (permissions) granted to a role within the current tenant.
     *
     * @param roleId id of the role whose granted resources are to be listed
     * @return a list of ResourceVO granted to the role
     */
    @PreAuthorize("@perm.can('role_resource_bind', 'list')")
    @Operation(summary = "List Resources by Role", description = "Return the full set of resources (permissions) granted to a role. " +
            "The role must belong to the current tenant; use to inspect what a role can do.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_resource_by_role")
    public Mono<R<List<ResourceVO>>> listResourceByRole(@Parameter(description = "Identifier of the role whose granted resources are to be listed; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "role_id") Long roleId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, roleService.getById(roleId));
            List<ResourceBO> entityBOList = roleResourceBindService.listResourceByRoleId(roleId);
            List<ResourceVO> entityVOList = resourceBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    /**
     * Resolve a principal's effective permissions by aggregating resources across its roles.
     *
     * @param principalId id of the principal whose effective permissions are to be resolved
     * @return a deduplicated list of ResourceVO the principal can access within the tenant
     */
    @PreAuthorize("@perm.can('role_resource_bind', 'list')")
    @Operation(summary = "List Resources by Principal", description = "Resolve the effective permissions of a principal (user or service account) " +
            "by aggregating resources from every role assigned to it within the current tenant. " +
            "The principal must be a member of the tenant; returns the deduplicated resource list.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_resource_by_principal")
    public Mono<R<List<ResourceVO>>> listResourceByPrincipal(@Parameter(description = "Identifier of the principal (user or service account) whose effective permissions are to be resolved; must be a member of the current tenant.", example = "1024") @NotNull @RequestParam(value = "principal_id") Long principalId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            tenantMembershipService.requireTenantMember(tenantId, principalId);
            List<ResourceBO> entityBOList = roleResourceBindService.listResourceByPrincipalId(principalId, tenantId);
            List<ResourceVO> entityVOList = resourceBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    /**
     * Return the roles within the current tenant that grant a given resource (permission).
     *
     * @param resourceId id of the resource for which to list the granting roles
     * @return a list of RoleVO that grant the resource within the tenant
     */
    @PreAuthorize("@perm.can('role_resource_bind', 'list')")
    @Operation(summary = "List Roles by Resource", description = "Return the roles within the current tenant that grant a given resource (permission). " +
            "Use to find which roles can perform a specific action.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_role_by_resource")
    public Mono<R<List<RoleVO>>> listRoleByResource(@Parameter(description = "Identifier of the resource (permission) for which to list the roles that grant it; scoped to the current tenant.", example = "1024") @NotNull @RequestParam(value = "resource_id") Long resourceId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            List<RoleBO> entityBOList = roleResourceBindService.listRoleByResourceId(resourceId, tenantId);
            List<RoleVO> entityVOList = roleBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

}
