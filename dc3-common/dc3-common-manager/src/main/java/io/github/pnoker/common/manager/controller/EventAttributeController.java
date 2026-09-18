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
import io.github.pnoker.common.manager.entity.bo.EventAttributeBO;
import io.github.pnoker.common.manager.entity.builder.EventAttributeBuilder;
import io.github.pnoker.common.manager.entity.query.EventAttributeOffsetRequest;
import io.github.pnoker.common.manager.entity.vo.EventAttributeVO;
import io.github.pnoker.common.manager.repository.EventAttributeFilter;
import io.github.pnoker.common.manager.service.ReactiveEventAttributeService;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Manages event attribute field definitions declared on profile templates, the configurable fields of a downward control instruction.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Tag(
        name = "event_attribute",
        description =
                "Command attribute definitions: manage configurable parameters of device commands including name, type, default value, and validation rules")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.EVENT_ATTRIBUTE_URL_PREFIX)
@RequiredArgsConstructor
public class EventAttributeController implements BaseController {

    private final EventAttributeBuilder eventAttributeBuilder;

    private final ReactiveEventAttributeService eventAttributeService;

    /**
     * Declare a new event attribute field definition on a profile template for the current tenant.
     *
     * @param entityVO event attribute payload to create (name, type, default value, validation rules)
     * @return add-success status
     */
    @PreAuthorize("@perm.can('event_attribute', 'add')")
    @Operation(
            summary = "Add Event Attribute",
            description =
                    "Declare a new event attribute field on a profile template. "
                            + "A event attribute is a configurable field definition of a downward control instruction; returns the new attribute ID.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "false"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/add")
    public Mono<EventAttributeVO> add(@Validated(Add.class) @RequestBody EventAttributeVO entityVO) {
        return getTenantId()
                .zipWith(getUserId().defaultIfEmpty(0L))
                .zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> {
                    Long tenantId = tuple.getT1().getT1();
                    EventAttributeBO entityBO = eventAttributeBuilder.buildBOByVO(entityVO);
                    entityBO.setTenantId(tenantId);
                    entityBO.setCreatorId(tuple.getT1().getT2());
                    entityBO.setCreatorName(tuple.getT2());
                    entityBO.setOperatorId(tuple.getT1().getT2());
                    entityBO.setOperatorName(tuple.getT2());
                    return eventAttributeService.add(entityBO).map(eventAttributeBuilder::buildVOByBO);
                });
    }

    /**
     * Permanently delete a event attribute field definition by ID, scoped to the current tenant.
     *
     * @param id id of the event attribute to delete; must belong to the current tenant
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('event_attribute', 'delete')")
    @Operation(
            summary = "Delete Event Attribute",
            description = "Permanently delete a event attribute field definition by ID (tenant-scoped). "
                    + "Removes the field from its parent command; the action cannot be undone.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                                @ExtensionProperty(name = "destructive", value = "true"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @DeleteMapping("/delete")
    public Mono<Void> delete(
            @Parameter(
                            description = "Primary key of the entity to delete. Must belong to the current tenant.",
                            example = "1024")
                    @NotNull
                    @RequestParam(value = "id")
                    Long id,
            @Parameter(
                            description = "Current optimistic-lock version required as a deletion precondition.",
                            example = "0")
                    @NotNull
                    @Min(0)
                    @RequestParam("version")
                    Integer version) {
        return getTenantId()
                .zipWith(getUserId().defaultIfEmpty(0L))
                .zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> eventAttributeService
                        .delete(
                                tuple.getT1().getT1(),
                                id,
                                version,
                                tuple.getT1().getT2(),
                                tuple.getT2())
                        .then());
    }

    /**
     * Modify an existing event attribute field definition, scoped to the current tenant.
     *
     * @param entityVO event attribute payload carrying the updated fields; ownership is verified before applying
     * @return update-success status
     */
    @PreAuthorize("@perm.can('event_attribute', 'update')")
    @Operation(
            summary = "Update Event Attribute",
            description =
                    "Modify an existing event attribute field definition (tenant-scoped). "
                            + "Use to rename or change the type/default of a field declared on a command in the profile template.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/update")
    public Mono<EventAttributeVO> update(@Validated(Update.class) @RequestBody EventAttributeVO entityVO) {
        return getTenantId()
                .zipWith(getUserId().defaultIfEmpty(0L))
                .zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> {
                    Long tenantId = tuple.getT1().getT1();
                    EventAttributeBO entityBO = eventAttributeBuilder.buildBOByVO(entityVO);
                    entityBO.setTenantId(tenantId);
                    entityBO.setOperatorId(tuple.getT1().getT2());
                    entityBO.setOperatorName(tuple.getT2());
                    return eventAttributeService.update(entityBO).map(eventAttributeBuilder::buildVOByBO);
                });
    }

    /**
     * Fetch one event attribute field definition by ID, scoped to the current tenant.
     *
     * @param id id of the event attribute to fetch; must belong to the current tenant
     * @return the matched EventAttributeVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('event_attribute', 'get')")
    @Operation(
            summary = "Get Event Attribute by ID",
            description = "Fetch one event attribute field definition by ID (tenant-scoped). "
                    + "Returns the attribute's name, type and default value as declared on its parent command.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @GetMapping("/get_by_id")
    public Mono<EventAttributeVO> getById(
            @Parameter(
                            description = "Primary key of the target record; must belong to the current tenant.",
                            example = "1024")
                    @NotNull
                    @RequestParam(value = "id")
                    Long id) {
        return getTenantId()
                .flatMap(tenantId ->
                        eventAttributeService.getById(tenantId, id).map(eventAttributeBuilder::buildVOByBO));
    }

    /**
     * Return every event attribute field definition reachable through a given driver, scoped to the current tenant.
     *
     * @param driverId id of the driver whose reachable event attribute fields are enumerated; must belong to the current tenant
     * @return a list of EventAttributeVO exposed by the driver; empty when the driver is not found
     */
    @PreAuthorize("@perm.can('event_attribute', 'list')")
    @Operation(
            summary = "List Event Attributes by Driver ID",
            description =
                    "Return every event attribute exposed by the commands of devices driven by a given driver (tenant-scoped). "
                            + "Use to enumerate which configurable command fields a driver-type adapter can send; returns an empty list when the driver is not found.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @GetMapping("/list_by_driver_id")
    public Mono<List<EventAttributeVO>> listByDriverId(
            @Parameter(
                            description =
                                    "Identifier of the driver whose event attributes are enumerated; must belong to the current tenant.",
                            example = "1024")
                    @NotNull
                    @RequestParam(value = "driver_id")
                    Long driverId) {
        return getTenantId()
                .flatMap(tenantId -> eventAttributeService
                        .listByDriverId(tenantId, driverId)
                        .map(eventAttributeBuilder::buildVOByBO)
                        .collectList());
    }

    /**
     * Page through event attribute field definitions for the current tenant with query filters.
     *
     * @param request     optional query filters; null treated as empty
     * @return a page of EventAttributeVO matching the query
     */
    @PreAuthorize("@perm.can('event_attribute', 'list')")
    @Operation(
            summary = "List Event Attributes",
            description =
                    "Page through event attribute field definitions for the current tenant with query filters. "
                            + "Returns a page of attributes; use for browsing command fields or selecting a target attribute to inspect or edit.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/list")
    public Mono<OffsetPage<EventAttributeVO>> list(@RequestBody(required = false) EventAttributeOffsetRequest request) {
        EventAttributeOffsetRequest query = request == null ? new EventAttributeOffsetRequest() : request;
        return getTenantId()
                .flatMap(tenantId -> eventAttributeService
                        .list(new EventAttributeFilter(
                                tenantId,
                                query.attributeName(),
                                query.attributeCode(),
                                query.attributeTypeFlag(),
                                query.driverId(),
                                query.enableFlag(),
                                query.version(),
                                query.offset(),
                                query.limit(),
                                query.sort()))
                        .map(page -> OffsetPage.of(
                                page.items().stream()
                                        .map(eventAttributeBuilder::buildVOByBO)
                                        .toList(),
                                page.offset(),
                                page.limit(),
                                page.total())));
    }
}
