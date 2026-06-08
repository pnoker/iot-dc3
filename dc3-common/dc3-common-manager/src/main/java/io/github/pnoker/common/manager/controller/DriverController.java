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
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.builder.DriverBuilder;
import io.github.pnoker.common.manager.entity.query.DriverQuery;
import io.github.pnoker.common.manager.entity.vo.DriverVO;
import io.github.pnoker.common.manager.service.DriverService;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing driver management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "driver", description = "驱动")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.DRIVER_URL_PREFIX)
@RequiredArgsConstructor
public class DriverController implements BaseController {

    private final DriverBuilder driverBuilder;

    private final DriverService driverService;

    /**
     * Driver
     *
     * @param entityVO {@link DriverVO}
     * @return R of String
     */
    @PreAuthorize("@perm.can('driver', 'add')")
    @Operation(summary = "新增驱动管理", description = "新增一条驱动记录")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody DriverVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverBO entityBO = driverBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            driverService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * ID Driver
     *
     * @param id ID
     * @return R of String
     */
    @PreAuthorize("@perm.can('driver', 'delete')")
    @Operation(summary = "删除驱动管理", description = "删除指定ID的驱动")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, driverService.getById(id));
            driverService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    /**
     * Driver
     *
     * @param entityVO {@link DriverVO}
     * @return R of String
     */
    @PreAuthorize("@perm.can('driver', 'update')")
    @Operation(summary = "更新驱动管理", description = "更新驱动信息")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody DriverVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverBO entityBO = driverBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, driverService.getById(entityBO.getId()));
            driverService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * ID Driver
     *
     * @param id ID
     * @return DriverVO {@link DriverVO}
     */
    @PreAuthorize("@perm.can('driver', 'get')")
    @Operation(summary = "查询驱动管理", description = "根据ID查询驱动管理详细信息")
    @GetMapping("/get_by_id")
    public Mono<R<DriverVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverBO entityBO = requireTenant(tenantId, driverService.getById(id));
            DriverVO entityVO = driverBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * ID Driver
     *
     * @param driverIds Driver ID
     * @return Map(ID, DriverVO)
     */
    @PreAuthorize("@perm.can('driver', 'list')")
    @PostMapping("/list_by_ids")
    public Mono<R<Map<Long, DriverVO>>> listByIds(@RequestBody Set<Long> driverIds) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            List<DriverBO> entityBOList = filterTenant(tenantId, driverService.listByIds(driverIds));
            Map<Long, DriverVO> driverMap = entityBOList.stream()
                    .collect(Collectors.toMap(DriverBO::getId, entityBO -> driverBuilder.buildVOByBO(entityBO)));
            return R.ok(driverMap);
        }));
    }

    /**
     * SERVICENAME Driver
     *
     * @param serviceName Driver service name
     * @return Driver
     */
    @PreAuthorize("@perm.can('driver', 'get')")
    @Operation(summary = "查询驱动管理", description = "根据条件查询驱动管理")
    @GetMapping("/get_by_service_name")
    public Mono<R<DriverVO>> getByServiceName(@NotNull @RequestParam(value = "service_name") String serviceName) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverBO entityBO = driverService.getByServiceName(serviceName, tenantId);
            DriverVO entityVO = driverBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Driver
     *
     * @param entityQuery Driver Dto
     * @return Page Of Driver
     */
    @PreAuthorize("@perm.can('driver', 'list')")
    @Operation(summary = "查询驱动列表", description = "分页查询驱动管理列表")
    @PostMapping("/list")
    public Mono<R<Page<DriverVO>>> list(@RequestBody(required = false) DriverQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverQuery query = Objects.isNull(entityQuery) ? new DriverQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<DriverBO> entityPageBO = driverService.list(query);
            Page<DriverVO> entityPageVO = driverBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}
