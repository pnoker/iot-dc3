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
import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.common.manager.entity.bo.PointAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.builder.PointAttributeConfigBuilder;
import io.github.pnoker.common.manager.entity.query.PointAttributeConfigQuery;
import io.github.pnoker.common.manager.entity.vo.PointAttributeConfigVO;
import io.github.pnoker.common.manager.service.DeviceService;
import io.github.pnoker.common.manager.service.PointAttributeConfigService;
import io.github.pnoker.common.manager.service.PointAttributeService;
import io.github.pnoker.common.manager.service.PointService;
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
 * REST controller exposing point attribute config management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "point_attribute_config", description = "Point attribute configuration values: set and update per-device customization values for data point properties inherited from point attribute definitions")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.POINT_ATTRIBUTE_CONFIG_URL_PREFIX)
@RequiredArgsConstructor
public class PointAttributeConfigController implements BaseController {

    private final PointAttributeConfigBuilder pointAttributeConfigBuilder;

    private final PointAttributeConfigService pointAttributeConfigService;

    private final DeviceService deviceService;

    private final PointService pointService;

    private final PointAttributeService pointAttributeService;

    /**
     * Create the configured value of a point attribute for a device-point pair.
     *
     * @param entityVO point attribute config payload to create (attribute, device, point, value)
     * @return add-success status
     */
    @PreAuthorize("@perm.can('point_attribute_config', 'add')")
    @Operation(summary = "Add Point Attribute Configuration", description = "Set the configured value of a point attribute on a specific device-point pair for the current tenant. " +
            "A point attribute config is the concrete value of a field declared on the profile template; returns the new config ID.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody PointAttributeConfigVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointAttributeConfigBO entityBO = pointAttributeConfigBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            pointAttributeConfigService.add(entityBO);
            return R.ok(SuccessCode.ADD);
        }));
    }

    /**
     * Delete a point attribute config by ID.
     *
     * @param id id of the point attribute config to delete (must be tenant-owned)
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('point_attribute_config', 'delete')")
    @Operation(summary = "Delete Point Attribute Configuration", description = "Permanently delete a point attribute config by ID (tenant-scoped). " +
            "Removes the configured value bound to a device-point pair; the action cannot be undone.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, pointAttributeConfigService.getById(id));
            pointAttributeConfigService.delete(id);
            return R.ok(SuccessCode.DELETE);
        }));
    }

    /**
     * Update the configured value of an existing point attribute config.
     *
     * @param entityVO point attribute config payload to update (must carry an existing id)
     * @return update-success status
     */
    @PreAuthorize("@perm.can('point_attribute_config', 'update')")
    @Operation(summary = "Update Point Attribute Configuration", description = "Change the configured value of an existing point attribute config (tenant-scoped). " +
            "Use to revise how a specific device-point pair reads or writes a point attribute through its driver.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody PointAttributeConfigVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointAttributeConfigBO entityBO = pointAttributeConfigBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, pointAttributeConfigService.getById(entityBO.getId()));
            pointAttributeConfigService.update(entityBO);
            return R.ok(SuccessCode.UPDATE);
        }));
    }

    /**
     * Fetch a single point attribute config by record ID.
     *
     * @param id id of the point attribute config to fetch (must be tenant-owned)
     * @return the matched PointAttributeConfigVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('point_attribute_config', 'get')")
    @Operation(summary = "Get Point Attribute Configuration by ID", description = "Fetch one point attribute config by its record ID (tenant-scoped). " +
            "Use to inspect the configured value bound to a device-point pair before updating or deleting it.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<R<PointAttributeConfigVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointAttributeConfigBO entityBO = requireTenant(tenantId, pointAttributeConfigService.getById(id));
            PointAttributeConfigVO entityVO = pointAttributeConfigBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Fetch the single config that applies one point attribute to a device-point pair.
     *
     * @param attributeId id of the point attribute whose configured value is read (its driver must match the device's)
     * @param deviceId    id of the device the config applies to (must be tenant-owned)
     * @param pointId     id of the data point the config applies to (must share its profile with the device)
     * @return the matched PointAttributeConfigVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('point_attribute_config', 'get')")
    @Operation(summary = "Get Point Attribute Configuration by Attribute, Device, and Point IDs", description = "Fetch the single config that applies one point attribute to a specific device-point pair. " +
            "Look up by the (attribute, device, point) tuple; returns the configured value for that exact binding.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_attribute_id_and_device_id_and_point_id")
    public Mono<R<PointAttributeConfigVO>> getByAttributeIdAndDeviceIdAndPointId(
            @Parameter(description = "Identifier of the point attribute definition whose configured value is being looked up; must belong to the current tenant's driver.", example = "1024") @NotNull @RequestParam(value = "attribute_id") Long attributeId,
            @Parameter(description = "Identifier of the device the config applies to; must belong to the current tenant and share its profile with the point.", example = "2048") @NotNull @RequestParam(value = "device_id") Long deviceId,
            @Parameter(description = "Identifier of the data point the config applies to; must belong to the current tenant and share its profile with the device.", example = "3072") @NotNull @RequestParam(value = "point_id") Long pointId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requirePointConfigRelations(tenantId, deviceId, pointId, attributeId);
            PointAttributeConfigBO entityBO = pointAttributeConfigService
                    .getByAttributeIdAndDeviceIdAndPointId(attributeId, deviceId, pointId);
            requireTenant(tenantId, entityBO);
            PointAttributeConfigVO entityVO = pointAttributeConfigBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * List every point attribute config bound to one device-point pair.
     *
     * @param deviceId id of the device whose configs are listed (must be tenant-owned)
     * @param pointId  id of the data point whose configs are listed (must share its profile with the device)
     * @return a list of PointAttributeConfigVO bound to the device-point pair
     */
    @PreAuthorize("@perm.can('point_attribute_config', 'list')")
    @Operation(summary = "List Point Attribute Configurations by Device and Point IDs", description = "Return every point attribute config bound to one device-point pair (tenant-scoped). " +
            "Use to see all configured attribute values that govern how that point is read or written on the device.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_by_device_id_and_point_id")
    public Mono<R<List<PointAttributeConfigVO>>> listByDeviceIdAndPointId(
            @Parameter(description = "Identifier of the device whose point configs are listed; must belong to the current tenant.", example = "2048") @NotNull @RequestParam(value = "device_id") Long deviceId,
            @Parameter(description = "Identifier of the data point whose attribute configs are listed; must share its profile with the device.", example = "3072") @NotNull @RequestParam(value = "point_id") Long pointId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requirePointConfigRelations(tenantId, deviceId, pointId, null);
            List<PointAttributeConfigBO> entityBOList = filterTenant(tenantId,
                    pointAttributeConfigService.listByDeviceIdAndPointId(deviceId, pointId));
            List<PointAttributeConfigVO> entityVOList = pointAttributeConfigBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    /**
     * List every point attribute config for one device across all its points.
     *
     * @param deviceId id of the device whose point attribute configs are listed (must be tenant-owned)
     * @return a list of PointAttributeConfigVO bound to the device
     */
    @PreAuthorize("@perm.can('point_attribute_config', 'list')")
    @Operation(summary = "List Point Attribute Configurations by Device ID", description = "Return every point attribute config for one device across all its points (tenant-scoped). " +
            "Returns a flat list; use to review the full configuration surface of a device at once.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_by_device_id")
    public Mono<R<List<PointAttributeConfigVO>>> listByDeviceId(
            @Parameter(description = "Identifier of the device whose point attribute configs are listed; must belong to the current tenant.", example = "2048") @NotNull @RequestParam(value = "device_id") Long deviceId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, deviceService.getById(deviceId));
            List<PointAttributeConfigBO> entityBOList = filterTenant(tenantId,
                    pointAttributeConfigService.listByDeviceId(deviceId));
            List<PointAttributeConfigVO> entityVOList = pointAttributeConfigBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    /**
     * Page through point attribute configs with filters.
     *
     * @param entityQuery query filters (may be null)
     * @return a page of PointAttributeConfigVO matching the query
     */
    @PreAuthorize("@perm.can('point_attribute_config', 'list')")
    @Operation(summary = "List Point Attribute Configurations", description = "Page through point attribute configs for the current tenant with query filters. " +
            "Returns a page of configs; use for browsing or locating a config when the device or point binding is unknown.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<R<Page<PointAttributeConfigVO>>> list(
            @RequestBody(required = false) PointAttributeConfigQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointAttributeConfigQuery query = Objects.isNull(entityQuery) ? new PointAttributeConfigQuery()
                    : entityQuery;
            query.setTenantId(tenantId);
            Page<PointAttributeConfigBO> entityPageBO = pointAttributeConfigService.list(query);
            Page<PointAttributeConfigVO> entityPageVO = pointAttributeConfigBuilder
                    .buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    /**
     * Validate that device, point, and (optional) attribute belong to the tenant, share
     * a profile, and that the attribute's driver matches the device's driver.
     *
     * @param tenantId    tenant scope
     * @param deviceId    the device to validate
     * @param pointId     the point to validate
     * @param attributeId the attribute to validate, may be null to skip
     */
    private void requirePointConfigRelations(Long tenantId, Long deviceId, Long pointId, Long attributeId) {
        DeviceBO deviceBO = requireTenant(tenantId, deviceService.getById(deviceId));
        PointBO pointBO = requireTenant(tenantId, pointService.getById(pointId));
        if (Objects.isNull(deviceBO.getProfileId()) || !Objects.equals(deviceBO.getProfileId(), pointBO.getProfileId())) {
            throw new NotFoundException("Resource does not exist");
        }

        if (Objects.nonNull(attributeId)) {
            PointAttributeBO attributeBO = requireTenant(tenantId, pointAttributeService.getById(attributeId));
            if (!Objects.equals(deviceBO.getDriverId(), attributeBO.getDriverId())) {
                throw new NotFoundException("Resource does not exist");
            }
        }
    }

}
