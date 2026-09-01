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

import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.manager.entity.bo.LabelBindBO;
import io.github.pnoker.common.manager.entity.builder.LabelBindBuilder;
import io.github.pnoker.common.manager.entity.query.LabelBindListRequest;
import io.github.pnoker.common.manager.entity.vo.LabelBindVO;
import io.github.pnoker.common.manager.repository.BindingFilter;
import io.github.pnoker.common.manager.service.ReactiveLabelBindService;
import io.github.pnoker.common.manager.service.ReactiveEntityTenantService;
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
import org.springframework.http.HttpStatus;
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
 * REST controller exposing label binding management endpoints.
 *
 * @author pnoker
 * @since 2026.5.11
 */
@Tag(name = "label_bind", description = "Label tag bindings: assign and remove labels on devices, drivers, and other entities to enable categorization and filtered queries")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.LABEL_BIND_URL_PREFIX)
@RequiredArgsConstructor
public class LabelBindController implements BaseController {

    private final LabelBindBuilder labelBindBuilder;

    private final ReactiveLabelBindService labelBindService;

    private final ReactiveEntityTenantService entityTenantService;

    /**
     * Attach a label to an entity for the current tenant.
     *
     * @param entityVO label binding payload to create (label id, entity id, entity type)
     * @return the created label binding
     */
    @PreAuthorize("@perm.can('label_bind', 'add')")
    @Operation(summary = "Add Label Binding", description = "Attach a label to an entity for the current tenant. The label and target entity must share the same entity type and tenant; returns the created binding.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<ResponseEntity<LabelBindVO>> add(@Validated(Add.class) @RequestBody LabelBindVO entityVO) {
        return getTenantId()
                .zipWith(getUserId().defaultIfEmpty(0L))
                .zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> {
                    LabelBindBO entityBO = labelBindBuilder.buildBOByVO(entityVO);
                    entityBO.setTenantId(tuple.getT1().getT1());
                    entityBO.setCreatorId(tuple.getT1().getT2());
                    entityBO.setCreatorName(tuple.getT2());
                    entityBO.setOperatorId(tuple.getT1().getT2());
                    entityBO.setOperatorName(tuple.getT2());
                    return entityTenantService.requireEntityTenant(entityBO.getTenantId(), entityBO.getEntityTypeFlag(), entityBO.getEntityId())
                            .then(labelBindService.add(entityBO))
                            .map(labelBindBuilder::buildVOByBO)
                            .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created));
                });
    }

    /**
     * Remove a label binding by ID.
     *
     * @param id id of the label binding to delete (must be tenant-owned)
     * @return an empty response after deletion
     */
    @PreAuthorize("@perm.can('label_bind', 'delete')")
    @Operation(summary = "Delete Label Binding", description = "Permanently remove one tenant-owned label binding by ID.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @DeleteMapping("/delete")
    public Mono<ResponseEntity<Void>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId()
                .zipWith(getUserId().defaultIfEmpty(0L))
                .zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> labelBindService.delete(tuple.getT1().getT1(), id, tuple.getT1().getT2(), tuple.getT2())
                        .thenReturn(ResponseEntity.noContent().build()));
    }

    /**
     * Modify an existing label binding.
     *
     * @param entityVO label binding payload to update (must carry an existing id)
     * @return the updated label binding
     */
    @PreAuthorize("@perm.can('label_bind', 'update')")
    @Operation(summary = "Update Label Binding", description = "Modify an existing label binding for the current tenant. The new label and entity must match the binding's entity type and tenant scope.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/update")
    public Mono<ResponseEntity<LabelBindVO>> update(@Validated(Update.class) @RequestBody LabelBindVO entityVO) {
        return getTenantId()
                .zipWith(getUserId().defaultIfEmpty(0L))
                .zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> {
                    LabelBindBO entityBO = labelBindBuilder.buildBOByVO(entityVO);
                    entityBO.setTenantId(tuple.getT1().getT1());
                    entityBO.setOperatorId(tuple.getT1().getT2());
                    entityBO.setOperatorName(tuple.getT2());
                    return entityTenantService.requireEntityTenant(entityBO.getTenantId(), entityBO.getEntityTypeFlag(), entityBO.getEntityId())
                            .then(labelBindService.update(entityBO))
                            .map(labelBindBuilder::buildVOByBO)
                            .map(ResponseEntity::ok);
                });
    }

    /**
     * Fetch a single label binding by ID.
     *
     * @param id id of the label binding to fetch (must be tenant-owned)
     * @return the matched label binding
     */
    @PreAuthorize("@perm.can('label_bind', 'get')")
    @Operation(summary = "Get Label Binding by ID", description = "Fetch one label binding by ID for the current tenant.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<LabelBindVO> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId()
                .flatMap(tenantId -> labelBindService.getById(tenantId, id))
                .map(labelBindBuilder::buildVOByBO);
    }

    /**
     * Page through label bindings with filters.
     *
     * @param request query filters (may be null)
     * @return a page of label bindings matching the query
     */
    @PreAuthorize("@perm.can('label_bind', 'list')")
    @Operation(summary = "List Label Bindings", description = "Page through label bindings for the current tenant with optional label, entity type, and entity filters.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<OffsetPage<LabelBindVO>> list(@RequestBody(required = false) LabelBindListRequest request) {
        LabelBindListRequest query = request == null ? new LabelBindListRequest() : request;
        return getTenantId()
                .flatMap(tenantId -> labelBindService.list(new BindingFilter(
                        tenantId,
                        query.entityTypeFlag(),
                        query.labelId(),
                        query.entityId(),
                        query.offset(),
                        query.limit(),
                        query.sort())))
                .map(page -> new OffsetPage<>(
                        page.items().stream().map(labelBindBuilder::buildVOByBO).toList(),
                        page.offset(),
                        page.limit(),
                        page.total(),
                        page.hasNext()));
    }

}
