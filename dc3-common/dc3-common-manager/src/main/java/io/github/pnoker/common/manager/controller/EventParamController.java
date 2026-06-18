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
import java.util.Objects;

/**
 * REST controller exposing event param management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "event_param", description = "Event parameter definitions: manage data payload specifications for device events including name, type, and value mapping")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.EVENT_PARAM_URL_PREFIX)
@RequiredArgsConstructor
public class EventParamController implements BaseController {

    private final EventParamBuilder eventParamBuilder;

    private final EventParamService eventParamService;

    private final EventService eventService;

    @PreAuthorize("@perm.can('event_param', 'add')")
    @Operation(summary = "Add Event Parameter", description = "Declare a new parameter on an event for the current tenant. An event param is a named field an event exposes (with type and extension); returns the new event param ID.")
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
    @Operation(summary = "Delete Event Parameter", description = "Permanently delete an event param by ID (tenant-scoped, ownership verified before deletion). The parameter is removed from its event; the action cannot be undone.")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, eventParamService.getById(id));
            eventParamService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('event_param', 'update')")
    @Operation(summary = "Update Event Parameter", description = "Modify an existing event param's name, code, type or extension (tenant-scoped, ownership verified before mutation). Use to correct a parameter declared on an event.")
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
    @Operation(summary = "Get Event Parameter by ID", description = "Fetch one event param with its name, code, type and extension. Use to inspect a parameter a device-reported event exposes before reading event values.")
    @GetMapping("/get_by_id")
    public Mono<R<EventParamVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventParamBO entityBO = requireTenant(tenantId, eventParamService.getById(id));
            EventParamVO entityVO = eventParamBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    @PreAuthorize("@perm.can('event_param', 'list')")
    @Operation(summary = "List Event Parameters by Event ID", description = "Return every parameter declared on a given event (tenant-scoped, event ownership verified). Use to discover which fields a device-reported event exposes.")
    @GetMapping("/list_by_event_id")
    public Mono<R<List<EventParamVO>>> listByEventId(@Parameter(description = "Identifier of the event whose parameters are listed; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "event_id") Long eventId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, eventService.getById(eventId));
            List<EventParamBO> entityBOList = filterTenant(tenantId, eventParamService.listByEventId(eventId));
            List<EventParamVO> entityVOList = eventParamBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    @PreAuthorize("@perm.can('event_param', 'list')")
    @Operation(summary = "List Event Parameters", description = "Page through event params for the current tenant with filters from the query body. Returns a page of event params; use for browsing or auditing parameter declarations across events.")
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
