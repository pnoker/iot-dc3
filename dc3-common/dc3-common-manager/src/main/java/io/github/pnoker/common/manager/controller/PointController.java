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
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.builder.DeviceBuilder;
import io.github.pnoker.common.manager.entity.builder.PointBuilder;
import io.github.pnoker.common.manager.entity.query.PointOffsetQuery;
import io.github.pnoker.common.manager.entity.vo.DeviceByPointVO;
import io.github.pnoker.common.manager.entity.vo.PointConfigByDeviceVO;
import io.github.pnoker.common.manager.entity.vo.PointVO;
import io.github.pnoker.common.manager.service.ReactivePointService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST controller exposing point management endpoints.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Tag(name = "point", description = "Data point definitions: manage measurable or controllable attributes of industrial devices including read/write mode, data type, and value range")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.POINT_URL_PREFIX)
@RequiredArgsConstructor
public class PointController implements BaseController {

    private final PointBuilder pointBuilder;
    private final DeviceBuilder deviceBuilder;

    private final ReactivePointService reactivePointService;

    /**
     * Define a new point on a profile template for the current tenant, then return the add-success status.
     *
     * @param entityVO point payload to create
     * @return add-success status
     */
    @PreAuthorize("@perm.can('point', 'add')")
    @Operation(summary = "Add Point", description = "Define a new point (a single measurable channel such as a temperature reading) on a profile template for the current tenant. " +
            "Points are later attached to devices and read or written through the driver; returns the new point ID.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<PointVO> add(@Validated(Add.class) @RequestBody PointVO entityVO) {
        return getTenantId().zipWith(getUserId().defaultIfEmpty(0L)).zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> {
            Long tenantId = tuple.getT1().getT1();
            PointBO entityBO = pointBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            entityBO.setCreatorId(tuple.getT1().getT2()); entityBO.setCreatorName(tuple.getT2()); entityBO.setOperatorId(tuple.getT1().getT2()); entityBO.setOperatorName(tuple.getT2());
            return reactivePointService.add(entityBO).map(pointBuilder::buildVOByBO);
        });
    }

    /**
     * Delete a point after verifying it belongs to the current tenant, then return the delete-success status.
     *
     * @param id id of the point to delete
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('point', 'delete')")
    @Operation(summary = "Delete Point", description = "Permanently remove a point from its profile template by ID (tenant-scoped). " +
            "The point definition is removed from every device that instantiates the profile; the action cannot be undone.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @DeleteMapping("/delete")
    public Mono<Void> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id,
                             @Parameter(description = "Current optimistic-lock version required as a deletion precondition.", example = "0") @NotNull @Min(0) @RequestParam("version") Integer version) {
        return getTenantId().zipWith(getUserId().defaultIfEmpty(0L)).zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> reactivePointService.delete(tuple.getT1().getT1(), id, version, tuple.getT1().getT2(), tuple.getT2()).then());
    }

    /**
     * Update an existing point after verifying tenant ownership, then return the update-success status.
     *
     * @param entityVO point payload to update
     * @return update-success status
     */
    @PreAuthorize("@perm.can('point', 'update')")
    @Operation(summary = "Update Point", description = "Modify the definition of an existing point (name, data type, unit, access mode and similar fields) on its profile template. " +
            "Changes apply to every device that instantiates the profile; tenant ownership is verified before mutating.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/update")
    public Mono<PointVO> update(@Validated(Update.class) @RequestBody PointVO entityVO) {
        return getTenantId().zipWith(getUserId().defaultIfEmpty(0L)).zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> {
            Long tenantId = tuple.getT1().getT1();
            PointBO entityBO = pointBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            entityBO.setOperatorId(tuple.getT1().getT2()); entityBO.setOperatorName(tuple.getT2());
            return reactivePointService.update(entityBO).map(pointBuilder::buildVOByBO);
        });
    }

    /**
     * Fetch one point's definition by ID after verifying it belongs to the current tenant.
     *
     * @param id id of the point to fetch
     * @return the matched PointVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('point', 'get')")
    @Operation(summary = "Get Point by ID", description = "Fetch a single point's definition (data type, unit, access mode and metadata) by ID. " +
            "Tenant-scoped; use to inspect a point before reading its values or binding it to a device.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<PointVO> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> reactivePointService.getById(tenantId, id).map(pointBuilder::buildVOByBO));
    }

    /**
     * Resolve a batch of point IDs to their definitions, filtered to the current tenant.
     *
     * @param pointIds ids of the points to resolve
     * @return a map of id to PointVO for the tenant-owned matched ids
     */
    @PreAuthorize("@perm.can('point', 'list')")
    @Operation(summary = "List Points by IDs", description = "Resolve a batch of point IDs to their definitions for the current tenant. " +
            "Returns a map of point ID to point VO; IDs the tenant does not own are filtered out, so callers should treat missing keys as not-found.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list_by_ids")
    public Mono<Map<String, PointVO>> listByIds(@RequestBody Set<Long> pointIds) {
        return getTenantId().flatMap(tenantId -> reactivePointService.listByIds(tenantId, pointIds == null ? List.of() : List.copyOf(pointIds)).collectMap(bo -> String.valueOf(bo.getId()), pointBuilder::buildVOByBO));
    }

    /**
     * List every point defined on a given profile template, filtered to the current tenant.
     *
     * @param profileId id of the profile template whose points are returned
     * @return a list of PointVO defined on the profile
     */
    @PreAuthorize("@perm.can('point', 'list')")
    @Operation(summary = "List Points by Profile ID", description = "Return every point defined on a given profile template (tenant-scoped). " +
            "Use to enumerate the measurable channels a device will expose once it instantiates the profile; the profile must belong to the tenant.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_by_profile_id")
    public Mono<List<PointVO>> listByProfileId(@Parameter(description = "Identifier of the profile template whose points should be listed; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "profile_id") Long profileId) {
        return getTenantId().flatMap(tenantId -> reactivePointService.listByProfileId(tenantId, profileId).map(pointBuilder::buildVOByBO).collectList());
    }

    /**
     * List every point available on a given device, filtered to the current tenant.
     *
     * @param deviceId id of the device whose points are returned
     * @return a list of PointVO available on the device
     */
    @PreAuthorize("@perm.can('point', 'list')")
    @Operation(summary = "List Points by Device ID", description = "Return every point available on a specific device (tenant-scoped). " +
            "These are the channels the bound driver can read or write for that device; the device must belong to the tenant.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_by_device_id")
    public Mono<List<PointVO>> listByDeviceId(@Parameter(description = "Identifier of the device whose available points should be listed; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "device_id") Long deviceId) {
        return getTenantId().flatMap(tenantId -> reactivePointService.listByDeviceId(tenantId, deviceId).map(pointBuilder::buildVOByBO).collectList());
    }

    /** Canonical offset-based point listing. New clients must use this contract. */
    @PreAuthorize("@perm.can('point', 'list')")
    @Operation(summary = "List Points", description = "List tenant-scoped points with offset pagination and explicit sorting.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<OffsetPage<PointVO>> list(@RequestBody(required = false) PointOffsetQuery request) {
        PointOffsetQuery query = request == null
                ? new PointOffsetQuery(0L, 50, List.of(), null, null, null, null, null, null, null, null, null, null)
                : request;
        long offset = query.offset() == null ? 0L : query.offset();
        int limit = query.limit() == null ? 50 : query.limit();
        return getTenantId().flatMap(tenantId -> reactivePointService.list(new io.github.pnoker.common.manager.repository.PointFilter(
                        tenantId, query.pointName(), query.pointCode(), query.pointTypeFlag(), query.rwFlag(),
                        query.profileId(), query.enableFlag(), query.groupId(), query.labelId(), query.version(),
                        query.deviceId(), offset, limit, query.sort()))
                .map(page -> OffsetPage.of(page.items().stream().map(pointBuilder::buildVOByBO).toList(),
                        page.offset(), page.limit(), page.total())));
    }

    /**
     * Resolve the engineering unit of each point in a batch of IDs, filtered to the current tenant.
     *
     * @param pointIds ids of the points whose units should be resolved
     * @return a map of id to unit string for the tenant-owned matched points; fails when the resolution is empty
     */
    @PreAuthorize("@perm.can('point', 'list')")
    @Operation(summary = "List Point Units", description = "Resolve the engineering unit (for example Celsius or percent) of each point in a batch of IDs (tenant-scoped). " +
            "Returns a map of point ID to unit string; only tenant-owned points are included.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list_units")
    public Mono<Map<String, String>> listUnits(@RequestBody Set<Long> pointIds) {
        return getTenantId().flatMap(tenantId -> reactivePointService.listUnits(tenantId,
                pointIds == null ? List.of() : List.copyOf(pointIds)));
    }

    /**
     * Return device-level statistics for a single point after verifying it belongs to the current tenant.
     *
     * @param pointId id of the point whose device statistics are returned
     * @return a DeviceByPointVO carrying the point's device statistics
     */
    @PreAuthorize("@perm.can('point', 'list')")
    @Operation(summary = "Get Point Device Statistics", description = "Return device-level statistics for a single point, such as how many devices expose it (tenant-scoped). " +
            "Use to gauge the blast radius of editing a point before changing its definition; the point must belong to the tenant.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_device_statistics_by_point_id")
    public Mono<DeviceByPointVO> getDeviceStatisticsByPointId(
            @Parameter(description = "Identifier of the point whose device statistics should be returned; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "point_id") Long pointId) {
        return getTenantId().flatMap(tenantId -> reactivePointService.getDeviceStatisticsByPointId(tenantId, pointId)
                .map(deviceBuilder::buildVOPointByBO));
    }

    /**
     * Count the points available on a given device after verifying it belongs to the current tenant.
     *
     * @param deviceId id of the device whose point count is returned
     * @return the number of points available on the device
     */
    @PreAuthorize("@perm.can('point', 'list')")
    @Operation(summary = "Count Points by Device", description = "Return the number of points available on a specific device (tenant-scoped). " +
            "Use for quick cardinality checks without fetching full definitions; the device must belong to the tenant.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_count_by_device_id")
    public Mono<Long> getCountByDeviceId(@Parameter(description = "Identifier of the device whose point count should be returned; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "device_id") Long deviceId) {
        return getTenantId().flatMap(tenantId -> reactivePointService.getCountByDeviceId(tenantId, deviceId));
    }

    /**
     * Fetch the resolved point configuration for a device after verifying it belongs to the current tenant.
     *
     * @param deviceId id of the device whose resolved point configuration is returned
     * @return a PointConfigByDeviceVO merging profile template definitions with device instance attribute values
     */
    @PreAuthorize("@perm.can('point', 'get')")
    @Operation(summary = "Get Device Point Configuration", description = "Fetch the resolved point configuration for a device, merging the profile template definitions with the device's per-instance attribute values (tenant-scoped). " +
            "Use to see exactly how each point is configured before issuing reads, writes or commands through the driver.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_point_config_by_device_id")
    public Mono<PointConfigByDeviceVO> getPointConfigByDeviceId(
            @Parameter(description = "Identifier of the device whose resolved point configuration should be returned; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "device_id") Long deviceId) {
        return getTenantId().flatMap(tenantId -> reactivePointService.getPointConfigByDeviceId(tenantId, deviceId)
                .map(pointBuilder::buildVODeviceByBO));
    }

}
