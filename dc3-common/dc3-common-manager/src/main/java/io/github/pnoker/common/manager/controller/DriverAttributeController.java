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
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.manager.entity.builder.DriverAttributeBuilder;
import io.github.pnoker.common.manager.entity.query.DriverAttributeQuery;
import io.github.pnoker.common.manager.entity.vo.DriverAttributeVO;
import io.github.pnoker.common.manager.service.DriverAttributeService;
import io.github.pnoker.common.manager.service.DriverService;
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
 * REST controller exposing driver attribute management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "driver_attribute", description = "Driver attribute definitions: manage configurable properties of protocol drivers including name, type, default value, and validation constraints")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.DRIVER_ATTRIBUTE_URL_PREFIX)
@RequiredArgsConstructor
public class DriverAttributeController implements BaseController {

    private final DriverAttributeBuilder driverAttributeBuilder;

    private final DriverAttributeService driverAttributeService;

    private final DriverService driverService;

    /**
     * Create a driver attribute definition for the current tenant.
     *
     * @param entityVO driver attribute payload to create (name, code, type, default value)
     * @return add-success status
     */
    @PreAuthorize("@perm.can('driver_attribute', 'add')")
    @Operation(summary = "Add Driver Attribute", description = "Define a new driver attribute for the current tenant. A driver attribute is a configurable field " +
            "declared on a driver (name, code, type, default value) that driver instances supply a concrete value for; returns the new attribute ID.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody DriverAttributeVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverAttributeBO entityBO = driverAttributeBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            driverAttributeService.add(entityBO);
            return R.ok(SuccessCode.ADD);
        }));
    }

    /**
     * Delete a driver attribute definition by ID.
     *
     * @param id id of the driver attribute to delete (must be tenant-owned)
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('driver_attribute', 'delete')")
    @Operation(summary = "Delete Driver Attribute", description = "Permanently delete a driver attribute by ID (tenant-scoped). Removes the attribute definition for its " +
            "driver; the action cannot be undone.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, driverAttributeService.getById(id));
            driverAttributeService.delete(id);
            return R.ok(SuccessCode.DELETE);
        }));
    }

    /**
     * Update an existing driver attribute definition.
     *
     * @param entityVO driver attribute payload to update (must carry an existing id)
     * @return update-success status
     */
    @PreAuthorize("@perm.can('driver_attribute', 'update')")
    @Operation(summary = "Update Driver Attribute", description = "Update an existing driver attribute (tenant-scoped). Modifies the field definition's name, code, type, " +
            "default value or enable flag on its driver.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody DriverAttributeVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverAttributeBO entityBO = driverAttributeBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, driverAttributeService.getById(entityBO.getId()));
            driverAttributeService.update(entityBO);
            return R.ok(SuccessCode.UPDATE);
        }));
    }

    /**
     * Fetch a single driver attribute definition by ID.
     *
     * @param id id of the driver attribute to fetch (must be tenant-owned)
     * @return the matched DriverAttributeVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('driver_attribute', 'get')")
    @Operation(summary = "Get Driver Attribute by ID", description = "Fetch one driver attribute by ID (tenant-scoped). Use to inspect a driver's configurable field " +
            "definition such as its type and default value before configuring driver attribute values.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<R<DriverAttributeVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverAttributeBO entityBO = requireTenant(tenantId, driverAttributeService.getById(id));
            DriverAttributeVO entityVO = driverAttributeBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * List every driver attribute declared on a given driver.
     *
     * @param driverId id of the driver whose declared attributes are returned (must be tenant-owned)
     * @return a list of DriverAttributeVO for the driver; an empty list when the driver is not found
     */
    @PreAuthorize("@perm.can('driver_attribute', 'list')")
    @Operation(summary = "List Driver Attributes by Driver ID", description = "Return every driver attribute declared on a given driver (tenant-scoped). Use to discover which " +
            "configurable fields a driver exposes; returns an empty list when the driver does not exist.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_by_driver_id")
    public Mono<R<List<DriverAttributeVO>>> listByDriverId(@Parameter(description = "Identifier of the driver whose declared attributes are returned; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "driver_id") Long driverId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            try {
                requireTenant(tenantId, driverService.getById(driverId));
                List<DriverAttributeBO> entityBOList = filterTenant(tenantId, driverAttributeService.listByDriverId(driverId));
                List<DriverAttributeVO> entityVO = driverAttributeBuilder.buildVOListByBOList(entityBOList);
                return R.ok(entityVO);
            } catch (NotFoundException ignored) {
                return R.ok(Collections.emptyList());
            }
        }));
    }

    /**
     * Page through driver attribute definitions with filters.
     *
     * @param entityQuery query filters such as name, code and driver (may be null)
     * @return a page of DriverAttributeVO matching the query
     */
    @PreAuthorize("@perm.can('driver_attribute', 'list')")
    @Operation(summary = "List Driver Attributes", description = "Page through driver attributes for the current tenant with filters such as name, code and driver. Returns a " +
            "page of driver attributes; use for browsing or selecting a target attribute.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<R<Page<DriverAttributeVO>>> list(@RequestBody(required = false) DriverAttributeQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverAttributeQuery query = Objects.isNull(entityQuery) ? new DriverAttributeQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<DriverAttributeBO> entityPageBO = driverAttributeService.list(query);
            Page<DriverAttributeVO> entityPageVO = driverAttributeBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}
