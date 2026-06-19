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
import io.github.pnoker.common.auth.entity.bo.TenantBO;
import io.github.pnoker.common.auth.entity.builder.TenantBuilder;
import io.github.pnoker.common.auth.entity.query.TenantQuery;
import io.github.pnoker.common.auth.entity.vo.TenantVO;
import io.github.pnoker.common.auth.service.TenantService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ErrorCode;
import io.github.pnoker.common.enums.SuccessCode;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
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
 * REST controller exposing tenant management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "tenant", description = "Tenant registration and configuration: manage tenant lifecycles including creation, update, enablement, and multi-tenant isolation settings")
@Slf4j
@RestController
@RequestMapping(AuthConstant.TENANT_URL_PREFIX)
@RequiredArgsConstructor
public class TenantController implements BaseController {

    private final TenantBuilder tenantBuilder;

    private final TenantService tenantService;

    /**
     * Create a new tenant as a multi-tenant isolation boundary (system administrators only).
     *
     * @param entityVO tenant payload to create
     * @return add-success status
     */
    @PreAuthorize("@perm.can('tenant', 'add')")
    @Operation(summary = "Add Tenant", description = "Create a new tenant as the multi-tenant isolation boundary that owns its users, devices and data. Restricted to system administrators; returns the new tenant code.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody TenantVO entityVO) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            TenantBO userTenant = tenantService.getById(header.getTenantId());
            if (!"default".equals(userTenant.getTenantCode())) {
                throw new ServiceException("Only system administrators can create tenants");
            }
            TenantBO entityBO = tenantBuilder.buildBOByVO(entityVO);
            entityBO.setCreatorId(header.getUserId());
            entityBO.setCreatorName(header.getNickName());
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            tenantService.add(entityBO);
            return R.ok(SuccessCode.ADD);
        }));
    }

    /**
     * Remove a tenant by ID; non-administrators may delete only their own tenant.
     *
     * @param id id of the tenant to delete
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('tenant', 'delete')")
    @Operation(summary = "Delete Tenant", description = "Remove a tenant by ID, deleting the isolation boundary and its owned data. Non-administrators may delete only their own tenant.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the tenant to delete. System administrators may delete any tenant; non-administrators may only delete their own tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            TenantBO userTenant = tenantService.getById(tenantId);
            boolean isSystemAdmin = "default".equals(userTenant.getTenantCode());
            if (!isSystemAdmin && !Objects.equals(id, tenantId)) {
                throw new ServiceException("Access denied: cannot delete another tenant.");
            }
            tenantService.delete(id);
            return R.ok(SuccessCode.DELETE);
        }));
    }

    /**
     * Modify a tenant's editable attributes; non-administrators may update only their own tenant.
     *
     * @param entityVO tenant payload to apply
     * @return update-success status
     */
    @PreAuthorize("@perm.can('tenant', 'update')")
    @Operation(summary = "Update Tenant", description = "Modify a tenant's editable attributes such as name and enable flag. Non-administrators may update only their own tenant.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody TenantVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            TenantBO userTenant = tenantService.getById(tenantId);
            boolean isSystemAdmin = "default".equals(userTenant.getTenantCode());
            if (!isSystemAdmin && !Objects.equals(entityVO.getId(), tenantId)) {
                throw new ServiceException("Access denied: cannot update another tenant.");
            }
            TenantBO entityBO = tenantBuilder.buildBOByVO(entityVO);
            tenantService.update(entityBO);
            return R.ok(SuccessCode.UPDATE);
        }));
    }

    /**
     * Fetch one tenant by ID; non-administrators are scoped to their own tenant.
     *
     * @param id id of the tenant to retrieve
     * @return the matched TenantVO; non-administrators get not-found for any other tenant
     */
    @PreAuthorize("@perm.can('tenant', 'get')")
    @Operation(summary = "Get Tenant by ID", description = "Fetch one tenant by its primary key. Non-administrators are scoped to their own tenant and get a not-found result for any other ID.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<R<TenantVO>> getById(@Parameter(description = "Primary key of the tenant to retrieve. System administrators may query any tenant; non-administrators may only query their own tenant ID.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            TenantBO userTenant = tenantService.getById(tenantId);
            boolean isSystemAdmin = "default".equals(userTenant.getTenantCode());
            if (!isSystemAdmin && !Objects.equals(id, tenantId)) {
                throw new NotFoundException("Resource does not exist");
            }
            TenantBO entityBO = tenantService.getById(id);
            TenantVO entityVO = tenantBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Look up a tenant by its unique code; non-administrators are scoped to their own tenant.
     *
     * @param code unique business code of the tenant to retrieve
     * @return the matched TenantVO, or a no-resource failure when not found or out of scope
     */
    @PreAuthorize("@perm.can('tenant', 'get')")
    @Operation(summary = "Get Tenant by Code", description = "Look up a tenant by its unique tenant code. Non-administrators are scoped to their own tenant and receive a not-found result for any other code.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_code")
    public Mono<R<TenantVO>> getByCode(@Parameter(description = "Unique business code assigned to the tenant at creation time. System administrators may query any code; non-administrators receive not-found for codes outside their own tenant.", example = "tenant-001") @NotNull @RequestParam(value = "code") String code) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            TenantBO userTenant = tenantService.getById(tenantId);
            boolean isSystemAdmin = "default".equals(userTenant.getTenantCode());
            TenantBO select = tenantService.getByCode(code);
            if (Objects.isNull(select)) {
                return R.fail(ErrorCode.NOT_FOUND);
            }
            if (!isSystemAdmin && !Objects.equals(select.getId(), tenantId)) {
                return R.fail("Resource does not exist");
            }
            return R.ok(tenantBuilder.buildVOByBO(select));
        }));
    }

    /**
     * Page through tenants matching query filters; non-administrators see only their own.
     *
     * @param entityQuery optional tenant query filters
     * @return a page of TenantVO visible to the caller
     */
    @PreAuthorize("@perm.can('tenant', 'list')")
    @Operation(summary = "List Tenants", description = "Page through tenants matching the query filters (tenant code, name, enable flag). System administrators see all tenants; others see only their own.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<R<Page<TenantVO>>> list(@RequestBody(required = false) TenantQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            TenantBO userTenant = tenantService.getById(tenantId);
            boolean isSystemAdmin = "default".equals(userTenant.getTenantCode());
            TenantQuery query = Objects.isNull(entityQuery) ? new TenantQuery() : entityQuery;
            if (!isSystemAdmin) {
                query.setTenantCode(userTenant.getTenantCode());
            }
            Page<TenantBO> entityPageBO = tenantService.list(query);
            Page<TenantVO> entityPageVO = tenantBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}
