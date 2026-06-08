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
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.manager.entity.builder.DriverAttributeBuilder;
import io.github.pnoker.common.manager.entity.query.DriverAttributeQuery;
import io.github.pnoker.common.manager.entity.vo.DriverAttributeVO;
import io.github.pnoker.common.manager.service.DriverAttributeService;
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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * REST controller exposing driver attribute management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@RestController
@RequestMapping(ManagerConstant.DRIVER_ATTRIBUTE_URL_PREFIX)
@RequiredArgsConstructor
public class DriverAttributeController implements BaseController {

    private final DriverAttributeBuilder driverAttributeBuilder;

    private final DriverAttributeService driverAttributeService;

    private final DriverService driverService;

    /**
     * DriverAttribute
     *
     * @param entityVO {@link DriverAttributeVO}
     * @return R of String
     */
    @PreAuthorize("@perm.can('driver_attribute', 'add')")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody DriverAttributeVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverAttributeBO entityBO = driverAttributeBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            driverAttributeService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * ID DriverAttribute
     *
     * @param id ID
     * @return R of String
     */
    @PreAuthorize("@perm.can('driver_attribute', 'delete')")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, driverAttributeService.getById(id));
            driverAttributeService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    /**
     * DriverAttribute
     *
     * @param entityVO {@link DriverAttributeVO}
     * @return R of String
     */
    @PreAuthorize("@perm.can('driver_attribute', 'update')")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody DriverAttributeVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverAttributeBO entityBO = driverAttributeBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, driverAttributeService.getById(entityBO.getId()));
            driverAttributeService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * ID DriverAttribute
     *
     * @param id ID
     * @return DriverAttributeVO {@link DriverAttributeVO}
     */
    @PreAuthorize("@perm.can('driver_attribute', 'get')")
    @GetMapping("/get_by_id")
    public Mono<R<DriverAttributeVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverAttributeBO entityBO = requireTenant(tenantId, driverAttributeService.getById(id));
            DriverAttributeVO entityVO = driverAttributeBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Driver ID DriverAttribute
     *
     * @param id ID
     * @return DriverAttribute
     */
    @PreAuthorize("@perm.can('driver_attribute', 'list')")
    @GetMapping("/list_by_driver_id")
    public Mono<R<List<DriverAttributeVO>>> listByDriverId(@NotNull @RequestParam(value = "driver_id") Long driverId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            try {
                requireTenant(tenantId, driverService.getById(driverId));
                List<DriverAttributeBO> entityBOList = filterTenant(tenantId, driverAttributeService.listByDriverId(driverId));
                List<DriverAttributeVO> entityVO = driverAttributeBuilder.buildVOListByBOList(entityBOList);
                return R.ok(entityVO);
            } catch (NotFoundException ne) {
                return R.ok(Collections.emptyList());
            }
        }));
    }

    /**
     * DriverAttribute
     *
     * @param entityQuery Dto
     * @return Page Of DriverAttribute
     */
    @PreAuthorize("@perm.can('driver_attribute', 'list')")
    @PostMapping("/list")
    public Mono<R<Page<DriverAttributeVO>>> list(@RequestBody(required = false) DriverAttributeQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverAttributeQuery query = Objects.isNull(entityQuery) ? new DriverAttributeQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<DriverAttributeBO> entityPageBO = driverAttributeService.list(query);
            Page<DriverAttributeVO> entityPageVO = driverAttributeBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}
