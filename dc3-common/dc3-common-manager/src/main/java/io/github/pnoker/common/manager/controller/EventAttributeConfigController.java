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
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.EventAttributeBO;
import io.github.pnoker.common.manager.entity.bo.EventAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.common.manager.entity.builder.EventAttributeConfigBuilder;
import io.github.pnoker.common.manager.entity.query.EventAttributeConfigQuery;
import io.github.pnoker.common.manager.entity.vo.EventAttributeConfigVO;
import io.github.pnoker.common.manager.service.DeviceService;
import io.github.pnoker.common.manager.service.EventAttributeConfigService;
import io.github.pnoker.common.manager.service.EventAttributeService;
import io.github.pnoker.common.manager.service.EventService;
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
import java.util.Objects;

/**
 * Manages per-device event attribute configuration values that override the defaults declared on a profile template's event attributes.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "event_attribute_config", description = "Event attribute configuration values: set and update per-device customization values for event properties inherited from event attribute definitions")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.EVENT_ATTRIBUTE_CONFIG_URL_PREFIX)
@RequiredArgsConstructor
public class EventAttributeConfigController implements BaseController {

    private final EventAttributeConfigBuilder eventAttributeConfigBuilder;

    private final EventAttributeConfigService eventAttributeConfigService;

    private final DeviceService deviceService;

    private final EventService eventService;

    private final EventAttributeService eventAttributeService;

    /**
     * Set the configured value of one event attribute field for a specific device and event, overriding the profile template default.
     *
     * @param entityVO event attribute config payload to create (attribute, device, event and configured value)
     * @return add-success status
     */
    @PreAuthorize("@perm.can('event_attribute_config', 'add')")
    @Operation(summary = "Add Event Attribute Configuration", description = "Set the configured value of one event attribute field for a specific device and event under the current tenant. Use to override the attribute definition declared on the profile template for that device instance; returns the new config ID.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody EventAttributeConfigVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventAttributeConfigBO entityBO = eventAttributeConfigBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            eventAttributeConfigService.add(entityBO);
            return R.ok(SuccessCode.ADD);
        }));
    }

    /**
     * Permanently delete one event attribute configuration by ID, scoped to the current tenant.
     *
     * @param id id of the event attribute config to delete; must belong to the current tenant
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('event_attribute_config', 'delete')")
    @Operation(summary = "Delete Event Attribute Configuration", description = "Permanently delete one event attribute configuration by ID (tenant-scoped). Removes the device's configured value for that attribute while leaving the attribute definition on the profile template intact; the action cannot be undone.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, eventAttributeConfigService.getById(id));
            eventAttributeConfigService.delete(id);
            return R.ok(SuccessCode.DELETE);
        }));
    }

    /**
     * Change the configured value of an existing event attribute field for a specific device and event, scoped to the current tenant.
     *
     * @param entityVO event attribute config payload carrying the updated value; ownership is verified before applying
     * @return update-success status
     */
    @PreAuthorize("@perm.can('event_attribute_config', 'update')")
    @Operation(summary = "Update Event Attribute Configuration", description = "Change the configured value of an existing event attribute field for a specific device and event under the current tenant. Tenant ownership of the record is verified before applying the update.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody EventAttributeConfigVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventAttributeConfigBO entityBO = eventAttributeConfigBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, eventAttributeConfigService.getById(entityBO.getId()));
            eventAttributeConfigService.update(entityBO);
            return R.ok(SuccessCode.UPDATE);
        }));
    }

    /**
     * Fetch one event attribute configuration by its record ID, scoped to the current tenant.
     *
     * @param id id of the event attribute config to fetch; must belong to the current tenant
     * @return the matched EventAttributeConfigVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('event_attribute_config', 'get')")
    @Operation(summary = "Get Event Attribute Configuration by ID", description = "Fetch one event attribute configuration by its record ID (tenant-scoped). Use to inspect the configured value a device uses for a single event attribute field.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<R<EventAttributeConfigVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventAttributeConfigBO entityBO = requireTenant(tenantId, eventAttributeConfigService.getById(id));
            EventAttributeConfigVO entityVO = eventAttributeConfigBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Fetch the configured value of one event attribute field by its attribute, device and event IDs, scoped to the current tenant.
     *
     * @param attributeId id of the event attribute whose configured value is being fetched; its driver must match the device's driver
     * @param deviceId    id of the device whose configured value is being fetched; its profile must match the event's profile
     * @param eventId     id of the event whose attribute value is being fetched
     * @return the matched EventAttributeConfigVO; fails if the device/event/attribute triple is invalid or not tenant-owned
     */
    @PreAuthorize("@perm.can('event_attribute_config', 'get')")
    @Operation(summary = "Get Event Attribute Configuration by Attribute, Device, and Event IDs",
            description = "Fetch the configured value of one event attribute field for a specific device, event, " +
                    "and attribute. Validates that the device's profile matches the event and its driver matches " +
                    "the attribute before returning; use when you need a single attribute's device-specific override.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_attribute_id_and_device_id_and_event_id")
    public Mono<R<EventAttributeConfigVO>> getByAttributeIdAndDeviceIdAndEventId(
            @Parameter(description = "Identifier of the event attribute whose configured value is being fetched; its driver must match the device's driver.", example = "1024") @NotNull @RequestParam(value = "attribute_id") Long attributeId,
            @Parameter(description = "Identifier of the device whose configured value is being fetched; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "device_id") Long deviceId,
            @Parameter(description = "Identifier of the event whose attribute value is being fetched; its profile must match the device's profile.", example = "1024") @NotNull @RequestParam(value = "event_id") Long eventId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireEventConfigRelations(tenantId, deviceId, eventId, attributeId);
            EventAttributeConfigBO entityBO = eventAttributeConfigService
                    .getByAttributeIdAndDeviceIdAndEventId(attributeId, deviceId, eventId);
            requireTenant(tenantId, entityBO);
            EventAttributeConfigVO entityVO = eventAttributeConfigBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Return every event attribute configuration for one device and one event, scoped to the current tenant.
     *
     * @param deviceId id of the device whose configurations are listed; its profile must match the event's profile
     * @param eventId  id of the event whose configurations are listed
     * @return a list of EventAttributeConfigVO for the device-event pair
     */
    @PreAuthorize("@perm.can('event_attribute_config', 'list')")
    @Operation(summary = "List Event Attribute Configurations by Device and Event IDs", description = "Return every event attribute configuration for one device and one event (tenant-scoped). Use to read all configured values the device supplies for that event's attributes; the device's profile must match the event.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_by_device_id_and_event_id")
    public Mono<R<List<EventAttributeConfigVO>>> listByDeviceIdAndEventId(
            @Parameter(description = "Identifier of the device whose configurations are listed; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "device_id") Long deviceId,
            @Parameter(description = "Identifier of the event whose configurations are listed; its profile must match the device's profile.", example = "1024") @NotNull @RequestParam(value = "event_id") Long eventId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireEventConfigRelations(tenantId, deviceId, eventId, null);
            List<EventAttributeConfigBO> entityBOList = filterTenant(tenantId,
                    eventAttributeConfigService.listByDeviceIdAndEventId(deviceId, eventId));
            List<EventAttributeConfigVO> entityVOList = eventAttributeConfigBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    /**
     * Return every event attribute configuration for one device across all of its events, scoped to the current tenant.
     *
     * @param deviceId id of the device whose configurations are listed; must belong to the current tenant
     * @return a list of EventAttributeConfigVO set on the device
     */
    @PreAuthorize("@perm.can('event_attribute_config', 'list')")
    @Operation(summary = "List Event Attribute Configurations by Device ID", description = "Return every event attribute configuration for one device across all of its events (tenant-scoped). Use to read the full set of configured event-attribute values a device uses.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_by_device_id")
    public Mono<R<List<EventAttributeConfigVO>>> listByDeviceId(
            @Parameter(description = "Identifier of the device whose configurations are listed; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "device_id") Long deviceId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, deviceService.getById(deviceId));
            List<EventAttributeConfigBO> entityBOList = filterTenant(tenantId,
                    eventAttributeConfigService.listByDeviceId(deviceId));
            List<EventAttributeConfigVO> entityVOList = eventAttributeConfigBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    /**
     * Page through event attribute configurations for the current tenant with filters from the query body.
     *
     * @param entityQuery optional query filters; null treated as empty
     * @return a page of EventAttributeConfigVO matching the query
     */
    @PreAuthorize("@perm.can('event_attribute_config', 'list')")
    @Operation(summary = "List Event Attribute Configurations", description = "Page through event attribute configurations for the current tenant with filters from the query body. Returns a page of configurations; use for browsing or selecting a target configuration.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<R<Page<EventAttributeConfigVO>>> list(
            @RequestBody(required = false) EventAttributeConfigQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventAttributeConfigQuery query = Objects.isNull(entityQuery) ? new EventAttributeConfigQuery()
                    : entityQuery;
            query.setTenantId(tenantId);
            Page<EventAttributeConfigBO> entityPageBO = eventAttributeConfigService.list(query);
            Page<EventAttributeConfigVO> entityPageVO = eventAttributeConfigBuilder
                    .buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    /**
     * Validate that device, event, and (optional) attribute belong to the tenant,
     * share a profile, and that the attribute's driver matches the device's driver.
     *
     * @param tenantId    tenant scope
     * @param deviceId    the device to validate
     * @param eventId     the event to validate
     * @param attributeId the attribute to validate, may be null to skip
     */
    private void requireEventConfigRelations(Long tenantId, Long deviceId, Long eventId, Long attributeId) {
        DeviceBO deviceBO = requireTenant(tenantId, deviceService.getById(deviceId));
        EventBO eventBO = requireTenant(tenantId, eventService.getById(eventId));
        if (Objects.isNull(deviceBO.getProfileId()) || !Objects.equals(deviceBO.getProfileId(), eventBO.getProfileId())) {
            throw new NotFoundException("Resource does not exist");
        }

        if (Objects.nonNull(attributeId)) {
            EventAttributeBO attributeBO = requireTenant(tenantId, eventAttributeService.getById(attributeId));
            if (!Objects.equals(deviceBO.getDriverId(), attributeBO.getDriverId())) {
                throw new NotFoundException("Resource does not exist");
            }
        }
    }

}
