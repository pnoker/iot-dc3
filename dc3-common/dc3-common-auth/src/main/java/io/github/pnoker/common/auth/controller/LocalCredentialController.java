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
import io.github.pnoker.common.auth.entity.bo.LocalCredentialBO;
import io.github.pnoker.common.auth.entity.builder.LocalCredentialBuilder;
import io.github.pnoker.common.auth.entity.query.LocalCredentialQuery;
import io.github.pnoker.common.auth.entity.vo.LocalCredentialVO;
import io.github.pnoker.common.auth.service.LocalCredentialService;
import io.github.pnoker.common.auth.service.TenantMembershipService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.SuccessCode;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * REST controller exposing local credential management endpoints.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Tag(name = "local_credential", description = "Local credential management: create, update, verify, and rotate username/password credentials for user authentication")
@Slf4j
@RestController
@RequestMapping(AuthConstant.LOCAL_CREDENTIAL_URL_PREFIX)
@RequiredArgsConstructor
public class LocalCredentialController implements BaseController {

    private final LocalCredentialBuilder localCredentialBuilder;

    private final LocalCredentialService localCredentialService;

    private final TenantMembershipService tenantMembershipService;

    /**
     * Store a secret credential for a principal under the current tenant.
     *
     * @param entityVO local credential payload to create
     * @return add-success status; the caller must be a tenant member of the target principal
     */
    @PreAuthorize("@perm.can('local_credential', 'add')")
    @Operation(summary = "Add Local Credential", description = "Store a secret credential (e.g. password or API key) for a principal under the current tenant. " +
            "The caller must be a tenant member of the target principal; returns the new credential record id.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false"),
                    @ExtensionProperty(name = "hidden", value = "true")
            }))
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody LocalCredentialVO entityVO) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            LocalCredentialBO entityBO = localCredentialBuilder.buildBOByVO(entityVO);
            tenantMembershipService.requireTenantMember(header.getTenantId(), entityBO.getPrincipalId());
            fillCreateAudit(entityBO, header);
            localCredentialService.add(entityBO);
            return R.ok(SuccessCode.ADD);
        }));
    }

    /**
     * Remove a stored credential by id, revoking that secret for its principal.
     *
     * @param id id of the local credential to delete; its principal must belong to the current tenant
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('local_credential', 'delete')")
    @Operation(summary = "Delete Local Credential", description = "Remove a stored credential by id. The credential's principal must belong to the current tenant; revokes that secret for the principal.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false"),
                    @ExtensionProperty(name = "hidden", value = "true")
            }))
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LocalCredentialBO entityBO = localCredentialService.getById(id);
            tenantMembershipService.requireTenantMember(tenantId, entityBO.getPrincipalId());
            localCredentialService.delete(id);
            return R.ok(SuccessCode.DELETE);
        }));
    }

    /**
     * Modify an existing credential's metadata without changing its owning principal.
     *
     * @param entityVO local credential payload to update; principal id is preserved from the stored record
     * @return update-success status
     */
    @PreAuthorize("@perm.can('local_credential', 'update')")
    @Operation(summary = "Update Local Credential", description = "Modify an existing credential; when the write-only password field is supplied it is re-hashed and overwrites the stored secret. The principal id is preserved from the stored record.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false"),
                    @ExtensionProperty(name = "hidden", value = "true")
            }))
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody LocalCredentialVO entityVO) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            LocalCredentialBO current = localCredentialService.getById(entityVO.getId());
            tenantMembershipService.requireTenantMember(header.getTenantId(), current.getPrincipalId());
            LocalCredentialBO entityBO = localCredentialBuilder.buildBOByVO(entityVO);
            entityBO.setPrincipalId(current.getPrincipalId());
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            localCredentialService.update(entityBO);
            return R.ok(SuccessCode.UPDATE);
        }));
    }

    /**
     * Replace the raw password of a credential by id with a new value.
     *
     * @param id       id of the local credential to reset; its principal must belong to the current tenant
     * @param password new raw password that satisfies the system password policy
     * @return true on successful reset
     */
    @PreAuthorize("@perm.can('local_credential', 'update')")
    @Operation(summary = "Reset Local Credential Password", description = "Replace the raw password of a credential by id with a new value. Use to rotate or recover a principal's stored secret; returns true on success.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false"),
                    @ExtensionProperty(name = "hidden", value = "true")
            }))
    @PostMapping("/reset_password")
    public Mono<R<Boolean>> resetPassword(@Parameter(description = "Primary key of the local credential to reset. The credential's principal must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id,
                                          @Parameter(description = "New raw password to replace the current one. Must meet the system's password policy.", example = "P@ssw0rd!") @NotNull @RequestParam(value = "password") String password) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LocalCredentialBO entityBO = localCredentialService.getById(id);
            tenantMembershipService.requireTenantMember(tenantId, entityBO.getPrincipalId());
            localCredentialService.resetPassword(id, password);
            return R.ok(true);
        }));
    }

    /**
     * Fetch one credential by id without exposing the raw secret.
     *
     * @param id id of the local credential to fetch; its principal must belong to the current tenant
     * @return the matched LocalCredentialVO (raw secret omitted)
     */
    @PreAuthorize("@perm.can('local_credential', 'get')")
    @Operation(summary = "Get Local Credential by ID", description = "Fetch one credential by id. The credential's principal must belong to the current tenant; returns the credential view without exposing the raw secret.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<R<LocalCredentialVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LocalCredentialBO entityBO = localCredentialService.getById(id);
            tenantMembershipService.requireTenantMember(tenantId, entityBO.getPrincipalId());
            return R.ok(localCredentialBuilder.buildVOByBO(entityBO));
        }));
    }

    /**
     * Test whether a login name is free to register.
     *
     * @param name login name to check for availability
     * @return true when no existing credential currently uses the name
     */
    @PreAuthorize("@perm.can('local_credential', 'get')")
    @Operation(summary = "Check Login Name Availability", description = "Test whether a login name is free to register. Returns true when no credential currently uses the name; use before creating a new credential to avoid collisions.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/check")
    public Mono<R<Boolean>> checkLoginNameAvailable(@Parameter(description = "Login name to check for availability. Returns true when no existing credential uses this name.", example = "alice") @NotNull @RequestParam(value = "name") String name) {
        return async(() -> R.ok(localCredentialService.isLoginNameAvailable(name)));
    }

    /**
     * Page through credentials for the current tenant with query filters.
     *
     * @param entityQuery optional filter criteria such as principal and login name; an empty query pages all credentials
     * @return a page of LocalCredentialVO matching the query
     */
    @PreAuthorize("@perm.can('local_credential', 'list')")
    @Operation(summary = "List Local Credentials", description = "Page through credentials for the current tenant with query filters such as principal and login name. Returns a page of credential views; use to browse or select a target credential.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<R<Page<LocalCredentialVO>>> list(@RequestBody(required = false) LocalCredentialQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LocalCredentialQuery query = Objects.isNull(entityQuery) ? new LocalCredentialQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<LocalCredentialBO> entityPageBO = localCredentialService.list(query);
            return R.ok(localCredentialBuilder.buildVOPageByBOPage(entityPageBO));
        }));
    }

    /**
     * Stamp the creator and operator audit fields from the authenticated principal.
     *
     * @param entityBO the credential to stamp
     * @param header   the authenticated principal header
     */
    private void fillCreateAudit(LocalCredentialBO entityBO, RequestHeader.PrincipalHeader header) {
        entityBO.setCreatorId(header.getUserId());
        entityBO.setCreatorName(header.getNickName());
        entityBO.setOperatorId(header.getUserId());
        entityBO.setOperatorName(header.getNickName());
    }

}
