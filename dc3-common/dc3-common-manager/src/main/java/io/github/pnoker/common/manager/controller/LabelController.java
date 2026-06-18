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
import io.github.pnoker.common.dal.entity.builder.LabelBuilder;
import io.github.pnoker.common.dal.entity.query.LabelQuery;
import io.github.pnoker.common.dal.entity.vo.LabelVO;
import io.github.pnoker.common.dal.service.LabelService;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.SuccessCode;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * REST controller exposing label management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "label", description = "Label definitions: create, update, and manage labels for categorizing and tagging devices, drivers, and other platform entities with flexible key-value metadata")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.LABEL_URL_PREFIX)
@RequiredArgsConstructor
public class LabelController implements BaseController {

    private final LabelBuilder labelBuilder;

    private final LabelService labelService;

    /**
     * Create a label for the current tenant.
     *
     * @param entityVO label payload to create (name, color, entity type)
     * @return add-success status
     */
    @PreAuthorize("@perm.can('label', 'add')")
    @Operation(summary = "Add Label", description = "Create a label for the current tenant. A label is a tag used to filter and organize devices, drivers, points and other entities; returns a success result.")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody LabelVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LabelBO entityBO = labelBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            labelService.add(entityBO);
            return R.ok(SuccessCode.ADD);
        }));
    }

    /**
     * Delete a label by ID.
     *
     * @param id id of the label to delete (must be tenant-owned)
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('label', 'delete')")
    @Operation(summary = "Delete Label", description = "Permanently delete a label by ID (tenant-scoped). The label is removed from the tenant; entities previously tagged with it are unaffected but lose the tag association. This action cannot be undone.")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, labelService.getById(id));
            labelService.delete(id);
            return R.ok(SuccessCode.DELETE);
        }));
    }

    /**
     * Update an existing label's fields.
     *
     * @param entityVO label payload to update (must carry an existing id)
     * @return update-success status
     */
    @PreAuthorize("@perm.can('label', 'update')")
    @Operation(summary = "Update Label", description = "Update an existing label's fields for the current tenant. Ownership is verified before applying the change; returns a success result.")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody LabelVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LabelBO entityBO = labelBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, labelService.getById(entityBO.getId()));
            labelService.update(entityBO);
            return R.ok(SuccessCode.UPDATE);
        }));
    }

    /**
     * Fetch a single label by ID.
     *
     * @param id id of the label to fetch (must be tenant-owned)
     * @return the matched LabelVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('label', 'get')")
    @Operation(summary = "Get Label by ID", description = "Fetch one label by ID (tenant-scoped). Use to inspect a label's name and color before applying it to or filtering entities.")
    @GetMapping("/get_by_id")
    public Mono<R<LabelVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LabelBO entityBO = requireTenant(tenantId, labelService.getById(id));
            LabelVO entityVO = labelBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Page through labels with filters.
     *
     * @param entityQuery query filters (may be null)
     * @return a page of LabelVO matching the query
     */
    @PreAuthorize("@perm.can('label', 'list')")
    @Operation(summary = "List Labels", description = "Page through labels for the current tenant with filters from the query body. Returns a page of labels; use to browse available tags or pick one to apply to an entity.")
    @PostMapping("/list")
    public Mono<R<Page<LabelVO>>> list(@RequestBody(required = false) LabelQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LabelQuery query = Objects.isNull(entityQuery) ? new LabelQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<LabelBO> entityPageBO = labelService.list(query);
            Page<LabelVO> entityPageVO = labelBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}
