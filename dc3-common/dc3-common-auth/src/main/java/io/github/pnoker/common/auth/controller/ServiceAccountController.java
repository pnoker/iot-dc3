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
import io.github.pnoker.common.auth.entity.bo.ServiceAccountBO;
import io.github.pnoker.common.auth.entity.builder.ServiceAccountBuilder;
import io.github.pnoker.common.auth.entity.query.ServiceAccountQuery;
import io.github.pnoker.common.auth.entity.vo.ServiceAccountVO;
import io.github.pnoker.common.auth.service.AuditLogService;
import io.github.pnoker.common.auth.service.ServiceAccountService;
import io.github.pnoker.common.auth.service.TenantMembershipService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.EnableFlagEnum;
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
 * REST controller exposing service account management endpoints.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Tag(name = "service_account", description = "Service account lifecycle: manage machine-to-machine identities including creation, update, credential rotation, and enablement")
@Slf4j
@RestController
@RequestMapping(AuthConstant.SERVICE_ACCOUNT_URL_PREFIX)
@RequiredArgsConstructor
public class ServiceAccountController implements BaseController {

    private final ServiceAccountBuilder serviceAccountBuilder;

    private final ServiceAccountService serviceAccountService;

    private final AuditLogService auditLogService;

    private final TenantMembershipService tenantMembershipService;

