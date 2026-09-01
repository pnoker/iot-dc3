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

import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.entity.builder.UserBuilder;
import io.github.pnoker.common.auth.entity.query.UserOffsetRequest;
import io.github.pnoker.common.auth.entity.vo.UserVO;
import io.github.pnoker.common.auth.service.ReactiveUserCommandService;
import io.github.pnoker.common.auth.service.ReactiveUserService;
import io.github.pnoker.common.auth.repository.UserFilter;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;


/**
 * REST controller exposing user account management endpoints.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Tag(name = "user", description = "User account lifecycle: create, update, enable, disable, and manage user identities within tenant scope")
@Slf4j
@RestController
@RequestMapping(AuthConstant.USER_PROFILE_URL_PREFIX)
@RequiredArgsConstructor
public class UserController implements BaseController {

    private final UserBuilder userBuilder;

    private final ReactiveUserService reactiveUserService;

    private final ReactiveUserCommandService reactiveUserCommandService;

    /**
     * Create a user under the current tenant and enroll them as an active tenant member.
     *
     * @param entityVO user payload to create
     * @return add-success status
     */
    @PreAuthorize("@perm.can('user', 'add')")
    @Operation(summary = "Add User", description = "Create a new user under the current tenant and enroll them as an active tenant member. A user authenticates with username and password to access the platform; returns an add-success status.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<ResponseEntity<UserVO>> add(@Validated(Add.class) @RequestBody UserVO entityVO) {
        return getPrincipalHeader().flatMap(header -> {
            UserBO user = userBuilder.buildBOByVO(entityVO);
            user.setCreatorId(header.getUserId());
            user.setCreatorName(header.getNickName());
            user.setOperatorId(header.getUserId());
            user.setOperatorName(header.getNickName());
            return reactiveUserCommandService.add(header.getTenantId(), user, header.getUserId(), header.getNickName())
                    .map(saved -> ResponseEntity.status(201).body(userBuilder.buildVOByBO(saved)));
        });
    }

    /**
     * Remove a user and their tenant membership, verified via principal ownership.
     *
     * @param id id of the user to delete
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('user', 'delete')")
    @Operation(summary = "Delete User", description = "Remove a user and their tenant membership for the current tenant (verified by ID). Use to revoke a tenant member's access; returns a delete-success status.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @DeleteMapping("/delete")
    public Mono<ResponseEntity<Void>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getPrincipalHeader().flatMap(header -> reactiveUserCommandService
                .delete(header.getTenantId(), id, header.getUserId(), header.getNickName())
                .thenReturn(ResponseEntity.noContent().build()));
    }

    /**
     * Modify an existing user's profile after verifying tenant ownership via principal.
     *
     * @param entityVO user payload to apply
     * @return update-success status
     */
    @PreAuthorize("@perm.can('user', 'update')")
    @Operation(summary = "Update User", description = "Modify an existing user's profile (tenant-scoped, verified by ID). Use to change attributes like nickname or enable flag; returns an update-success status.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/update")
    public Mono<ResponseEntity<UserVO>> update(@Validated(Update.class) @RequestBody UserVO entityVO) {
        return getPrincipalHeader().flatMap(header -> {
            UserBO entityBO = userBuilder.buildBOByVO(entityVO);
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            return reactiveUserService.getById(header.getTenantId(), entityBO.getId())
                    .flatMap(current -> {
                        entityBO.setPrincipalId(current.getPrincipalId());
                        return reactiveUserCommandService.update(header.getTenantId(), entityBO,
                                header.getUserId(), header.getNickName());
                    })
                    .map(saved -> ResponseEntity.ok(userBuilder.buildVOByBO(saved)));
        });
    }

    /**
     * Fetch one user by ID within the current tenant (ownership checked via principal).
     *
     * @param id id of the user to retrieve
     * @return the matched UserVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('user', 'get')")
    @Operation(summary = "Get User by ID", description = "Fetch one user by ID within the current tenant (ownership checked via principal). Returns the user profile; use when you already hold the numeric ID.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<ResponseEntity<UserVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> reactiveUserService.getById(tenantId, id)
                .map(user -> ResponseEntity.ok(userBuilder.buildVOByBO(user))));
    }

    /**
     * Look up one user by username within the current tenant.
     *
     * @param name username (login name) of the user to retrieve
     * @return the matched UserVO; not-found and wrong-tenant both 404 to avoid leaking name existence
     */
    @PreAuthorize("@perm.can('user', 'get')")
    @Operation(summary = "Get User by Name", description = "Look up one user by username within the current tenant. Returns a 404 for both not-found and wrong-tenant so name existence is not leaked; use when resolving a login name to a profile.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_name")
    public Mono<ResponseEntity<UserVO>> getByName(@Parameter(description = "Username (login name) of the user to look up within the current tenant. Both not-found and wrong-tenant cases return 404 to avoid leaking name existence.", example = "john_doe") @NotNull @RequestParam(value = "name") String name) {
        return getTenantId().flatMap(tenantId -> reactiveUserService.getByUserName(tenantId, name)
                .map(user -> ResponseEntity.ok(userBuilder.buildVOByBO(user))));
    }

    /**
     * Page through users for the current tenant with optional filters (tenant scope enforced server-side).
     *
     * @param entityQuery optional user query filters (tenant id is overwritten server-side)
     * @return a page of UserVO matching the query
     */
    @PreAuthorize("@perm.can('user', 'list')")
    @Operation(summary = "List Users", description = "Page through users for the current tenant with filters from the query body (tenant scope is enforced server-side, not client-supplied). Returns a page of user profiles for browsing or selecting a target user.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<ResponseEntity<OffsetPage<UserVO>>> list(@RequestBody(required = false) UserOffsetRequest request) {
        UserOffsetRequest query = request == null ? new UserOffsetRequest() : request;
        return getTenantId().flatMap(tenantId -> reactiveUserService.list(new UserFilter(tenantId, query.principalId(),
                query.nickName(), query.userName(), query.phone(), query.email(), query.enableFlag(),
                new PageRequest(query.offset(), query.limit(), query.sort())))
                .map(page -> ResponseEntity.ok(OffsetPage.of(page.items().stream().map(userBuilder::buildVOByBO).toList(),
                        page.offset(), page.limit(), page.total()))));
    }

}
