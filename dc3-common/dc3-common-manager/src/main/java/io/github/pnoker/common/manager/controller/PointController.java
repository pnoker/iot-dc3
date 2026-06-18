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
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.manager.entity.bo.DeviceByPointBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.bo.PointConfigByDeviceBO;
import io.github.pnoker.common.manager.entity.builder.DeviceBuilder;
import io.github.pnoker.common.manager.entity.builder.PointBuilder;
import io.github.pnoker.common.manager.entity.query.PointQuery;
import io.github.pnoker.common.manager.entity.vo.DeviceByPointVO;
import io.github.pnoker.common.manager.entity.vo.PointConfigByDeviceVO;
import io.github.pnoker.common.manager.entity.vo.PointVO;
import io.github.pnoker.common.manager.service.DeviceService;
import io.github.pnoker.common.manager.service.PointService;
import io.github.pnoker.common.manager.service.ProfileService;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * REST controller exposing point management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "point", description = "Data point definitions: manage measurable or controllable attributes of industrial devices including read/write mode, data type, and value range")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.POINT_URL_PREFIX)
@RequiredArgsConstructor
public class PointController implements BaseController {

    private final PointBuilder pointBuilder;

    private final PointService pointService;

    private final DeviceBuilder deviceBuilder;

    private final DeviceService deviceService;

    private final ProfileService profileService;

