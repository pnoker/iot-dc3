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

package io.github.pnoker.common.manager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.dal.entity.bo.LabelBO;
import io.github.pnoker.common.dal.entity.bo.LabelBindBO;
import io.github.pnoker.common.dal.entity.builder.LabelBindBuilder;
import io.github.pnoker.common.dal.entity.query.LabelBindQuery;
import io.github.pnoker.common.dal.entity.vo.LabelBindVO;
import io.github.pnoker.common.dal.service.LabelBindService;
import io.github.pnoker.common.dal.service.LabelService;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.enums.SuccessCode;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.service.EntityTenantService;
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
 * REST controller exposing label binding management endpoints.
 *
 * @author pnoker
 * @version 2026.5.11
 * @since 2026.5.11
 */
@Tag(name = "label_bind", description = "Label tag bindings: assign and remove labels on devices, drivers, and other entities to enable categorization and filtered queries")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.LABEL_BIND_URL_PREFIX)
@RequiredArgsConstructor
public class LabelBindController implements BaseController {

    private final LabelBindBuilder labelBindBuilder;

    private final LabelBindService labelBindService;

    private final LabelService labelService;

    private final EntityTenantService entityTenantService;

    /**
     * Attach a label to an entity for the current tenant.
     *
     * @param entityVO label binding payload to create (label id, entity id, entity type)
     * @return add-success status
     */
    @PreAuthorize("@perm.can('label_bind', 'add')")
    @Operation(summary = "Add Label Binding", description = "Attach a label to an entity (device, driver, etc.) for the current tenant. The label and target entity must share the same entity type and belong to the tenant; returns the add result.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody LabelBindVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LabelBindBO entityBO = labelBindBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            validateBind(tenantId, entityBO);
            labelBindService.add(entityBO);
            return R.ok(SuccessCode.ADD);
        }));
    }

    /**
     * Remove a label binding by ID.
     *
     * @param id id of the label binding to delete (must be tenant-owned)
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('label_bind', 'delete')")
    @Operation(summary = "Delete Label Binding", description = "Remove a label binding by ID (tenant-scoped). Deletes only the association, leaving the label and the bound entity intact.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, labelBindService.getById(id));
            labelBindService.delete(id);
            return R.ok(SuccessCode.DELETE);
        }));
    }

    /**
     * Modify an existing label binding (re-point it to another label or entity).
     *
     * @param entityVO label binding payload to update (must carry an existing id)
     * @return update-success status
     */
    @PreAuthorize("@perm.can('label_bind', 'update')")
    @Operation(summary = "Update Label Binding", description = "Modify an existing label binding for the current tenant, such as re-pointing it to another label or entity. The new label and entity must match the binding's entity type and tenant scope.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody LabelBindVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LabelBindBO entityBO = labelBindBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, labelBindService.getById(entityBO.getId()));
            validateBind(tenantId, entityBO);
            labelBindService.update(entityBO);
            return R.ok(SuccessCode.UPDATE);
        }));
    }

    /**
     * Fetch a single label binding by ID.
     *
     * @param id id of the label binding to fetch (must be tenant-owned)
     * @return the matched LabelBindVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('label_bind', 'get')")
    @Operation(summary = "Get Label Binding by ID", description = "Fetch one label binding by ID (tenant-scoped). Use to inspect which label is attached to which entity and its entity type.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<R<LabelBindVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LabelBindBO entityBO = requireTenant(tenantId, labelBindService.getById(id));
            LabelBindVO entityVO = labelBindBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Page through label bindings with filters.
     *
     * @param entityQuery query filters (may be null)
     * @return a page of LabelBindVO matching the query
     */
    @PreAuthorize("@perm.can('label_bind', 'list')")
    @Operation(summary = "List Label Bindings", description = "Page through label bindings for the current tenant with filters from the query body. Returns a page of bindings; use to discover which entities carry a given label.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<R<Page<LabelBindVO>>> list(@RequestBody(required = false) LabelBindQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LabelBindQuery query = Objects.isNull(entityQuery) ? new LabelBindQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<LabelBindBO> entityPageBO = labelBindService.list(query);
            Page<LabelBindVO> entityPageVO = labelBindBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    /**
     * Validate a label binding: the label belongs to the tenant, its type matches the
     * entity type, and the bound entity itself belongs to the tenant.
     *
     * @param tenantId tenant scope
     * @param entityBO the binding to validate
     */
    private void validateBind(Long tenantId, LabelBindBO entityBO) {
        EntityTypeEnum entityTypeFlag = entityBO.getEntityTypeFlag();
        LabelBO labelBO = requireTenant(tenantId, labelService.getById(entityBO.getLabelId()));
        if (!Objects.equals(labelBO.getEntityTypeFlag(), entityTypeFlag)) {
            throw new NotFoundException("Resource does not exist");
        }
        entityTenantService.requireEntityTenant(tenantId, entityTypeFlag, entityBO.getEntityId());
    }

}
