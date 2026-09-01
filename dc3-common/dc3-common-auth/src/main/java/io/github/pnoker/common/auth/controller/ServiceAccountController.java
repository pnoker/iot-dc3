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

import io.github.pnoker.common.auth.entity.bo.ServiceAccountBO;
import io.github.pnoker.common.auth.entity.builder.ServiceAccountBuilder;
import io.github.pnoker.common.auth.entity.query.ServiceAccountOffsetRequest;
import io.github.pnoker.common.auth.entity.vo.ServiceAccountVO;
import io.github.pnoker.common.auth.service.ReactiveAuditLogService;
import io.github.pnoker.common.auth.service.ReactiveServiceAccountService;
import io.github.pnoker.common.auth.service.ReactiveTenantMembershipService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
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
 * REST controller exposing service account management endpoints.
 *
 * @author pnoker
 * @since 2026.6.12
 */
@Tag(name = "service_account", description = "Service account lifecycle: manage machine-to-machine identities including creation, update, credential rotation, and enablement")
@Slf4j
@RestController
@RequestMapping(AuthConstant.SERVICE_ACCOUNT_URL_PREFIX)
@RequiredArgsConstructor
public class ServiceAccountController implements BaseController {

    private final ServiceAccountBuilder serviceAccountBuilder;

    private final ReactiveServiceAccountService serviceAccountService;

    private final ReactiveAuditLogService auditLogService;

    private final ReactiveTenantMembershipService tenantMembershipService;

    /**
     * Create a non-human service account principal under the current tenant.
     *
     * @param entityVO service account payload to create
     * @return add-success status
     */
    @PreAuthorize("@perm.can('service_account', 'add')")
    @Operation(summary = "Add Service Account", description = "Create a non-human service account principal under the current tenant.", extensions = @Extension(name = "x-dc3-ai", properties = {@ExtensionProperty(name = "riskLevel", value = "HIGH"), @ExtensionProperty(name = "destructive", value = "false"), @ExtensionProperty(name = "idempotent", value = "false"), @ExtensionProperty(name = "openWorld", value = "false")}))
    @PostMapping("/add")
    public Mono<ResponseEntity<ServiceAccountVO>> add(@Validated(Add.class) @RequestBody ServiceAccountVO entityVO) {
        return getPrincipalHeader().flatMap(header -> {
            ServiceAccountBO entityBO = serviceAccountBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(header.getTenantId());
            fillCreateAudit(entityBO, header);
            return tenantMembershipService.requireTenantMember(header.getTenantId(), entityBO.getOwnerPrincipalId())
                    .then(serviceAccountService.add(entityBO))
                    .flatMap(saved -> auditLogService.log(header, "CREATE", "service_account", saved.getId(),
                                    saved.getServiceAccountName(), "SUCCESS", null)
                            .thenReturn(ResponseEntity.status(201).body(serviceAccountBuilder.buildVOByBO(saved))));
        });
    }

