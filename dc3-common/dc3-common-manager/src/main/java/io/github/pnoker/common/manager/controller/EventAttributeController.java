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
import io.github.pnoker.common.manager.entity.bo.EventAttributeBO;
import io.github.pnoker.common.manager.entity.builder.EventAttributeBuilder;
import io.github.pnoker.common.manager.entity.query.EventAttributeQuery;
import io.github.pnoker.common.manager.entity.vo.EventAttributeVO;
import io.github.pnoker.common.manager.service.EventAttributeService;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * REST controller exposing event attribute management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@RestController
@RequestMapping(ManagerConstant.EVENT_ATTRIBUTE_URL_PREFIX)
@RequiredArgsConstructor
public class EventAttributeController implements BaseController {

    private final EventAttributeBuilder eventAttributeBuilder;

    private final EventAttributeService eventAttributeService;

    private final DriverService driverService;

    /**
     * EventAttribute
     *
     * @param entityVO {@link EventAttributeVO}
     * @return R of String
     */
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody EventAttributeVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventAttributeBO entityBO = eventAttributeBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            eventAttributeService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * ID EventAttribute
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, eventAttributeService.getById(id));
            eventAttributeService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    /**
     * EventAttribute
     *
     * @param entityVO {@link EventAttributeVO}
     * @return R of String
     */
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody EventAttributeVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventAttributeBO entityBO = eventAttributeBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, eventAttributeService.getById(entityBO.getId()));
            eventAttributeService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * ID EventAttribute
     *
     * @param id ID
     * @return EventAttributeVO {@link EventAttributeVO}
     */
    @GetMapping("/get_by_id")
    public Mono<R<EventAttributeVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventAttributeBO entityBO = requireTenant(tenantId, eventAttributeService.getById(id));
            EventAttributeVO entityVO = eventAttributeBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Driver ID EventAttribute
     *
     * @param id ID
     * @return EventAttribute
     */
    @GetMapping("/list_by_driver_id")
    public Mono<R<List<EventAttributeVO>>> listByDriverId(@NotNull @RequestParam(value = "driver_id") Long driverId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            try {
                requireTenant(tenantId, driverService.getById(driverId));
                List<EventAttributeBO> entityBOList = filterTenant(tenantId, eventAttributeService.listByDriverId(driverId));
                List<EventAttributeVO> entityVO = eventAttributeBuilder.buildVOListByBOList(entityBOList);
                return R.ok(entityVO);
            } catch (NotFoundException ne) {
                return R.ok(Collections.emptyList());
            }
        }));
    }

    /**
     * EventAttribute
     *
     * @param entityQuery Dto
     * @return Page Of EventAttribute
     */
    @PostMapping("/list")
    public Mono<R<Page<EventAttributeVO>>> list(@RequestBody(required = false) EventAttributeQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventAttributeQuery query = Objects.isNull(entityQuery) ? new EventAttributeQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<EventAttributeBO> entityPageBO = eventAttributeService.list(query);
            Page<EventAttributeVO> entityPageVO = eventAttributeBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}