    /**
     * Point
     *
     * @param entityVO {@link PointVO}
     * @return R of String
     */
    @PreAuthorize("@perm.can('point', 'add')")
    @Operation(summary = "Add Point", description = "Define a new point (a single measurable channel such as a temperature reading) on a profile template for the current tenant. " +
            "Points are later attached to devices and read or written through the driver; returns the new point ID.")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody PointVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointBO entityBO = pointBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            pointService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * ID Point
     *
     * @param id ID
     * @return R of String
     */
    @PreAuthorize("@perm.can('point', 'delete')")
    @Operation(summary = "Delete Point", description = "Permanently remove a point from its profile template by ID (tenant-scoped). " +
            "The point definition is removed from every device that instantiates the profile; the action cannot be undone.")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, pointService.getById(id));
            pointService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    /**
     * Point
     *
     * @param entityVO {@link PointVO}
     * @return R of String
     */
    @PreAuthorize("@perm.can('point', 'update')")
    @Operation(summary = "Update Point", description = "Modify the definition of an existing point (name, data type, unit, access mode and similar fields) on its profile template. " +
            "Changes apply to every device that instantiates the profile; tenant ownership is verified before mutating.")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody PointVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointBO entityBO = pointBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, pointService.getById(entityBO.getId()));
            pointService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * ID Point
     *
     * @param id ID
     * @return PointVO {@link PointVO}
     */
    @PreAuthorize("@perm.can('point', 'get')")
    @Operation(summary = "Get Point by ID", description = "Fetch a single point's definition (data type, unit, access mode and metadata) by ID. " +
            "Tenant-scoped; use to inspect a point before reading its values or binding it to a device.")
    @GetMapping("/get_by_id")
    public Mono<R<PointVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointBO entityBO = requireTenant(tenantId, pointService.getById(id));
            PointVO entityVO = pointBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * ID Point
     *
     * @param pointIds Point ID
     * @return Map(ID, PointVO)
     */
    @PreAuthorize("@perm.can('point', 'list')")
    @Operation(summary = "List Points by IDs", description = "Resolve a batch of point IDs to their definitions for the current tenant. " +
            "Returns a map of point ID to point VO; IDs the tenant does not own are filtered out, so callers should treat missing keys as not-found.")
    @PostMapping("/list_by_ids")
    public Mono<R<Map<Long, PointVO>>> listByIds(@RequestBody Set<Long> pointIds) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            List<PointBO> entityBOList = filterTenant(tenantId, pointService.listByIds(pointIds));
            Map<Long, PointVO> deviceMap = entityBOList.stream()
                    .collect(Collectors.toMap(PointBO::getId, entityBO -> pointBuilder.buildVOByBO(entityBO)));
            return R.ok(deviceMap);
        }));
    }

    /**
     * ID Point
     *
     * @param profileId Point ID
     * @return Point
     */
    @PreAuthorize("@perm.can('point', 'list')")
    @Operation(summary = "List Points by Profile ID", description = "Return every point defined on a given profile template (tenant-scoped). " +
            "Use to enumerate the measurable channels a device will expose once it instantiates the profile; the profile must belong to the tenant.")
    @GetMapping("/list_by_profile_id")
    public Mono<R<List<PointVO>>> listByProfileId(@Parameter(description = "Identifier of the profile template whose points should be listed; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "profile_id") Long profileId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, profileService.getById(profileId));
            List<PointBO> entityBOList = filterTenant(tenantId, pointService.listByProfileId(profileId, tenantId));
            List<PointVO> entityVOList = pointBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    /**
     * Device ID Point
     *
     * @param deviceId Device ID
     * @return Point Array
     */
    @PreAuthorize("@perm.can('point', 'list')")
    @Operation(summary = "List Points by Device ID", description = "Return every point available on a specific device (tenant-scoped). " +
            "These are the channels the bound driver can read or write for that device; the device must belong to the tenant.")
    @GetMapping("/list_by_device_id")
    public Mono<R<List<PointVO>>> listByDeviceId(@Parameter(description = "Identifier of the device whose available points should be listed; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "device_id") Long deviceId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, deviceService.getById(deviceId));
            List<PointBO> entityBOList = filterTenant(tenantId, pointService.listByDeviceId(deviceId, tenantId));
            List<PointVO> entityVOList = pointBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    /**
     * Point
     *
     * @param entityQuery Point Dto
     * @return Page Of Point
     */
    @PreAuthorize("@perm.can('point', 'list')")
    @Operation(summary = "List Points", description = "Page through points for the current tenant with filters such as name and profile. " +
            "Returns a page of point definitions; use for browsing points or selecting targets for value reads, writes or device binding.")
    @PostMapping("/list")
    public Mono<R<Page<PointVO>>> list(@RequestBody(required = false) PointQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointQuery query = Objects.isNull(entityQuery) ? new PointQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<PointBO> entityPageBO = pointService.list(query);
            Page<PointVO> entityPageVO = pointBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    /**
     * @param pointIds Point ID
     * @return Map String:String
     */
    @PreAuthorize("@perm.can('point', 'list')")
    @Operation(summary = "List Point Units", description = "Resolve the engineering unit (for example Celsius or percent) of each point in a batch of IDs (tenant-scoped). " +
            "Returns a map of point ID to unit string; only tenant-owned points are included.")
    @PostMapping("/unit")
    public Mono<R<Map<Long, String>>> unit(@RequestBody Set<Long> pointIds) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            Set<Long> scopedPointIds = filterTenant(tenantId, pointService.listByIds(pointIds)).stream()
                    .map(PointBO::getId)
                    .collect(Collectors.toSet());
            Map<Long, String> units = pointService.unit(scopedPointIds);
            if (Objects.nonNull(units)) {
                Map<Long, String> unitCodeMap = units.entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                return R.ok(unitCodeMap);
            }
            return R.fail();
        }));
    }

    /**
     * @param pointId id
     * @return {@link R}<{@link Set}<{@link Long}>>
     */
    @PreAuthorize("@perm.can('point', 'list')")
    @Operation(summary = "Get Point Device Statistics", description = "Return device-level statistics for a single point, such as how many devices expose it (tenant-scoped). " +
            "Use to gauge the blast radius of editing a point before changing its definition; the point must belong to the tenant.")
    @GetMapping("/list_device_statistics_by_point_id")
    public Mono<R<DeviceByPointVO>> getPointStatisticsWithDevice(
            @Parameter(description = "Identifier of the point whose device statistics should be returned; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "point_id") Long pointId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, pointService.getById(pointId));
            DeviceByPointBO deviceByPointBO = pointService.getPointStatisticsWithDevice(pointId);
            DeviceByPointVO deviceByPointVO = deviceBuilder.buildVOPointByBO(deviceByPointBO);
            return R.ok(deviceByPointVO);
        }));
    }

    /**
     * @param deviceId
     * @return
     */
    @PreAuthorize("@perm.can('point', 'list')")
    @Operation(summary = "Count Points by Device", description = "Return the number of points available on a specific device (tenant-scoped). " +
            "Use for quick cardinality checks without fetching full definitions; the device must belong to the tenant.")
    @GetMapping("/get_count_by_device_id")
    public Mono<R<Long>> getPointByDeviceId(@Parameter(description = "Identifier of the device whose point count should be returned; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "device_id") Long deviceId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, deviceService.getById(deviceId));
            Long count = pointService.getPointByDeviceId(deviceId);
            return R.ok(count);
        }));
    }

    /**
     * @param deviceId
     * @return
     */
    @PreAuthorize("@perm.can('point', 'get')")
    @Operation(summary = "Get Device Point Configuration", description = "Fetch the resolved point configuration for a device, merging the profile template definitions with the device's per-instance attribute values (tenant-scoped). " +
            "Use to see exactly how each point is configured before issuing reads, writes or commands through the driver.")
    @GetMapping("/get_point_config_by_device_id")
    public Mono<R<PointConfigByDeviceVO>> getPointConfigByDeviceId(
            @Parameter(description = "Identifier of the device whose resolved point configuration should be returned; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "device_id") Long deviceId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, deviceService.getById(deviceId));
            PointConfigByDeviceBO pointConfigByDeviceBO = pointService.getPointConfigByDeviceId(deviceId);
            PointConfigByDeviceVO pointConfigByDeviceVO = pointBuilder.buildVODeviceByBO(pointConfigByDeviceBO);
            return R.ok(pointConfigByDeviceVO);
        }));
    }

}
