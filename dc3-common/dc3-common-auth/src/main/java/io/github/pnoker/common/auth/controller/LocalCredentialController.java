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

import io.github.pnoker.common.auth.entity.bo.LocalCredentialBO;
import io.github.pnoker.common.auth.entity.builder.LocalCredentialBuilder;
import io.github.pnoker.common.auth.entity.query.LocalCredentialOffsetRequest;
import io.github.pnoker.common.auth.entity.vo.LocalCredentialVO;
import io.github.pnoker.common.auth.repository.LocalCredentialFilter;
import io.github.pnoker.common.auth.service.ReactiveLocalCredentialCommandService;
import io.github.pnoker.common.auth.service.ReactiveLocalCredentialService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
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

/** Tenant-scoped, non-blocking local credential endpoints. */
@Tag(name = "local_credential", description = "Local credential management")
@RestController
@RequestMapping(AuthConstant.LOCAL_CREDENTIAL_URL_PREFIX)
@RequiredArgsConstructor
public class LocalCredentialController implements BaseController {

    private final LocalCredentialBuilder localCredentialBuilder;
    private final ReactiveLocalCredentialService reactiveLocalCredentialService;
    private final ReactiveLocalCredentialCommandService reactiveLocalCredentialCommandService;

    @PreAuthorize("@perm.can('local_credential', 'add')")
    @Operation(
            summary = "Add Local Credential",
            description =
                    "Create a tenant-scoped local credential and return the sanitized credential projection without exposing its password hash.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                                        @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "false"),
                                        @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/add")
    public Mono<ResponseEntity<LocalCredentialVO>> add(@Validated(Add.class) @RequestBody LocalCredentialVO entityVO) {
        return getPrincipalHeader().flatMap(header -> {
            LocalCredentialBO credential = localCredentialBuilder.buildBOByVO(entityVO);
            credential.setPrincipalId(parseId(entityVO.getPrincipalId(), "principalId"));
            return reactiveLocalCredentialCommandService
                    .add(header.getTenantId(), credential, header.getUserId(), header.getNickName())
                    .map(saved -> ResponseEntity.status(201).body(localCredentialBuilder.buildVOByBO(saved)));
        });
    }

    @PreAuthorize("@perm.can('local_credential', 'delete')")
    @Operation(
            summary = "Delete Local Credential",
            description =
                    "Revoke a tenant-scoped local credential using a transaction and return no response body after successful deletion.",
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
            @Parameter(description = "Primary key of the credential to revoke; it must belong to the current tenant.")
                    @NotNull
                    @RequestParam("id")
                    Long id) {
        return getPrincipalHeader()
                .flatMap(header -> reactiveLocalCredentialCommandService
                        .delete(header.getTenantId(), id, header.getUserId(), header.getNickName())
                        .thenReturn(ResponseEntity.noContent().build()));
    }

    @PreAuthorize("@perm.can('local_credential', 'update')")
    @Operation(
            summary = "Update Local Credential",
            description =
                    "Update tenant-scoped credential metadata and optionally replace its password, returning the sanitized credential projection.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                                        @ExtensionProperty(name = "destructive", value = "true"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                        @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/update")
    public Mono<ResponseEntity<LocalCredentialVO>> update(
            @Validated(Update.class) @RequestBody LocalCredentialVO entityVO) {
        return getPrincipalHeader().flatMap(header -> {
            LocalCredentialBO credential = localCredentialBuilder.buildBOByVO(entityVO);
            credential.setId(parseId(entityVO.getId(), "id"));
            return reactiveLocalCredentialCommandService
                    .update(header.getTenantId(), credential, header.getUserId(), header.getNickName())
                    .map(saved -> ResponseEntity.ok(localCredentialBuilder.buildVOByBO(saved)));
        });
    }

    @PreAuthorize("@perm.can('local_credential', 'update')")
    @Operation(
            summary = "Reset Local Credential Password",
            description =
                    "Replace a credential password, clear failed-login state, and require a password change on the next login.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                                        @ExtensionProperty(name = "destructive", value = "true"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                        @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/reset_password")
    public Mono<ResponseEntity<LocalCredentialVO>> resetPassword(
            @Parameter(description = "Primary key of the credential to reset; it must belong to the current tenant.")
                    @NotNull
                    @RequestParam("id")
                    Long id,
            @Parameter(description = "New raw password that replaces the current credential secret.")
                    @NotNull
                    @RequestParam("password")
                    String password) {
        return getPrincipalHeader()
                .flatMap(header -> reactiveLocalCredentialCommandService
                        .resetPassword(header.getTenantId(), id, password, header.getUserId(), header.getNickName())
                        .map(saved -> ResponseEntity.ok(localCredentialBuilder.buildVOByBO(saved))));
    }

    @PreAuthorize("@perm.can('local_credential', 'get')")
    @Operation(
            summary = "Get Local Credential by ID",
            description =
                    "Fetch one tenant-scoped credential projection by identifier without ever returning its password hash or raw secret.",
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
    public Mono<ResponseEntity<LocalCredentialVO>> getById(
            @Parameter(description = "Primary key of the credential to fetch; it must belong to the current tenant.")
                    @NotNull
                    @RequestParam("id")
                    Long id) {
        return getTenantId()
                .flatMap(tenantId -> reactiveLocalCredentialService
                        .getById(tenantId, id)
                        .map(credential -> ResponseEntity.ok(localCredentialBuilder.buildVOByBO(credential))));
    }

    @PreAuthorize("@perm.can('local_credential', 'get')")
    @Operation(
            summary = "Check Login Name Availability",
            description = "Check whether a normalized login name is available for a credential in the current tenant.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                        @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                        @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @GetMapping("/check")
    public Mono<ResponseEntity<Boolean>> checkLoginNameAvailable(
            @Parameter(description = "Login name whose availability should be checked within the current tenant.")
                    @NotNull
                    @RequestParam("name")
                    String name) {
        return getTenantId()
                .flatMap(tenantId -> reactiveLocalCredentialService.isLoginNameAvailable(tenantId, name))
                .map(ResponseEntity::ok);
    }

    @PreAuthorize("@perm.can('local_credential', 'list')")
    @Operation(
            summary = "List Local Credentials",
            description =
                    "List tenant-scoped credentials with offset pagination and whitelisted stable sorting; password hashes are never returned.",
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
    public Mono<ResponseEntity<OffsetPage<LocalCredentialVO>>> list(
            @RequestBody(required = false) LocalCredentialOffsetRequest request) {
        LocalCredentialOffsetRequest query = request == null ? new LocalCredentialOffsetRequest() : request;
        return getTenantId()
                .flatMap(tenantId -> reactiveLocalCredentialService
                        .list(new LocalCredentialFilter(
                                tenantId,
                                query.principalId(),
                                query.loginName(),
                                query.credentialType(),
                                query.enableFlag(),
                                new PageRequest(query.offset(), query.limit(), query.sort())))
                        .map(page -> ResponseEntity.ok(OffsetPage.of(
                                page.items().stream()
                                        .map(localCredentialBuilder::buildVOByBO)
                                        .toList(),
                                page.offset(),
                                page.limit(),
                                page.total()))));
    }

    private Long parseId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(field + " must be numeric", error);
        }
    }
}
