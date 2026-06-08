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
import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.common.manager.entity.builder.PointAttributeBuilder;
import io.github.pnoker.common.manager.entity.query.PointAttributeQuery;
import io.github.pnoker.common.manager.entity.vo.PointAttributeVO;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.PointAttributeService;
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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing point attribute management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "point_attribute", description = "位号属性")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.POINT_ATTRIBUTE_URL_PREFIX)
@RequiredArgsConstructor
public class PointAttributeController implements BaseController {

    private final PointAttributeBuilder pointAttributeBuilder;

    private final PointAttributeService pointAttributeService;

    private final DriverService driverService;

    /**
     * 位号属性
     *
     * @param entityVO {@link PointAttributeVO}
     * @return R of String
     */
    @PreAuthorize("@perm.can('point_attribute', 'add')")
    @Operation(summary = "新增PointAttribute", description = "新增一条PointAttribute记录")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody PointAttributeVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointAttributeBO entityBO = pointAttributeBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            pointAttributeService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * ID 位号属性
     *
     * @param id ID
     * @return R of String
     */
    @PreAuthorize("@perm.can('point_attribute', 'delete')")
    @Operation(summary = "删除PointAttribute", description = "删除指定ID的PointAttribute")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, pointAttributeService.getById(id));
            pointAttributeService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    /**
     * 位号属性
     *
     * @param entityVO {@link PointAttributeVO}
     * @return R of String
     */
    @PreAuthorize("@perm.can('point_attribute', 'update')")
    @Operation(summary = "更新PointAttribute", description = "更新PointAttribute信息")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody PointAttributeVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointAttributeBO entityBO = pointAttributeBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, pointAttributeService.getById(entityBO.getId()));
            pointAttributeService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * ID 位号属性
     *
     * @param id ID
     * @return PointAttributeVO {@link PointAttributeVO}
     */
    @PreAuthorize("@perm.can('point_attribute', 'get')")
    @Operation(summary = "查询PointAttribute", description = "根据ID查询PointAttribute详细信息")
    @GetMapping("/get_by_id")
    public Mono<R<PointAttributeVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointAttributeBO entityBO = requireTenant(tenantId, pointAttributeService.getById(id));
            PointAttributeVO entityVO = pointAttributeBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Driver ID 位号属性
     *
     * @param id ID
     * @return Array
     */
    @PreAuthorize("@perm.can('point_attribute', 'list')")
    @Operation(summary = "查询PointAttribute列表", description = "根据关联条件查询PointAttribute列表")
    @GetMapping("/list_by_driver_id")
    public Mono<R<List<PointAttributeVO>>> listByDriverId(@NotNull @RequestParam(value = "driver_id") Long driverId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            try {
                requireTenant(tenantId, driverService.getById(driverId));
                List<PointAttributeBO> entityBOList = filterTenant(tenantId, pointAttributeService.listByDriverId(driverId));
                List<PointAttributeVO> entityVO = pointAttributeBuilder.buildVOListByBOList(entityBOList);
                return R.ok(entityVO);
            } catch (NotFoundException ne) {
                return R.ok(Collections.emptyList());
            }
        }));
    }

    /**
     * 位号属性
     *
     * @param entityQuery
     * @return Page Of 位号属性
     */
    @PreAuthorize("@perm.can('point_attribute', 'list')")
    @Operation(summary = "查询PointAttribute列表", description = "分页查询PointAttribute列表")
    @PostMapping("/list")
    public Mono<R<Page<PointAttributeVO>>> list(@RequestBody(required = false) PointAttributeQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointAttributeQuery query = Objects.isNull(entityQuery) ? new PointAttributeQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<PointAttributeBO> entityPageBO = pointAttributeService.list(query);
            Page<PointAttributeVO> entityPageVO = pointAttributeBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}
