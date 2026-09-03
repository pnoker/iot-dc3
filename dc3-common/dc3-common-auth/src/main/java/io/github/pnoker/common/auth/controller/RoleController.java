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

import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.builder.RoleBuilder;
import io.github.pnoker.common.auth.entity.query.RoleOffsetRequest;
import io.github.pnoker.common.auth.entity.vo.RoleTreeVO;
import io.github.pnoker.common.auth.entity.vo.RoleVO;
import io.github.pnoker.common.auth.service.ReactiveRoleService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller exposing role management endpoints.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Tag(
        name = "role",
        description =
                "Role management: create, update, and delete roles that aggregate permissions for assignment to users and service accounts")
@Slf4j
@RestController
@RequestMapping(AuthConstant.ROLE_URL_PREFIX)
@RequiredArgsConstructor
public class RoleController implements BaseController {

    private final RoleBuilder roleBuilder;

    private final ReactiveRoleService roleService;

    /**
     * Create a named role that bundles permissions for the current tenant.
     *
     * @param entityVO role payload to create
     * @return add-success status
     */
    @PreAuthorize("@perm.can('role', 'add')")
    @Operation(
            summary = "Add Role",
            description = "Create a role in the current tenant and return the persisted role representation.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "false"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/add")
    public Mono<ResponseEntity<RoleVO>> add(@Validated(Add.class) @RequestBody RoleVO entityVO) {
        return getPrincipalHeader().flatMap(header -> {
            RoleBO role = roleBuilder.buildBOByVO(entityVO);
            role.setTenantId(header.getTenantId());
            role.setCreatorId(header.getUserId());
            role.setCreatorName(header.getNickName());
            role.setOperatorId(header.getUserId());
            role.setOperatorName(header.getNickName());
            return roleService.add(role).map(saved -> ResponseEntity.status(201).body(roleBuilder.buildVOByBO(saved)));
        });
    }

    /** Delete the role. */
    @PreAuthorize("@perm.can('role', 'delete')")
    @Operation(
            summary = "Delete Role",
            description = "Delete a tenant role without children and return no content on success.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                                @ExtensionProperty(name = "destructive", value = "true"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @DeleteMapping("/delete")
    public Mono<ResponseEntity<Void>> delete(
            @Parameter(description = "Role identifier owned by the current tenant") @NotNull @RequestParam("id")
                    Long id) {
        return getPrincipalHeader()
                .flatMap(header ->
                        roleService.delete(header.getTenantId(), id, header.getUserId(), header.getNickName()))
                .thenReturn(ResponseEntity.noContent().build());
    }

    /** Update one role and emit the updated row. */
    @PreAuthorize("@perm.can('role', 'update')")
    @Operation(
            summary = "Update Role",
            description = "Update editable fields of a tenant role and return the new representation.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/update")
    public Mono<ResponseEntity<RoleVO>> update(@Validated(Update.class) @RequestBody RoleVO entityVO) {
        return getPrincipalHeader().flatMap(header -> {
            RoleBO role = roleBuilder.buildBOByVO(entityVO);
            role.setTenantId(header.getTenantId());
            role.setOperatorId(header.getUserId());
            role.setOperatorName(header.getNickName());
            return roleService
                    .update(header.getTenantId(), role)
                    .map(saved -> ResponseEntity.ok(roleBuilder.buildVOByBO(saved)));
        });
    }

    /** Resolve the role by its id. */
    @PreAuthorize("@perm.can('role', 'get')")
    @Operation(
            summary = "Get Role by ID",
            description = "Fetch one role owned by the current tenant by its identifier.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @GetMapping("/get_by_id")
    public Mono<ResponseEntity<RoleVO>> getById(
            @Parameter(description = "Role identifier owned by the current tenant") @NotNull @RequestParam("id")
                    Long id) {
        return getTenantId()
                .flatMap(tenantId -> roleService
                        .getById(tenantId, id)
                        .map(role -> ResponseEntity.ok(roleBuilder.buildVOByBO(role))));
    }

    /** Page roles matching the tenant-scoped filters. */
    @PreAuthorize("@perm.can('role', 'list')")
    @Operation(
            summary = "List Roles",
            description = "List tenant roles with deterministic offset pagination and optional filters.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/list")
    public Mono<ResponseEntity<OffsetPage<RoleVO>>> list(@RequestBody(required = false) RoleOffsetRequest request) {
        RoleOffsetRequest q = request == null ? new RoleOffsetRequest() : request;
        return getTenantId()
                .flatMap(tenantId -> roleService
                        .list(new io.github.pnoker.common.auth.repository.RoleFilter(
                                tenantId,
                                q.roleName(),
                                q.roleCode(),
                                q.enableFlag(),
                                new io.github.pnoker.db.r2dbc.core.page.PageRequest(q.offset(), q.limit(), q.sort())))
                        .map(page -> ResponseEntity.ok(OffsetPage.of(
                                page.items().stream()
                                        .map(roleBuilder::buildVOByBO)
                                        .toList(),
                                page.offset(),
                                page.limit(),
                                page.total()))));
    }

    /** Emit the role tree for the tenant. */
    @PreAuthorize("@perm.can('role', 'list')")
    @Operation(
            summary = "List Role Tree",
            description = "List the tenant role hierarchy as a nested tree with stable ordering.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/list_tree")
    public Mono<ResponseEntity<List<RoleTreeVO>>> listTree(@RequestBody(required = false) RoleOffsetRequest request) {
        RoleOffsetRequest q = request == null ? new RoleOffsetRequest() : request;
        return getTenantId()
                .flatMap(tenantId -> roleService
                        .listTree(new io.github.pnoker.common.auth.repository.RoleFilter(
                                tenantId,
                                q.roleName(),
                                q.roleCode(),
                                q.enableFlag(),
                                new io.github.pnoker.db.r2dbc.core.page.PageRequest(0, 200, q.sort())))
                        .map(roleBuilder::buildTreeVOByBO)
                        .collectList()
                        .map(ResponseEntity::ok));
    }
}
