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
import io.github.pnoker.common.auth.entity.bo.RoleTreeBO;
import io.github.pnoker.common.auth.entity.builder.RoleBuilder;
import io.github.pnoker.common.auth.entity.query.RoleQuery;
import io.github.pnoker.common.auth.entity.vo.RoleTreeVO;
import io.github.pnoker.common.auth.entity.vo.RoleVO;
import io.github.pnoker.common.auth.service.RoleService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * REST controller exposing role management endpoints.
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2016.10.1
 */
@Tag(name = "role", description = "Role management: create, update, and delete roles that aggregate permissions for assignment to users and service accounts")
@Slf4j
@RestController
@RequestMapping(AuthConstant.ROLE_URL_PREFIX)
@RequiredArgsConstructor
public class RoleController implements BaseController {

    private final RoleBuilder roleBuilder;

    private final RoleService roleService;

    /**
     * Create a named role that bundles permissions for the current tenant.
     *
     * @param entityVO role payload to create
     * @return add-success status
     */
    @PreAuthorize("@perm.can('role', 'add')")
    @Operation(summary = "Add Role", description = "Create a named permission bundle for the current tenant. A role groups resources (permissions) that can later be assigned to principals; returns the new role ID.")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody RoleVO entityVO) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            RoleBO entityBO = roleBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(header.getTenantId());
            entityBO.setCreatorId(header.getUserId());
            entityBO.setCreatorName(header.getNickName());
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            roleService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * Remove a role by ID, scoped to the current tenant.
     *
     * @param id id of the role to delete
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('role', 'delete')")
    @Operation(summary = "Delete Role", description = "Remove a role by ID, scoped to the current tenant. Deleting detaches the role from its bound resources and principals; the caller must own the role.")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, roleService.getById(id));
            roleService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    /**
     * Modify a tenant-owned role's attributes after verifying ownership.
     *
     * @param entityVO role payload to apply
     * @return update-success status
     */
    @PreAuthorize("@perm.can('role', 'update')")
    @Operation(summary = "Update Role", description = "Modify a tenant-owned role's attributes such as name and enable flag. Verifies ownership before applying; returns the update result.")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody RoleVO entityVO) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            RoleBO entityBO = roleBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(header.getTenantId());
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            requireTenant(header.getTenantId(), roleService.getById(entityBO.getId()));
            roleService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * Fetch one role of the current tenant by its ID.
     *
     * @param id id of the role to retrieve
     * @return the matched RoleVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('role', 'get')")
    @Operation(summary = "Get Role by ID", description = "Fetch one role of the current tenant by its ID. Returns the role's attributes; use to inspect a role before binding resources or principals.")
    @GetMapping("/get_by_id")
    public Mono<R<RoleVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RoleBO entityBO = requireTenant(tenantId, roleService.getById(id));
            RoleVO entityVO = roleBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Page through roles for the current tenant with optional filters.
     *
     * @param entityQuery optional role query filters (tenant id is set server-side)
     * @return a page of RoleVO matching the query
     */
    @PreAuthorize("@perm.can('role', 'list')")
    @Operation(summary = "List Roles", description = "Page through roles for the current tenant with filters such as name and enable flag. Returns a page of roles; use for browsing or selecting a role to bind.")
    @PostMapping("/list")
    public Mono<R<Page<RoleVO>>> list(@RequestBody(required = false) RoleQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RoleQuery query = Objects.isNull(entityQuery) ? new RoleQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<RoleBO> entityPageBO = roleService.list(query);
            Page<RoleVO> entityPageVO = roleBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    /**
     * Return the tenant's roles as a hierarchical tree.
     *
     * @param entityQuery optional role query filters (tenant id is set server-side)
     * @return a list of RoleTreeVO nodes representing the tenant's role hierarchy
     */
    @PreAuthorize("@perm.can('role', 'list')")
    @Operation(summary = "List Role Tree", description = "Return the tenant's roles as a hierarchical tree. Use when a nested role grouping is needed for selection or display rather than a flat page.")
    @PostMapping("/list_tree")
    public Mono<R<List<RoleTreeVO>>> listTree(@RequestBody(required = false) RoleQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RoleQuery query = Objects.isNull(entityQuery) ? new RoleQuery() : entityQuery;
            query.setTenantId(tenantId);
            List<RoleTreeBO> entityBOList = roleService.listTree(query);
            return R.ok(roleBuilder.buildTreeVOListByBOList(entityBOList));
        }));
    }

}
