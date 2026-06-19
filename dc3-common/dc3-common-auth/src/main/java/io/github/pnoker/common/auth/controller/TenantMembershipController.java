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
import io.github.pnoker.common.auth.entity.bo.TenantMembershipBO;
import io.github.pnoker.common.auth.entity.builder.TenantMembershipBuilder;
import io.github.pnoker.common.auth.entity.query.TenantMembershipQuery;
import io.github.pnoker.common.auth.entity.vo.TenantMembershipVO;
import io.github.pnoker.common.auth.service.AuditLogService;
import io.github.pnoker.common.auth.service.TenantMembershipService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.MembershipStatusEnum;
import io.github.pnoker.common.enums.SuccessCode;
import io.github.pnoker.common.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * REST controller exposing tenant membership management endpoints. Tenant-scoped: list and add are
 * always pinned to the caller's tenant, and delete verifies ownership before removing.
 *
 * @author pnoker
 * @version 2026.6.13
 * @since 2026.6.13
 */
@Tag(name = "tenant_membership", description = "Tenant membership bindings: associate users, service accounts, and roles with tenants for access control within tenant scope")
@Slf4j
@RestController
@RequestMapping(AuthConstant.TENANT_MEMBERSHIP_URL_PREFIX)
@RequiredArgsConstructor
public class TenantMembershipController implements BaseController {

    private final TenantMembershipBuilder tenantMembershipBuilder;

    private final TenantMembershipService tenantMembershipService;

    private final AuditLogService auditLogService;

    /**
     * Page through the memberships of the caller's tenant.
     *
     * @param entityQuery optional membership query filters (tenant id is pinned server-side)
     * @return a page of TenantMembershipVO belonging to the caller's tenant
     */
    @PreAuthorize("@perm.can('tenant_membership', 'list')")
    @Operation(summary = "List Tenant Memberships", description = "Page through the memberships of the caller's tenant, optionally filtered by query. " +
            "Each row links a principal (user or service account) to the tenant; use to see who belongs to it.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<R<Page<TenantMembershipVO>>> list(@RequestBody(required = false) TenantMembershipQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            TenantMembershipQuery query = Objects.isNull(entityQuery) ? new TenantMembershipQuery() : entityQuery;
            query.setTenantId(tenantId);
            return R.ok(tenantMembershipBuilder.buildVOPageByBOPage(tenantMembershipService.list(query)));
        }));
    }

    /**
     * Attach a principal to the caller's tenant (body tenant id is ignored).
     *
     * @param entityVO membership payload to create
     * @return add-success status
     */
    @PreAuthorize("@perm.can('tenant_membership', 'add')")
    @Operation(summary = "Add Tenant Membership", description = "Attach a principal (user or service account) to the caller's tenant. " +
            "The body tenant id is ignored and the membership is pinned to the caller; defaults to ACTIVE status and records an audit entry.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<R<String>> add(@RequestBody TenantMembershipVO entityVO) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            TenantMembershipBO entityBO = tenantMembershipBuilder.buildBOByVO(entityVO);
            // Pin to the caller's tenant — the body tenant id is ignored.
            entityBO.setTenantId(header.getTenantId());
            if (Objects.isNull(entityBO.getMembershipStatus())) {
                entityBO.setMembershipStatus(MembershipStatusEnum.ACTIVE);
            }
            entityBO.setCreatorId(header.getUserId());
            entityBO.setCreatorName(header.getNickName());
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            tenantMembershipService.add(entityBO);
            auditLogService.log(header, "CREATE", "tenant_membership", entityBO.getPrincipalId(),
                    null, "SUCCESS", null);
            return R.ok(SuccessCode.ADD);
        }));
    }

    /**
     * Remove a membership record by ID after verifying it belongs to the caller's tenant.
     *
     * @param id id of the membership to delete
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('tenant_membership', 'delete')")
    @Operation(summary = "Delete Tenant Membership", description = "Remove a membership record by ID, verifying it belongs to the caller's tenant first. " +
            "Returns 404 if the membership is owned by another tenant; logs a DELETE audit entry on success.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            // Verify the membership belongs to the caller's tenant before deleting.
            Long tenantId = header.getTenantId();
            TenantMembershipBO current = tenantMembershipService.getById(id);
            if (!Objects.equals(current.getTenantId(), tenantId)) {
                throw new NotFoundException("Tenant membership does not exist");
            }
            tenantMembershipService.delete(id);
            auditLogService.log(header, "DELETE", "tenant_membership", id, null, "SUCCESS", null);
            return R.ok(SuccessCode.DELETE);
        }));
    }
}
