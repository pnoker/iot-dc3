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
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeConfigBO;
import io.github.pnoker.common.manager.entity.builder.DriverAttributeConfigBuilder;
import io.github.pnoker.common.manager.entity.query.DriverAttributeConfigQuery;
import io.github.pnoker.common.manager.entity.vo.DriverAttributeConfigVO;
import io.github.pnoker.common.manager.service.DeviceService;
import io.github.pnoker.common.manager.service.DriverAttributeConfigService;
import io.github.pnoker.common.manager.service.DriverAttributeService;
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
 * REST controller exposing driver attribute config management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "driver_attribute_config", description = "Driver attribute configuration values: set and update per-device customization values for driver properties inherited from driver attribute definitions")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.DRIVER_ATTRIBUTE_CONFIG_URL_PREFIX)
@RequiredArgsConstructor
public class DriverAttributeConfigController implements BaseController {

    private final DriverAttributeConfigBuilder driverAttributeConfigBuilder;

    private final DriverAttributeConfigService driverAttributeConfigService;

    private final DeviceService deviceService;

    private final DriverAttributeService driverAttributeService;

    /**
     * Create the configured value of a driver attribute for a device.
     *
     * @param entityVO driver attribute config payload to create (device, attribute, value)
     * @return add-success status
     */
    @PreAuthorize("@perm.can('driver_attribute_config', 'add')")
    @Operation(summary = "Add Driver Attribute Configuration", description = "Set the configured value of a driver attribute for a device under the current tenant. " +
            "A driver attribute config holds the concrete value of a field declared on the device's driver; returns the new config ID.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody DriverAttributeConfigVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverAttributeConfigBO entityBO = driverAttributeConfigBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            driverAttributeConfigService.add(entityBO);
            return R.ok(SuccessCode.ADD);
        }));
    }

    /**
     * Delete a driver attribute config by ID.
     *
     * @param id id of the driver attribute config to delete (must be tenant-owned)
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('driver_attribute_config', 'delete')")
    @Operation(summary = "Delete Driver Attribute Configuration", description = "Permanently delete a driver attribute config by ID (tenant-scoped). " +
            "Removes the configured value for that driver attribute on its device; the action cannot be undone.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, driverAttributeConfigService.getById(id));
            driverAttributeConfigService.delete(id);
            return R.ok(SuccessCode.DELETE);
        }));
    }

    /**
     * Update the configured value of an existing driver attribute config.
     *
     * @param entityVO driver attribute config payload to update (must carry an existing id)
     * @return update-success status
     */
    @PreAuthorize("@perm.can('driver_attribute_config', 'update')")
    @Operation(summary = "Update Driver Attribute Configuration", description = "Change the configured value of an existing driver attribute config (tenant-scoped). " +
            "Use to reconfigure how a specific device's driver field is set; ownership is verified before the update is applied.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody DriverAttributeConfigVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverAttributeConfigBO entityBO = driverAttributeConfigBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, driverAttributeConfigService.getById(entityBO.getId()));
            driverAttributeConfigService.update(entityBO);
            return R.ok(SuccessCode.UPDATE);
        }));
    }

    /**
     * Fetch a single driver attribute config by ID.
     *
     * @param id id of the driver attribute config to fetch (must be tenant-owned)
     * @return the matched DriverAttributeConfigVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('driver_attribute_config', 'get')")
    @Operation(summary = "Get Driver Attribute Configuration by ID", description = "Fetch one driver attribute config by ID (tenant-scoped). " +
            "Returns the configured value of a single driver attribute field for the device it belongs to.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<R<DriverAttributeConfigVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverAttributeConfigBO entityBO = requireTenant(tenantId, driverAttributeConfigService.getById(id));
            DriverAttributeConfigVO entityVO = driverAttributeConfigBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Fetch the configured value of one driver attribute for a specific device.
     *
     * @param deviceId    id of the device whose config is read (must be tenant-owned)
     * @param attributeId id of the driver attribute whose value is read (its driver must match the device's)
     * @return the matched DriverAttributeConfigVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('driver_attribute_config', 'get')")
    @Operation(summary = "Get Driver Attribute Configuration by Device and Attribute IDs", description = "Fetch the configured value of one driver attribute for a specific device (tenant-scoped). " +
            "The device's driver must match the attribute's driver; use to read a single connection attribute before invoking the driver.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_device_id_and_attribute_id")
    public Mono<R<DriverAttributeConfigVO>> getByDeviceIdAndAttributeId(
            @Parameter(description = "Identifier of the device whose driver attribute config is being read; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "device_id") Long deviceId,
            @Parameter(description = "Identifier of the driver attribute whose configured value is being read; its driver must match the device's driver.", example = "2048") @NotNull @RequestParam(value = "attribute_id") Long attributeId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireDriverConfigRelations(tenantId, deviceId, attributeId);
            DriverAttributeConfigBO entityBO = driverAttributeConfigService.selectByAttributeIdAndDeviceId(deviceId,
                    attributeId);
            requireTenant(tenantId, entityBO);
            DriverAttributeConfigVO entityVO = driverAttributeConfigBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * List every driver attribute config for a given device.
     *
     * @param deviceId id of the device whose configs are listed (must be tenant-owned)
     * @return a list of DriverAttributeConfigVO bound to the device
     */
    @PreAuthorize("@perm.can('driver_attribute_config', 'list')")
    @Operation(summary = "List Driver Attribute Configurations by Device ID", description = "Return every driver attribute config for a given device (tenant-scoped). " +
            "Use to load all connection attribute values a device applies to its driver in one call.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_by_device_id")
    public Mono<R<List<DriverAttributeConfigVO>>> listByDeviceId(
            @Parameter(description = "Identifier of the device whose driver attribute configs are being listed; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "device_id") Long deviceId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, deviceService.getById(deviceId));
            List<DriverAttributeConfigBO> entityBOList = filterTenant(tenantId, driverAttributeConfigService.listByDeviceId(deviceId));
            List<DriverAttributeConfigVO> entityVOList = driverAttributeConfigBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    /**
     * Page through driver attribute configs with filters.
     *
     * @param entityQuery query filters (may be null)
     * @return a page of DriverAttributeConfigVO matching the query
     */
    @PreAuthorize("@perm.can('driver_attribute_config', 'list')")
    @Operation(summary = "List Driver Attribute Configurations", description = "Page through driver attribute configs for the current tenant with query filters. " +
            "Returns a page of configured driver attribute values; use for browsing or selecting a target config.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<R<Page<DriverAttributeConfigVO>>> list(
            @RequestBody(required = false) DriverAttributeConfigQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverAttributeConfigQuery query = Objects.isNull(entityQuery) ? new DriverAttributeConfigQuery()
                    : entityQuery;
            query.setTenantId(tenantId);
            Page<DriverAttributeConfigBO> entityPageBO = driverAttributeConfigService.list(query);
            Page<DriverAttributeConfigVO> entityPageVO = driverAttributeConfigBuilder
                    .buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    /**
     * Validate that device and attribute belong to the tenant and share a driver.
     *
     * @param tenantId    tenant scope
     * @param deviceId    the device to validate
     * @param attributeId the attribute to validate
     */
    private void requireDriverConfigRelations(Long tenantId, Long deviceId, Long attributeId) {
        DeviceBO deviceBO = requireTenant(tenantId, deviceService.getById(deviceId));
        DriverAttributeBO attributeBO = requireTenant(tenantId, driverAttributeService.getById(attributeId));
        if (!Objects.equals(deviceBO.getDriverId(), attributeBO.getDriverId())) {
            throw new NotFoundException("Resource does not exist");
        }
    }

}
