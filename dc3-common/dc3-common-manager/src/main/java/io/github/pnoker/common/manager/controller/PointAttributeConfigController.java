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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing point attribute config management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "point_attribute_config", description = "位号属性配置")
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
     * PointConfig
     *
     * @param entityVO {@link PointAttributeConfigVO}
     * @return R of String
     */
    @PreAuthorize("@perm.can('point_attribute_config', 'add')")
    @Operation(summary = "新增位号属性配置", description = "新增一条位号属性配置记录")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody PointAttributeConfigVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointAttributeConfigBO entityBO = pointAttributeConfigBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            pointAttributeConfigService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * ID PointConfig
     *
     * @param id ID
     * @return R of String
     */
    @PreAuthorize("@perm.can('point_attribute_config', 'delete')")
    @Operation(summary = "删除位号属性配置", description = "删除指定ID的位号属性配置")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, pointAttributeConfigService.getById(id));
            pointAttributeConfigService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    /**
     * PointConfig
     *
     * @param entityVO {@link PointAttributeConfigVO}
     * @return R of String
     */
    @PreAuthorize("@perm.can('point_attribute_config', 'update')")
    @Operation(summary = "更新位号属性配置", description = "更新位号属性配置信息")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody PointAttributeConfigVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointAttributeConfigBO entityBO = pointAttributeConfigBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, pointAttributeConfigService.getById(entityBO.getId()));
            pointAttributeConfigService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * ID PointConfig
     *
     * @param id ID
     * @return PointAttributeConfigVO {@link PointAttributeConfigVO}
     */
    @PreAuthorize("@perm.can('point_attribute_config', 'get')")
    @Operation(summary = "查询位号属性配置", description = "根据ID查询位号属性配置详细信息")
    @GetMapping("/get_by_id")
    public Mono<R<PointAttributeConfigVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointAttributeConfigBO entityBO = requireTenant(tenantId, pointAttributeConfigService.getById(id));
            PointAttributeConfigVO entityVO = pointAttributeConfigBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * ID, Device ID Point ID PointConfig
     *
     * @param attributeId Attribute ID
     * @param deviceId    Device ID
     * @param pointId     Point ID
     * @return PointConfig
     */
    @PreAuthorize("@perm.can('point_attribute_config', 'get')")
    @Operation(summary = "查询位号属性配置", description = "根据属性ID、设备ID和位号ID查询位号属性配置")
    @GetMapping("/get_by_attribute_id_and_device_id_and_point_id")
    public Mono<R<PointAttributeConfigVO>> getByAttributeIdAndDeviceIdAndPointId(
            @NotNull @RequestParam(value = "attribute_id") Long attributeId,
            @NotNull @RequestParam(value = "device_id") Long deviceId,
            @NotNull @RequestParam(value = "point_id") Long pointId) {
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
     * Device ID Point ID PointConfig
     *
     * @param deviceId Device ID
     * @param pointId  Point ID
     * @return PointConfig
     */
    @PreAuthorize("@perm.can('point_attribute_config', 'list')")
    @Operation(summary = "查询位号属性配置列表", description = "根据设备ID和位号ID查询位号属性配置列表")
    @GetMapping("/list_by_device_id_and_point_id")
    public Mono<R<List<PointAttributeConfigVO>>> listByDeviceIdAndPointId(
            @NotNull @RequestParam(value = "device_id") Long deviceId,
            @NotNull @RequestParam(value = "point_id") Long pointId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requirePointConfigRelations(tenantId, deviceId, pointId, null);
            List<PointAttributeConfigBO> entityBOList = filterTenant(tenantId,
                    pointAttributeConfigService.listByDeviceIdAndPointId(deviceId, pointId));
            List<PointAttributeConfigVO> entityVOList = pointAttributeConfigBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    /**
     * Device ID PointConfig
     *
     * @param deviceId Device ID
     * @return PointConfig
     */
    @PreAuthorize("@perm.can('point_attribute_config', 'list')")
    @Operation(summary = "查询位号属性配置列表", description = "根据设备ID查询位号属性配置列表")
    @GetMapping("/list_by_device_id")
    public Mono<R<List<PointAttributeConfigVO>>> listByDeviceId(
            @NotNull @RequestParam(value = "device_id") Long deviceId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, deviceService.getById(deviceId));
            List<PointAttributeConfigBO> entityBOList = filterTenant(tenantId,
                    pointAttributeConfigService.listByDeviceId(deviceId));
            List<PointAttributeConfigVO> entityVOList = pointAttributeConfigBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    /**
     * PointConfig
     *
     * @param entityQuery PointConfig Dto
     * @return Page Of PointConfig
     */
    @PreAuthorize("@perm.can('point_attribute_config', 'list')")
    @Operation(summary = "查询位号属性配置列表", description = "分页查询位号属性配置列表")
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