    @PreAuthorize("@perm.can('service_account', 'delete')")
    @Operation(summary = "Delete Service Account", description = "Delete a tenant-scoped service account and its linked principal and membership.", extensions = @Extension(name = "x-dc3-ai", properties = {@ExtensionProperty(name = "riskLevel", value = "HIGH"), @ExtensionProperty(name = "destructive", value = "true"), @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")}))
    @DeleteMapping("/delete")
    public Mono<ResponseEntity<Void>> delete(@Parameter(description = "Service account identifier owned by the current tenant") @NotNull @RequestParam(value = "id") Long id) {
        return getPrincipalHeader().flatMap(header -> serviceAccountService.getById(header.getTenantId(), id)
                .flatMap(current -> serviceAccountService.delete(header.getTenantId(), id, header.getUserId(), header.getNickName())
                        .then(auditLogService.log(header, "DELETE", "service_account", id,
                                current.getServiceAccountName(), "SUCCESS", null)))
                .thenReturn(ResponseEntity.noContent().build()));
    }

    @PreAuthorize("@perm.can('service_account', 'update')")
    @Operation(summary = "Update Service Account", description = "Update editable service-account attributes within the current tenant.", extensions = @Extension(name = "x-dc3-ai", properties = {@ExtensionProperty(name = "riskLevel", value = "MEDIUM"), @ExtensionProperty(name = "destructive", value = "false"), @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")}))
    @PostMapping("/update")
    public Mono<ResponseEntity<ServiceAccountVO>> update(@Validated(Update.class) @RequestBody ServiceAccountVO entityVO) {
        return getPrincipalHeader().flatMap(header -> serviceAccountService.getById(header.getTenantId(), parseId(entityVO.getId()))
                .flatMap(current -> {
                    ServiceAccountBO update = serviceAccountBuilder.buildBOByVO(entityVO);
                    update.setId(current.getId());
                    update.setTenantId(current.getTenantId());
                    if (update.getServiceAccountName() == null || update.getServiceAccountName().isBlank()) update.setServiceAccountName(current.getServiceAccountName());
                    if (update.getOwnerPrincipalId() == null) update.setOwnerPrincipalId(current.getOwnerPrincipalId());
                    if (update.getPurpose() == null) update.setPurpose(current.getPurpose());
                    if (update.getExpireTime() == null) update.setExpireTime(current.getExpireTime());
                    if (update.getLastUsedTime() == null) update.setLastUsedTime(current.getLastUsedTime());
                    if (update.getEnableFlag() == null) update.setEnableFlag(current.getEnableFlag());
                    if (update.getCredentialPolicyExt() == null) update.setCredentialPolicyExt(current.getCredentialPolicyExt());
                    update.setOperatorId(header.getUserId());
                    update.setOperatorName(header.getNickName());
                    return tenantMembershipService.requireTenantMember(header.getTenantId(), update.getOwnerPrincipalId())
                            .then(serviceAccountService.update(header.getTenantId(), update));
                })
                .flatMap(saved -> auditLogService.log(header, "UPDATE", "service_account", saved.getId(),
                                saved.getServiceAccountName(), "SUCCESS", null)
                        .thenReturn(ResponseEntity.ok(serviceAccountBuilder.buildVOByBO(saved)))));
    }

    @PreAuthorize("@perm.can('service_account', 'update')")
    @Operation(summary = "Enable Service Account", description = "Enable a tenant-scoped service account for authentication.", extensions = @Extension(name = "x-dc3-ai", properties = {@ExtensionProperty(name = "riskLevel", value = "MEDIUM"), @ExtensionProperty(name = "destructive", value = "false"), @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")}))
    @PostMapping("/enable")
    public Mono<ResponseEntity<Void>> enable(@Parameter(description = "Service account identifier owned by the current tenant") @NotNull @RequestParam(value = "id") Long id) {
        return toggleEnableFlag(id, EnableFlagEnum.ENABLE);
    }

    @PreAuthorize("@perm.can('service_account', 'update')")
    @Operation(summary = "Disable Service Account", description = "Disable a tenant-scoped service account and reject future authentication.", extensions = @Extension(name = "x-dc3-ai", properties = {@ExtensionProperty(name = "riskLevel", value = "MEDIUM"), @ExtensionProperty(name = "destructive", value = "true"), @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")}))
    @PostMapping("/disable")
    public Mono<ResponseEntity<Void>> disable(@Parameter(description = "Service account identifier owned by the current tenant") @NotNull @RequestParam(value = "id") Long id) {
        return toggleEnableFlag(id, EnableFlagEnum.DISABLE);
    }

    private Mono<ResponseEntity<Void>> toggleEnableFlag(Long id, EnableFlagEnum target) {
        return getPrincipalHeader().flatMap(header -> serviceAccountService.getById(header.getTenantId(), id)
                .flatMap(current -> {
                    current.setEnableFlag(target);
                    current.setOperatorId(header.getUserId());
                    current.setOperatorName(header.getNickName());
                    return serviceAccountService.update(header.getTenantId(), current)
                            .flatMap(saved -> auditLogService.log(header, target == EnableFlagEnum.ENABLE ? "ENABLE" : "DISABLE",
                                            "service_account", id, saved.getServiceAccountName(), "SUCCESS", null));
                })
                .thenReturn(ResponseEntity.noContent().build()));
    }

    @PreAuthorize("@perm.can('service_account', 'get')")
    @Operation(summary = "Get Service Account by ID", description = "Fetch one service account scoped to the current tenant by identifier.", extensions = @Extension(name = "x-dc3-ai", properties = {@ExtensionProperty(name = "riskLevel", value = "LOW"), @ExtensionProperty(name = "destructive", value = "false"), @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")}))
    @GetMapping("/get_by_id")
    public Mono<ResponseEntity<ServiceAccountVO>> getById(@Parameter(description = "Service account identifier owned by the current tenant") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> serviceAccountService.getById(tenantId, id)
                .map(account -> ResponseEntity.ok(serviceAccountBuilder.buildVOByBO(account))));
    }

    @PreAuthorize("@perm.can('service_account', 'list')")
    @Operation(summary = "List Service Accounts", description = "List tenant-scoped service accounts using deterministic offset pagination and filters.", extensions = @Extension(name = "x-dc3-ai", properties = {@ExtensionProperty(name = "riskLevel", value = "LOW"), @ExtensionProperty(name = "destructive", value = "false"), @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")}))
    @PostMapping("/list")
    public Mono<ResponseEntity<OffsetPage<ServiceAccountVO>>> list(@RequestBody(required = false) ServiceAccountOffsetRequest request) {
        ServiceAccountOffsetRequest query = request == null ? new ServiceAccountOffsetRequest() : request;
        return getTenantId().flatMap(tenantId -> serviceAccountService.list(new io.github.pnoker.common.auth.repository.ServiceAccountFilter(
                        tenantId, null, query.serviceAccountName(), query.ownerPrincipalId(), query.enableFlag(),
                        new io.github.pnoker.db.r2dbc.core.page.PageRequest(query.offset(), query.limit(), query.sort())))
                .map(page -> ResponseEntity.ok(OffsetPage.of(page.items().stream().map(serviceAccountBuilder::buildVOByBO).toList(),
                        page.offset(), page.limit(), page.total()))));
    }

    private Long parseId(String id) {
        try { return Long.valueOf(id); } catch (RuntimeException error) { throw new IllegalArgumentException("service account id is invalid", error); }
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
