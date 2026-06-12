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
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.exception.NotFoundException;
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
 * REST controller exposing role-principal binding management endpoints.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2016.10.1
 */
@Tag(name = "role_principal_bind", description = "Role-principal bindings")
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

    @PreAuthorize("@perm.can('role_principal_bind', 'add')")
    @Operation(summary = "Add Role-principal Binding", description = "Create a role-principal binding record")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody RolePrincipalBindVO entityVO) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            RolePrincipalBindBO entityBO = rolePrincipalBindBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(header.getTenantId());
            requireTenant(header.getTenantId(), roleService.getById(entityBO.getRoleId()));
            tenantMembershipService.requireTenantMember(header.getTenantId(), entityBO.getPrincipalId());
            fillCreateAudit(entityBO, header);
            rolePrincipalBindService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('role_principal_bind', 'delete')")
    @Operation(summary = "Delete Role-principal Binding", description = "Delete a role-principal binding record by ID")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Record ID") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RolePrincipalBindBO entityBO = rolePrincipalBindService.getById(id);
            if (!Objects.equals(tenantId, entityBO.getTenantId())) {
                throw new NotFoundException("Resource does not exist");
            }
            rolePrincipalBindService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('role_principal_bind', 'list')")
    @Operation(summary = "List Role-principal Bindings", description = "List role-principal bindings with pagination")
    @PostMapping("/list")
    public Mono<R<Page<RolePrincipalBindVO>>> list(@RequestBody(required = false) RolePrincipalBindQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RolePrincipalBindQuery query = Objects.isNull(entityQuery) ? new RolePrincipalBindQuery() : entityQuery;
            Page<RolePrincipalBindBO> entityPageBO = rolePrincipalBindService.list(query, tenantId);
            return R.ok(rolePrincipalBindBuilder.buildVOPageByBOPage(entityPageBO));
        }));
    }

    @PreAuthorize("@perm.can('role_principal_bind', 'list')")
    @Operation(summary = "List Roles by Principal", description = "List roles bound to a principal")
    @GetMapping("/list_role_by_principal")
    public Mono<R<List<RoleVO>>> listRoleByPrincipal(@Parameter(description = "Principal ID") @NotNull @RequestParam(value = "principal_id") Long principalId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            tenantMembershipService.requireTenantMember(tenantId, principalId);
            List<RoleBO> roles = rolePrincipalBindService.listRoleByTenantIdAndPrincipalId(tenantId, principalId);
            return R.ok(roleBuilder.buildVOListByBOList(roles));
        }));
    }

    @PreAuthorize("@perm.can('role_principal_bind', 'list')")
    @Operation(summary = "List Users by Role", description = "List human users bound to a role")
    @GetMapping("/list_user_by_role")
    public Mono<R<List<UserVO>>> listUserByRole(@Parameter(description = "Role ID") @NotNull @RequestParam(value = "role_id") Long roleId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, roleService.getById(roleId));
            List<UserBO> users = rolePrincipalBindService.listUserByRoleId(roleId)
                    .stream()
                    .filter(user -> tenantMembershipService.isTenantMember(tenantId, user.getPrincipalId()))
                    .toList();
            return R.ok(userBuilder.buildVOListByBOList(users));
        }));
    }

    private void fillCreateAudit(RolePrincipalBindBO entityBO, RequestHeader.PrincipalHeader header) {
        entityBO.setCreatorId(header.getUserId());
        entityBO.setCreatorName(header.getNickName());
        entityBO.setOperatorId(header.getUserId());
        entityBO.setOperatorName(header.getNickName());
    }

}
