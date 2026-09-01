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
import io.github.pnoker.common.manager.entity.bo.LabelBO;
import io.github.pnoker.common.manager.entity.builder.LabelBuilder;
import io.github.pnoker.common.manager.entity.query.LabelListRequest;
import io.github.pnoker.common.manager.entity.vo.LabelVO;
import io.github.pnoker.common.manager.repository.LabelFilter;
import io.github.pnoker.common.manager.service.ReactiveLabelService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller exposing label management endpoints.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Tag(name = "label", description = "Label definitions: create, update, and manage labels for categorizing and tagging devices, drivers, and other platform entities with flexible key-value metadata")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.LABEL_URL_PREFIX)
@RequiredArgsConstructor
public class LabelController implements BaseController {

    private final LabelBuilder labelBuilder;

    private final ReactiveLabelService labelService;

    /**
     * Create a label for the current tenant.
     *
     * @param entityVO label payload to create (name, color, entity type)
     * @return add-success status
     */
    @PreAuthorize("@perm.can('label', 'add')")
    @Operation(summary = "Add Label", description = "Create a label for the current tenant. A label is a tag used to filter and organize devices, drivers, points and other entities; returns a success result.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<ResponseEntity<LabelVO>> add(@Validated(Add.class) @RequestBody LabelVO entityVO) {
        return getTenantId().zipWith(getUserId().defaultIfEmpty(0L)).zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> {
            LabelBO entityBO = labelBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tuple.getT1().getT1()); entityBO.setCreatorId(tuple.getT1().getT2());
            entityBO.setCreatorName(tuple.getT2()); entityBO.setOperatorId(tuple.getT1().getT2()); entityBO.setOperatorName(tuple.getT2());
            return labelService.add(entityBO).map(labelBuilder::buildVOByBO)
                    .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created));
        });
    }

    /**
     * Delete a label by ID.
     *
     * @param id id of the label to delete (must be tenant-owned)
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('label', 'delete')")
    @Operation(summary = "Delete Label", description = "Permanently delete a label by ID (tenant-scoped). The label is removed from the tenant; entities previously tagged with it are unaffected but lose the tag association. This action cannot be undone.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @DeleteMapping("/delete")
    public Mono<ResponseEntity<Void>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().zipWith(getUserId().defaultIfEmpty(0L)).zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> labelService.delete(tuple.getT1().getT1(), id, tuple.getT1().getT2(), tuple.getT2())
                        .thenReturn(ResponseEntity.noContent().build()));
    }

    /**
     * Update an existing label's fields.
     *
     * @param entityVO label payload to update (must carry an existing id)
     * @return update-success status
     */
    @PreAuthorize("@perm.can('label', 'update')")
    @Operation(summary = "Update Label", description = "Update an existing label's fields for the current tenant. Ownership is verified before applying the change; returns a success result.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/update")
    public Mono<ResponseEntity<LabelVO>> update(@Validated(Update.class) @RequestBody LabelVO entityVO) {
        return getTenantId().zipWith(getUserId().defaultIfEmpty(0L)).zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> {
            LabelBO entityBO = labelBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tuple.getT1().getT1()); entityBO.setOperatorId(tuple.getT1().getT2()); entityBO.setOperatorName(tuple.getT2());
            return labelService.update(entityBO).map(labelBuilder::buildVOByBO).map(ResponseEntity::ok);
        });
    }

    /**
     * Fetch a single label by ID.
     *
     * @param id id of the label to fetch (must be tenant-owned)
     * @return the matched LabelVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('label', 'get')")
    @Operation(summary = "Get Label by ID", description = "Fetch one label by ID (tenant-scoped). Use to inspect a label's name and color before applying it to or filtering entities.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<LabelVO> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> labelService.getById(tenantId, id)).map(labelBuilder::buildVOByBO);
    }

    /**
     * Page through labels with filters.
     *
     * @param request query filters (may be null)
     * @return a page of LabelVO matching the query
     */
    @PreAuthorize("@perm.can('label', 'list')")
    @Operation(summary = "List Labels", description = "Page through labels for the current tenant with filters from the query body. Returns a page of labels; use to browse available tags or pick one to apply to an entity.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<OffsetPage<LabelVO>> list(@RequestBody(required = false) LabelListRequest request) {
        LabelListRequest query = request == null ? new LabelListRequest() : request;
        return getTenantId().flatMap(tenantId -> labelService.list(new LabelFilter(tenantId, query.labelName(),
                        query.color(), query.entityTypeFlag(), query.enableFlag(), query.offset(), query.limit(), query.sort())))
                .map(page -> new OffsetPage<>(page.items().stream().map(labelBuilder::buildVOByBO).toList(),
                        page.offset(), page.limit(), page.total(), page.hasNext()));
    }

}
