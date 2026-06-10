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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing role management endpoints.
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2016.10.1
 */
@Tag(name = "role", description = "Roles")
@Slf4j
@RestController
@RequestMapping(AuthConstant.ROLE_URL_PREFIX)
@RequiredArgsConstructor
public class RoleController implements BaseController {

    private final RoleBuilder roleBuilder;

    private final RoleService roleService;

    @PreAuthorize("@perm.can('role', 'add')")
    @Operation(summary = "Add Role", description = "Create a role record")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody RoleVO entityVO) {
        return getUserHeader().flatMap(header -> async(() -> {
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

    @PreAuthorize("@perm.can('role', 'delete')")
    @Operation(summary = "Delete Role", description = "Delete a role record by ID")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Record ID") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, roleService.getById(id));
            roleService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('role', 'update')")
    @Operation(summary = "Update Role", description = "Update a role record")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody RoleVO entityVO) {
        return getUserHeader().flatMap(header -> async(() -> {
            RoleBO entityBO = roleBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(header.getTenantId());
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            requireTenant(header.getTenantId(), roleService.getById(entityBO.getId()));
            roleService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('role', 'get')")
    @Operation(summary = "Get Role by ID", description = "Get role details by ID")
    @GetMapping("/get_by_id")
    public Mono<R<RoleVO>> getById(@Parameter(description = "Record ID") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RoleBO entityBO = requireTenant(tenantId, roleService.getById(id));
            RoleVO entityVO = roleBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    @PreAuthorize("@perm.can('role', 'list')")
    @Operation(summary = "List Roles", description = "List roles with pagination")
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

    @PreAuthorize("@perm.can('role', 'list')")
    @Operation(summary = "List Role Tree", description = "List roles as a tenant-scoped tree")
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
