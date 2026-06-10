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
import io.github.pnoker.common.auth.service.TenantBindService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.valid.Add;
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
 * REST controller exposing role-resource binding management endpoints.
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2016.10.1
 */
@Tag(name = "role_resource_bind", description = "Role-resource bindings")
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

    private final TenantBindService tenantBindService;

    @PreAuthorize("@perm.can('role_resource_bind', 'add')")
    @Operation(summary = "Add Role-resource Binding", description = "Create a role-resource binding record")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody RoleResourceBindVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RoleResourceBindBO entityBO = roleResourceBindBuilder.buildBOByVO(entityVO);
            requireTenant(tenantId, roleService.getById(entityBO.getRoleId()));
            roleResourceBindService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('role_resource_bind', 'delete')")
    @Operation(summary = "Delete Role-resource Binding", description = "Delete a role-resource binding record by ID")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Record ID") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RoleResourceBindBO entityBO = roleResourceBindService.getById(id);
            requireTenant(tenantId, roleService.getById(entityBO.getRoleId()));
            roleResourceBindService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('role_resource_bind', 'list')")
    @Operation(summary = "List Role-resource Bindings", description = "List role-resource bindings with pagination")
    @PostMapping("/list")
    public Mono<R<Page<RoleResourceBindVO>>> list(@RequestBody(required = false) RoleResourceBindQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RoleResourceBindQuery query = Objects.isNull(entityQuery) ? new RoleResourceBindQuery() : entityQuery;
            Page<RoleResourceBindBO> entityPageBO = roleResourceBindService.list(query, tenantId);
            Page<RoleResourceBindVO> entityPageVO = roleResourceBindBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    @PreAuthorize("@perm.can('role_resource_bind', 'list')")
    @Operation(summary = "List Resources by Role", description = "List resources bound to a role")
    @GetMapping("/list_resource_by_role")
    public Mono<R<List<ResourceVO>>> listResourceByRole(@Parameter(description = "Role ID") @NotNull @RequestParam(value = "role_id") Long roleId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, roleService.getById(roleId));
            List<ResourceBO> entityBOList = roleResourceBindService.listResourceByRoleId(roleId);
            List<ResourceVO> entityVOList = resourceBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    @PreAuthorize("@perm.can('role_resource_bind', 'list')")
    @Operation(summary = "List Resources by User", description = "List resources accessible to a user")
    @GetMapping("/list_resource_by_user")
    public Mono<R<List<ResourceVO>>> listResourceByUser(@Parameter(description = "User ID") @NotNull @RequestParam(value = "user_id") Long userId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            tenantBindService.requireTenantMember(tenantId, userId);
            List<ResourceBO> entityBOList = roleResourceBindService.listResourceByUserId(userId, tenantId);
            List<ResourceVO> entityVOList = resourceBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    @PreAuthorize("@perm.can('role_resource_bind', 'list')")
    @Operation(summary = "List Roles by Resource", description = "List roles bound to a resource")
    @GetMapping("/list_role_by_resource")
    public Mono<R<List<RoleVO>>> listRoleByResource(@Parameter(description = "Resource ID") @NotNull @RequestParam(value = "resource_id") Long resourceId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            List<RoleBO> entityBOList = roleResourceBindService.listRoleByResourceId(resourceId, tenantId);
            List<RoleVO> entityVOList = roleBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

}
