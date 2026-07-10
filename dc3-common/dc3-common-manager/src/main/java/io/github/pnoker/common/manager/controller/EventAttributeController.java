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
import io.github.pnoker.common.manager.entity.bo.EventAttributeBO;
import io.github.pnoker.common.manager.entity.builder.EventAttributeBuilder;
import io.github.pnoker.common.manager.entity.query.EventAttributeQuery;
import io.github.pnoker.common.manager.entity.vo.EventAttributeVO;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.EventAttributeService;
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
 * Manages event attribute field definitions declared on profile templates, the configurable fields of a device-reported event.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "event_attribute", description = "Event attribute definitions: manage configurable properties of device events including name, type, and value constraints")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.EVENT_ATTRIBUTE_URL_PREFIX)
@RequiredArgsConstructor
public class EventAttributeController implements BaseController {

    private final EventAttributeBuilder eventAttributeBuilder;

    private final EventAttributeService eventAttributeService;

    private final DriverService driverService;

    /**
     * Define a new event attribute field on a profile template for the current tenant.
     *
     * @param entityVO event attribute payload to create (name, type, value constraints)
     * @return add-success status
     */
    @PreAuthorize("@perm.can('event_attribute', 'add')")
    @Operation(summary = "Add Event Attribute", description = "Define a new event attribute field on a profile template for the current tenant. " +
            "An event attribute declares one configurable field of a device-reported event; returns the new attribute ID.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody EventAttributeVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventAttributeBO entityBO = eventAttributeBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            eventAttributeService.add(entityBO);
            return R.ok(SuccessCode.ADD);
        }));
    }

    /**
     * Permanently delete an event attribute field definition by ID, scoped to the current tenant.
     *
     * @param id id of the event attribute to delete; must belong to the current tenant
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('event_attribute', 'delete')")
    @Operation(summary = "Delete Event Attribute", description = "Permanently delete an event attribute field definition by ID (tenant-scoped). " +
            "Removes the attribute from its profile template; the action cannot be undone.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, eventAttributeService.getById(id));
            eventAttributeService.delete(id);
            return R.ok(SuccessCode.DELETE);
        }));
    }

    /**
     * Modify an existing event attribute field definition, scoped to the current tenant.
     *
     * @param entityVO event attribute payload carrying the updated fields; ownership is verified before applying
     * @return update-success status
     */
    @PreAuthorize("@perm.can('event_attribute', 'update')")
    @Operation(summary = "Update Event Attribute", description = "Modify an existing event attribute field definition on its profile template. " +
            "Tenant-scoped: ownership of the existing attribute is verified before applying the change.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody EventAttributeVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventAttributeBO entityBO = eventAttributeBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, eventAttributeService.getById(entityBO.getId()));
            eventAttributeService.update(entityBO);
            return R.ok(SuccessCode.UPDATE);
        }));
    }

    /**
     * Fetch one event attribute field definition by ID, scoped to the current tenant.
     *
     * @param id id of the event attribute to fetch; must belong to the current tenant
     * @return the matched EventAttributeVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('event_attribute', 'get')")
    @Operation(summary = "Get Event Attribute by ID", description = "Fetch one event attribute field definition by ID (tenant-scoped). " +
            "Use to inspect a single configurable field of a device-reported event before editing it.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<R<EventAttributeVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventAttributeBO entityBO = requireTenant(tenantId, eventAttributeService.getById(id));
            EventAttributeVO entityVO = eventAttributeBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Return every event attribute field definition reachable through a given driver, scoped to the current tenant.
     *
     * @param driverId id of the driver whose reachable event attribute fields are listed; must belong to the current tenant
     * @return a list of EventAttributeVO exposed by the driver; empty when the driver is not found
     */
    @PreAuthorize("@perm.can('event_attribute', 'list')")
    @Operation(summary = "List Event Attributes by Driver ID", description = "Return every event attribute field definition reachable through a given driver (tenant-scoped). " +
            "Use to discover which device-reported event fields a driver exposes; returns an empty list when the driver is not found.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_by_driver_id")
    public Mono<R<List<EventAttributeVO>>> listByDriverId(@Parameter(description = "Identifier of the driver whose reachable event attribute fields should be listed; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "driver_id") Long driverId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            try {
                requireTenant(tenantId, driverService.getById(driverId));
                List<EventAttributeBO> entityBOList = filterTenant(tenantId, eventAttributeService.listByDriverId(driverId));
                List<EventAttributeVO> entityVO = eventAttributeBuilder.buildVOListByBOList(entityBOList);
                return R.ok(entityVO);
            } catch (NotFoundException ignored) {
                return R.ok(Collections.emptyList());
            }
        }));
    }

    /**
     * Page through event attribute field definitions for the current tenant with query filters.
     *
     * @param entityQuery optional query filters; null treated as empty
     * @return a page of EventAttributeVO matching the query
     */
    @PreAuthorize("@perm.can('event_attribute', 'list')")
    @Operation(summary = "List Event Attributes", description = "Page through event attribute field definitions for the current tenant with filters from the query body. " +
            "Returns a page of attributes; use for browsing or selecting a target event attribute.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<R<Page<EventAttributeVO>>> list(@RequestBody(required = false) EventAttributeQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventAttributeQuery query = Objects.isNull(entityQuery) ? new EventAttributeQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<EventAttributeBO> entityPageBO = eventAttributeService.list(query);
            Page<EventAttributeVO> entityPageVO = eventAttributeBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}
