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
import io.github.pnoker.common.manager.entity.bo.EventParamBO;
import io.github.pnoker.common.manager.entity.builder.EventParamBuilder;
import io.github.pnoker.common.manager.entity.query.EventParamQuery;
import io.github.pnoker.common.manager.entity.vo.EventParamVO;
import io.github.pnoker.common.manager.service.EventParamService;
import io.github.pnoker.common.manager.service.EventService;
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

/**
 * REST controller exposing event param management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@RestController
@RequestMapping(ManagerConstant.EVENT_PARAM_URL_PREFIX)
@RequiredArgsConstructor
public class EventParamController implements BaseController {

    private final EventParamBuilder eventParamBuilder;

    private final EventParamService eventParamService;

    private final EventService eventService;

    @PreAuthorize("@perm.can('event_param', 'add')")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody EventParamVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventParamBO entityBO = eventParamBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            eventParamService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('event_param', 'delete')")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, eventParamService.getById(id));
            eventParamService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('event_param', 'update')")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody EventParamVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventParamBO entityBO = eventParamBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, eventParamService.getById(entityBO.getId()));
            eventParamService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('event_param', 'get')")
    @GetMapping("/get_by_id")
    public Mono<R<EventParamVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventParamBO entityBO = requireTenant(tenantId, eventParamService.getById(id));
            EventParamVO entityVO = eventParamBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    @PreAuthorize("@perm.can('event_param', 'list')")
    @GetMapping("/list_by_event_id")
    public Mono<R<List<EventParamVO>>> listByEventId(@NotNull @RequestParam(value = "event_id") Long eventId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, eventService.getById(eventId));
            List<EventParamBO> entityBOList = filterTenant(tenantId, eventParamService.listByEventId(eventId));
            List<EventParamVO> entityVOList = eventParamBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    @PreAuthorize("@perm.can('event_param', 'list')")
    @PostMapping("/list")
    public Mono<R<Page<EventParamVO>>> list(@RequestBody(required = false) EventParamQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventParamQuery query = Objects.isNull(entityQuery) ? new EventParamQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<EventParamBO> entityPageBO = eventParamService.list(query);
            Page<EventParamVO> entityPageVO = eventParamBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}