    /**
     * Create a non-human service account principal under the current tenant.
     *
     * @param entityVO service account payload to create
     * @return add-success status
     */
    @PreAuthorize("@perm.can('service_account', 'add')")
    @Operation(summary = "Add Service Account", description = "Create a non-human service account principal under the current tenant for API/MCP access. "
            + "The named owner principal must already be a tenant member; returns the create result.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false"),
                    @ExtensionProperty(name = "hidden", value = "true")
            }))
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody ServiceAccountVO entityVO) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            ServiceAccountBO entityBO = serviceAccountBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(header.getTenantId());
            tenantMembershipService.requireTenantMember(header.getTenantId(), entityBO.getOwnerPrincipalId());
            fillCreateAudit(entityBO, header);
            serviceAccountService.add(entityBO);
            return R.ok(SuccessCode.ADD);
        }));
    }

    /**
     * Delete a service account by ID, scoped to the current tenant.
     *
     * @param id id of the service account to delete
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('service_account', 'delete')")
    @Operation(summary = "Delete Service Account", description = "Delete a service account by ID, scoped to the current tenant. "
            + "Removes the machine principal so its credentials can no longer authenticate; returns the delete result.",
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
            ServiceAccountBO entityBO = requireTenant(tenantId, serviceAccountService.getById(id));
            serviceAccountService.delete(entityBO.getId());
            return R.ok(SuccessCode.DELETE);
        }));
    }

    /**
     * Update a tenant-scoped service account's editable attributes.
     *
     * @param entityVO service account payload to apply
     * @return update-success status
     */
    @PreAuthorize("@perm.can('service_account', 'update')")
    @Operation(summary = "Update Service Account", description = "Update a tenant-scoped service account's name, purpose, expiry, owner or credential policy. "
            + "Tenant and ID are taken from the existing record; any new owner principal must be a tenant member.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody ServiceAccountVO entityVO) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            ServiceAccountBO current = requireTenant(header.getTenantId(), serviceAccountService.getById(entityVO.getId()));
            ServiceAccountBO entityBO = serviceAccountBuilder.buildBOByVO(entityVO);
            entityBO.setId(current.getId());
            entityBO.setTenantId(current.getTenantId());
            Long ownerPrincipalId = Objects.requireNonNullElse(entityBO.getOwnerPrincipalId(),
                    current.getOwnerPrincipalId());
            tenantMembershipService.requireTenantMember(header.getTenantId(), ownerPrincipalId);
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            serviceAccountService.update(entityBO);
            return R.ok(SuccessCode.UPDATE);
        }));
    }

    /**
     * Set a service account's enable flag to ENABLE so its credentials can authenticate.
     *
     * @param id id of the service account to enable
     * @return update-success status
     */
    @PreAuthorize("@perm.can('service_account', 'update')")
    @Operation(summary = "Enable Service Account", description = "Set a service account's enable flag to ENABLE so its credentials can authenticate again. "
            + "Tenant-scoped and audit-logged as an ENABLE action.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/enable")
    public Mono<R<String>> enable(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return toggleEnableFlag(id, EnableFlagEnum.ENABLE);
    }

    /**
     * Set a service account's enable flag to DISABLE to block its credentials from authenticating.
     *
     * @param id id of the service account to disable
     * @return update-success status
     */
    @PreAuthorize("@perm.can('service_account', 'update')")
    @Operation(summary = "Disable Service Account", description = "Set a service account's enable flag to DISABLE to block its credentials from authenticating. "
            + "Tenant-scoped and audit-logged as a DISABLE action.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/disable")
    public Mono<R<String>> disable(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return toggleEnableFlag(id, EnableFlagEnum.DISABLE);
    }

    /**
     * Toggle a service account's enable flag via the full update path (keeping the
     * linked Principal in sync) and record an audit entry.
     *
     * @param id     the service account id
     * @param target the target enable flag
     * @return update-success result
     */
    private Mono<R<String>> toggleEnableFlag(Long id, EnableFlagEnum target) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            // Reuse the full update path so the linked Principal row stays in sync; only the
            // enable flag flips, every other column is carried from the current record.
            ServiceAccountBO current = requireTenant(header.getTenantId(), serviceAccountService.getById(id));
            current.setEnableFlag(target);
            current.setOperatorId(header.getUserId());
            current.setOperatorName(header.getNickName());
            serviceAccountService.update(current);
            auditLogService.log(header, target == EnableFlagEnum.ENABLE ? "ENABLE" : "DISABLE",
                    "service_account", id, current.getServiceAccountName(), "SUCCESS", null);
            return R.ok(SuccessCode.UPDATE);
        }));
    }

    /**
     * Fetch one service account by ID, scoped to the current tenant.
     *
     * @param id id of the service account to retrieve
     * @return the matched ServiceAccountVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('service_account', 'get')")
    @Operation(summary = "Get Service Account by ID", description = "Fetch one service account by ID, scoped to the current tenant. "
            + "Returns the principal, owner, purpose, expiry and enable flag; use to inspect an account before rotating its credentials.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<R<ServiceAccountVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            ServiceAccountBO entityBO = requireTenant(tenantId, serviceAccountService.getById(id));
            return R.ok(serviceAccountBuilder.buildVOByBO(entityBO));
        }));
    }

    /**
     * Page through service accounts for the current tenant with optional filters.
     *
     * @param entityQuery optional service account query filters (tenant id is set server-side)
     * @return a page of ServiceAccountVO matching the query
     */
    @PreAuthorize("@perm.can('service_account', 'list')")
    @Operation(summary = "List Service Accounts", description = "Page through service accounts for the current tenant with the supplied query filters. "
            + "Returns a page of service accounts; use to browse machine principals or select a target account.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<R<Page<ServiceAccountVO>>> list(@RequestBody(required = false) ServiceAccountQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            ServiceAccountQuery query = Objects.isNull(entityQuery) ? new ServiceAccountQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<ServiceAccountBO> entityPageBO = serviceAccountService.list(query);
            return R.ok(serviceAccountBuilder.buildVOPageByBOPage(entityPageBO));
        }));
    }

    /**
     * Stamp the creator and operator audit fields from the authenticated principal.
     *
     * @param entityBO the service account to stamp
     * @param header   the authenticated principal header
     */
    private void fillCreateAudit(ServiceAccountBO entityBO, RequestHeader.PrincipalHeader header) {
        entityBO.setCreatorId(header.getUserId());
        entityBO.setCreatorName(header.getNickName());
        entityBO.setOperatorId(header.getUserId());
        entityBO.setOperatorName(header.getNickName());
    }

}
