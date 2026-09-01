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
import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.common.manager.entity.builder.EventBuilder;
import io.github.pnoker.common.manager.entity.query.EventOffsetRequest;
import io.github.pnoker.common.manager.entity.vo.EventVO;
import io.github.pnoker.common.manager.service.ReactiveEventService;
import io.github.pnoker.common.manager.repository.EventFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Manages device-reported event definitions declared on profile templates, the occurrences a device raises at runtime.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Tag(name = "event", description = "Device event definitions: manage alarm definitions, state-change notifications, and status events with configurable trigger conditions and severity levels")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.EVENT_URL_PREFIX)
@RequiredArgsConstructor
public class EventController implements BaseController {

    private final EventBuilder eventBuilder;

    private final ReactiveEventService reactiveEventService;

    /**
     * Define a new device-reported event on a profile template for the current tenant.
     *
     * @param entityVO event payload to create, carrying its attribute definitions
     * @return the id of the newly created event
     */
    @PreAuthorize("@perm.can('event', 'add')")
    @Operation(summary = "Add Event", description = "Define a new device-reported event on a profile template for the current tenant. " +
            "An event is an occurrence or alert a device raises at runtime, with its attribute definitions; returns the new event ID.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<EventVO> add(@Validated(Add.class) @RequestBody EventVO entityVO) {
        return getTenantId().zipWith(getUserId().defaultIfEmpty(0L)).zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> {
            Long tenantId = tuple.getT1().getT1();
            EventBO entityBO = eventBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            entityBO.setCreatorId(tuple.getT1().getT2()); entityBO.setCreatorName(tuple.getT2());
            entityBO.setOperatorId(tuple.getT1().getT2()); entityBO.setOperatorName(tuple.getT2());
            return reactiveEventService.add(entityBO).map(eventBuilder::buildVOByBO);
        });
    }

    /**
     * Permanently delete an event definition by ID, scoped to the current tenant, while preserving reported event history.
     *
     * @param id id of the event to delete; must belong to the current tenant
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('event', 'delete')")
    @Operation(summary = "Delete Event", description = "Permanently delete an event definition by ID (tenant-scoped). " +
            "Removes the event and its attribute definitions from the profile template while preserving reported event history; the action cannot be undone.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @DeleteMapping("/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id,
                             @Parameter(description = "Current optimistic-lock version required as a deletion precondition.", example = "0") @NotNull @Min(0) @RequestParam("version") Integer version) {
        return getTenantId().zipWith(getUserId().defaultIfEmpty(0L)).zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> reactiveEventService.delete(tuple.getT1().getT1(), id, version, tuple.getT1().getT2(), tuple.getT2()).then());
    }

    /**
     * Modify an existing event definition on the profile template, scoped to the current tenant.
     *
     * @param entityVO event payload carrying the updated fields; ownership is verified before applying
     * @return update-success status
     */
    @PreAuthorize("@perm.can('event', 'update')")
    @Operation(summary = "Update Event", description = "Modify an existing event definition on the profile template (tenant-scoped). " +
            "Updates event attributes such as name and configuration; use to refine how a device-reported occurrence is modeled.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/update")
    public Mono<EventVO> update(@Validated(Update.class) @RequestBody EventVO entityVO) {
        return getTenantId().zipWith(getUserId().defaultIfEmpty(0L)).zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> {
            Long tenantId = tuple.getT1().getT1();
            EventBO entityBO = eventBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            entityBO.setOperatorId(tuple.getT1().getT2()); entityBO.setOperatorName(tuple.getT2());
            return reactiveEventService.update(entityBO).map(eventBuilder::buildVOByBO);
        });
    }

    /**
     * Fetch one event definition with its attribute definitions, scoped to the current tenant.
     *
     * @param id id of the event to fetch; must belong to the current tenant
     * @return the matched EventVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('event', 'get')")
    @Operation(summary = "Get Event by ID", description = "Fetch one event definition with its attribute definitions (tenant-scoped). " +
            "Use to inspect an event before subscribing to its reported history or adjusting its configuration.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<EventVO> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> reactiveEventService.getById(tenantId, id).map(eventBuilder::buildVOByBO));
    }

    /**
     * Return every event definition declared on a given profile template, scoped to the current tenant.
     *
     * @param profileId id of the profile template whose event definitions are returned; must belong to the current tenant
     * @return a list of EventVO declared on the profile
     */
    @PreAuthorize("@perm.can('event', 'list')")
    @Operation(summary = "List Events by Profile ID", description = "Return every event definition declared on a given profile template (tenant-scoped). " +
            "Use to discover which device-reported occurrences the profile exposes before binding or inspecting a device.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_by_profile_id")
    public Mono<List<EventVO>> listByProfileId(@Parameter(description = "Identifier of the profile template whose event definitions should be returned; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "profile_id") Long profileId) {
        return getTenantId().flatMap(tenantId -> reactiveEventService.listByProfileId(tenantId, profileId).map(eventBuilder::buildVOByBO).collectList());
    }

    /**
     * Return every event definition available on a given device, resolved from its profile template, scoped to the current tenant.
     *
     * @param deviceId id of the device whose available event definitions are returned; must belong to the current tenant
     * @return a list of EventVO the device may report
     */
    @PreAuthorize("@perm.can('event', 'list')")
    @Operation(summary = "List Events by Device ID", description = "Return every event definition available on a given device (tenant-scoped), " +
            "resolved from the device's profile template. Use to see which occurrences a specific device may report.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_by_device_id")
    public Mono<List<EventVO>> listByDeviceId(@Parameter(description = "Identifier of the device whose available event definitions should be returned; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "device_id") Long deviceId) {
        return getTenantId().flatMap(tenantId -> reactiveEventService.listByDeviceId(tenantId, deviceId).map(eventBuilder::buildVOByBO).collectList());
    }

    /**
     * Page through event definitions for the current tenant with filters from the query body.
     *
     * @param entityQuery optional query filters; null treated as empty
     * @return a page of EventVO matching the query
     */
    @PreAuthorize("@perm.can('event', 'list')")
    @Operation(summary = "List Events", description = "Page through event definitions for the current tenant with filters from the query body. " +
            "Returns a page of events; use for browsing or locating a specific event definition.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<OffsetPage<EventVO>> list(@RequestBody(required = false) EventOffsetRequest request) {
        EventOffsetRequest query = request == null ? new EventOffsetRequest() : request;
        return getTenantId().flatMap(tenantId -> reactiveEventService.list(new EventFilter(
                        tenantId, query.eventName(), query.eventCode(), query.eventTypeFlag(), query.eventLevelFlag(),
                        query.profileId(), query.enableFlag(), query.version(), query.deviceId(), query.offset(), query.limit(), query.sort()))
                .map(page -> OffsetPage.of(page.items().stream().map(eventBuilder::buildVOByBO).toList(), page.offset(), page.limit(), page.total())));
    }

}
