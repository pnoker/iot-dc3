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
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.builder.DriverBuilder;
import io.github.pnoker.common.manager.entity.query.DriverQuery;
import io.github.pnoker.common.manager.entity.vo.DriverVO;
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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller exposing driver management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "driver", description = "Protocol driver lifecycle: register, configure, schedule, and control industrial protocol adapters that connect physical devices to the platform")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.DRIVER_URL_PREFIX)
@RequiredArgsConstructor
public class DriverController implements BaseController {

    private final DriverBuilder driverBuilder;

    private final DriverService driverService;

    /**
     * Register a new driver protocol adapter for the current tenant, then return the add-success status.
     *
     * @param entityVO driver payload to create
     * @return add-success status
     */
    @PreAuthorize("@perm.can('driver', 'add')")
    @Operation(summary = "Add Driver", description = "Register a new driver protocol adapter for the current tenant. " +
            "A driver connects devices to the platform and reads or writes their point values; returns the new driver ID.")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody DriverVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverBO entityBO = driverBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            driverService.add(entityBO);
            return R.ok(SuccessCode.ADD);
        }));
    }

    /**
     * Delete a driver after verifying it belongs to the current tenant, then return the delete-success status.
     *
     * @param id id of the driver to delete
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('driver', 'delete')")
    @Operation(summary = "Delete Driver", description = "Permanently delete a driver by ID (tenant-scoped). " +
            "Removes the driver adapter; devices bound to it can no longer collect or write point values until reassigned; the action cannot be undone.")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, driverService.getById(id));
            driverService.delete(id);
            return R.ok(SuccessCode.DELETE);
        }));
    }

    /**
     * Update an existing driver after verifying tenant ownership, then return the update-success status.
     *
     * @param entityVO driver payload to update
     * @return update-success status
     */
    @PreAuthorize("@perm.can('driver', 'update')")
    @Operation(summary = "Update Driver", description = "Update an existing driver's attributes (tenant-scoped). " +
            "Modifies the protocol adapter configuration such as service name, mode and enable flag; returns the update result.")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody DriverVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverBO entityBO = driverBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, driverService.getById(entityBO.getId()));
            driverService.update(entityBO);
            return R.ok(SuccessCode.UPDATE);
        }));
    }

    /**
     * Fetch one driver by ID after verifying it belongs to the current tenant.
     *
     * @param id id of the driver to fetch
     * @return the matched DriverVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('driver', 'get')")
    @Operation(summary = "Get Driver by ID", description = "Fetch one driver by ID (tenant-scoped). " +
            "Use to inspect a protocol adapter before assigning devices, sending commands or reading point values through it.")
    @GetMapping("/get_by_id")
    public Mono<R<DriverVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverBO entityBO = requireTenant(tenantId, driverService.getById(id));
            DriverVO entityVO = driverBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Resolve a set of driver IDs to their details, filtered to the current tenant.
     *
     * @param driverIds ids of the drivers to resolve
     * @return a map of id to DriverVO for the tenant-owned matched ids
     */
    @PreAuthorize("@perm.can('driver', 'list')")
    @Operation(summary = "List Drivers by IDs", description = "Return the drivers matching a set of IDs, filtered to the current tenant. " +
            "Returns a map of driver ID to driver; missing or out-of-tenant IDs are silently omitted.")
    @PostMapping("/list_by_ids")
    public Mono<R<Map<Long, DriverVO>>> listByIds(@RequestBody Set<Long> driverIds) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            List<DriverBO> entityBOList = filterTenant(tenantId, driverService.listByIds(driverIds));
            Map<Long, DriverVO> driverMap = entityBOList.stream()
                    .collect(Collectors.toMap(DriverBO::getId, entityBO -> driverBuilder.buildVOByBO(entityBO)));
            return R.ok(driverMap);
        }));
    }

    /**
     * Fetch one driver by its protocol service name for the current tenant.
     *
     * @param serviceName protocol service name of the driver to fetch
     * @return the matched DriverVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('driver', 'get')")
    @Operation(summary = "Get Driver by Service Name", description = "Fetch one driver by its protocol service name (tenant-scoped). " +
            "Use to resolve a driver instance from the service identifier under which it registered with the platform.")
    @GetMapping("/get_by_service_name")
    public Mono<R<DriverVO>> getByServiceName(@Parameter(description = "Unique protocol service name under which the driver registered; must belong to the current tenant.", example = "Modbus-TCP-Driver") @NotNull @RequestParam(value = "service_name") String serviceName) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverBO entityBO = driverService.getByServiceName(serviceName, tenantId);
            DriverVO entityVO = driverBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Page through drivers for the current tenant using the supplied query filters.
     *
     * @param entityQuery optional query filters (name, service name, mode, enable flag); a new query is used when null
     * @return a page of DriverVO matching the query
     */
    @PreAuthorize("@perm.can('driver', 'list')")
    @Operation(summary = "List Drivers", description = "Page through drivers for the current tenant with filters such as name, service name, mode and enable flag. " +
            "Returns a page of drivers; use for browsing or selecting a target protocol adapter for a device.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<R<Page<DriverVO>>> list(@RequestBody(required = false) DriverQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverQuery query = Objects.isNull(entityQuery) ? new DriverQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<DriverBO> entityPageBO = driverService.list(query);
            Page<DriverVO> entityPageVO = driverBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}
