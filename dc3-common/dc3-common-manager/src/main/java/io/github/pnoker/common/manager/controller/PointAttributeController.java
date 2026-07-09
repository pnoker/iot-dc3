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
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.SuccessCode;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.common.manager.entity.builder.PointAttributeBuilder;
import io.github.pnoker.common.manager.entity.query.PointAttributeQuery;
import io.github.pnoker.common.manager.entity.vo.PointAttributeVO;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.PointAttributeService;
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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * REST controller exposing point attribute management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "point_attribute", description = "Point attribute definitions: manage configurable properties of data points including name, type, default value, and validation rules")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.POINT_ATTRIBUTE_URL_PREFIX)
@RequiredArgsConstructor
public class PointAttributeController implements BaseController {

    private final PointAttributeBuilder pointAttributeBuilder;

    private final PointAttributeService pointAttributeService;

    private final DriverService driverService;

    /**
     * Declare a new point attribute field for the current tenant on a given driver.
     *
     * @param entityVO point attribute payload to create (name, code, type, default value)
     * @return add-success status
     */
    @PreAuthorize("@perm.can('point_attribute', 'add')")
    @Operation(summary = "Add Point Attribute", description = "Declare a new point attribute field for the current tenant on a given driver. A point attribute is a configurable field definition that tells the driver how to read or write a point's value; returns the new attribute ID.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody PointAttributeVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointAttributeBO entityBO = pointAttributeBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            pointAttributeService.add(entityBO);
            return R.ok(SuccessCode.ADD);
        }));
    }

    /**
     * Delete a point attribute field definition by ID.
     *
     * @param id id of the point attribute to delete (must be tenant-owned)
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('point_attribute', 'delete')")
    @Operation(summary = "Delete Point Attribute", description = "Permanently delete a point attribute field definition by ID (tenant-scoped). Removes the attribute from its driver; the action cannot be undone.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, pointAttributeService.getById(id));
            pointAttributeService.delete(id);
            return R.ok(SuccessCode.DELETE);
        }));
    }

    /**
     * Update an existing point attribute field definition.
     *
     * @param entityVO point attribute payload to update (must carry an existing id)
     * @return update-success status
     */
    @PreAuthorize("@perm.can('point_attribute', 'update')")
    @Operation(summary = "Update Point Attribute", description = "Modify an existing point attribute field definition by ID (tenant-scoped). Use to change a driver-level field's name, code, type, default value or enable flag; ownership is verified before saving.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody PointAttributeVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointAttributeBO entityBO = pointAttributeBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, pointAttributeService.getById(entityBO.getId()));
            pointAttributeService.update(entityBO);
            return R.ok(SuccessCode.UPDATE);
        }));
    }

    /**
     * Fetch a single point attribute field definition by ID.
     *
     * @param id id of the point attribute to fetch (must be tenant-owned)
     * @return the matched PointAttributeVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('point_attribute', 'get')")
    @Operation(summary = "Get Point Attribute by ID", description = "Fetch one point attribute field definition by ID (tenant-scoped). Use to inspect a driver-level field such as its name, code, type, default value and enable flag before editing it.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<R<PointAttributeVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointAttributeBO entityBO = requireTenant(tenantId, pointAttributeService.getById(id));
            PointAttributeVO entityVO = pointAttributeBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * List every point attribute field definition owned by a given driver.
     *
     * @param driverId id of the driver whose point attribute definitions are returned (must be tenant-owned)
     * @return a list of PointAttributeVO for the driver; an empty list when the driver is not found
     */
    @PreAuthorize("@perm.can('point_attribute', 'list')")
    @Operation(summary = "List Point Attributes by Driver ID", description = "Return every point attribute field definition owned by a given driver (tenant-scoped). Use to discover which configurable fields a driver exposes for reading or writing point values; returns an empty list when the driver is not found.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_by_driver_id")
    public Mono<R<List<PointAttributeVO>>> listByDriverId(@Parameter(description = "Identifier of the driver whose point attribute field definitions are returned; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "driver_id") Long driverId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            try {
                requireTenant(tenantId, driverService.getById(driverId));
                List<PointAttributeBO> entityBOList = filterTenant(tenantId, pointAttributeService.listByDriverId(driverId));
                List<PointAttributeVO> entityVO = pointAttributeBuilder.buildVOListByBOList(entityBOList);
                return R.ok(entityVO);
            } catch (NotFoundException ignored) {
                return R.ok(Collections.emptyList());
            }
        }));
    }

    /**
     * Page through point attribute field definitions with filters.
     *
     * @param entityQuery query filters such as name, driver and enable flag (may be null)
     * @return a page of PointAttributeVO matching the query
     */
    @PreAuthorize("@perm.can('point_attribute', 'list')")
    @Operation(summary = "List Point Attributes", description = "Page through point attribute field definitions for the current tenant with filters such as attribute name, driver and enable flag. Returns a page of point attributes; use for browsing or selecting a target attribute.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<R<Page<PointAttributeVO>>> list(@RequestBody(required = false) PointAttributeQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointAttributeQuery query = Objects.isNull(entityQuery) ? new PointAttributeQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<PointAttributeBO> entityPageBO = pointAttributeService.list(query);
            Page<PointAttributeVO> entityPageVO = pointAttributeBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}
