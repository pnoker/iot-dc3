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

import io.github.pnoker.common.auth.entity.bo.RolePrincipalBindBO;
import io.github.pnoker.common.auth.entity.builder.RoleBuilder;
import io.github.pnoker.common.auth.entity.builder.RolePrincipalBindBuilder;
import io.github.pnoker.common.auth.entity.builder.UserBuilder;
import io.github.pnoker.common.auth.entity.query.RolePrincipalBindOffsetRequest;
import io.github.pnoker.common.auth.entity.vo.RolePrincipalBindVO;
import io.github.pnoker.common.auth.entity.vo.RoleVO;
import io.github.pnoker.common.auth.entity.vo.UserVO;
import io.github.pnoker.common.auth.repository.RolePrincipalBindFilter;
import io.github.pnoker.common.auth.service.ReactiveRolePrincipalBindService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

/** REST endpoints for tenant-scoped role-principal assignments. */
@Tag(name = "role_principal_bind")
@RestController
@RequestMapping(AuthConstant.ROLE_PRINCIPAL_URL_PREFIX)
@RequiredArgsConstructor
public class RolePrincipalBindController implements BaseController {
    private final RolePrincipalBindBuilder bindingBuilder;
    private final ReactiveRolePrincipalBindService bindingService;
    private final RoleBuilder roleBuilder;
    private final UserBuilder userBuilder;

    /** Add one role principal binding and return the stored view. */
    @PreAuthorize("@perm.can('role_principal_bind', 'add')")
    @Operation(
            summary = "Bind Principal to Role",
            description = "Assign a tenant member to an existing role and return the created binding.",
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
    public Mono<ResponseEntity<RolePrincipalBindVO>> add(
            @Validated(Add.class) @RequestBody RolePrincipalBindVO entityVO) {
        return getPrincipalHeader().flatMap(header -> {
            RolePrincipalBindBO binding = bindingBuilder.buildBOByVO(entityVO);
            binding.setTenantId(header.getTenantId());
            binding.setCreatorId(header.getUserId());
            binding.setCreatorName(header.getNickName());
            binding.setOperatorId(header.getUserId());
            binding.setOperatorName(header.getNickName());
            return bindingService
                    .add(binding)
                    .map(saved -> ResponseEntity.status(201).body(bindingBuilder.buildVOByBO(saved)));
        });
    }

    /** Delete the role principal binding. */
    @PreAuthorize("@perm.can('role_principal_bind', 'delete')")
    @Operation(
            summary = "Delete Role-principal Binding",
            description = "Revoke a role assignment owned by the current tenant and return no content.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                                @ExtensionProperty(name = "destructive", value = "true"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @DeleteMapping("/delete")
    public Mono<ResponseEntity<Void>> delete(
            @Parameter(description = "Binding identifier owned by the current tenant") @NotNull @RequestParam("id")
                    Long id) {
        return getPrincipalHeader()
                .flatMap(header ->
                        bindingService.delete(header.getTenantId(), id, header.getUserId(), header.getNickName()))
                .thenReturn(ResponseEntity.noContent().build());
    }

    /** Page role principal bindings matching the tenant-scoped filters. */
    @PreAuthorize("@perm.can('role_principal_bind', 'list')")
    @Operation(
            summary = "List Role-principal Bindings",
            description = "List tenant role assignments using deterministic offset pagination and optional filters.",
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
    public Mono<ResponseEntity<OffsetPage<RolePrincipalBindVO>>> list(
            @RequestBody(required = false) RolePrincipalBindOffsetRequest request) {
        RolePrincipalBindOffsetRequest query = request == null ? new RolePrincipalBindOffsetRequest() : request;
        return getTenantId()
                .flatMap(tenantId -> bindingService
                        .list(new RolePrincipalBindFilter(
                                tenantId,
                                query.roleId(),
                                query.principalId(),
                                query.principalType(),
                                new PageRequest(query.offset(), query.limit(), query.sort())))
                        .map(page -> ResponseEntity.ok(OffsetPage.of(
                                page.items().stream()
                                        .map(bindingBuilder::buildVOByBO)
                                        .toList(),
                                page.offset(),
                                page.limit(),
                                page.total()))));
    }

    /** List role principal bindings matched by principal. */
    @PreAuthorize("@perm.can('role_principal_bind', 'list')")
    @Operation(
            summary = "List Roles by Principal",
            description = "List enabled roles assigned to a tenant principal.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @GetMapping("/list_role_by_principal")
    public Mono<ResponseEntity<List<RoleVO>>> listRoleByPrincipal(
            @Parameter(description = "Principal identifier within the current tenant")
                    @NotNull
                    @RequestParam("principal_id")
                    Long principalId) {
        return getTenantId()
                .flatMap(tenantId -> bindingService
                        .listRolesByPrincipal(tenantId, principalId)
                        .map(roleBuilder::buildVOByBO)
                        .collectList()
                        .map(ResponseEntity::ok));
    }

    /** List role principal bindings matched by role. */
    @PreAuthorize("@perm.can('role_principal_bind', 'list')")
    @Operation(
            summary = "List Users by Role",
            description = "List enabled tenant users assigned to a role.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @GetMapping("/list_user_by_role")
    public Mono<ResponseEntity<List<UserVO>>> listUserByRole(
            @Parameter(description = "Role identifier within the current tenant") @NotNull @RequestParam("role_id")
                    Long roleId) {
        return getTenantId()
                .flatMap(tenantId -> bindingService
                        .listUsersByRole(tenantId, roleId)
                        .map(userBuilder::buildVOByBO)
                        .collectList()
                        .map(ResponseEntity::ok));
    }
}
