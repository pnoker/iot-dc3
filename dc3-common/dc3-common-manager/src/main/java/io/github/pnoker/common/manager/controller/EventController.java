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
import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.common.manager.entity.builder.EventBuilder;
import io.github.pnoker.common.manager.entity.query.EventQuery;
import io.github.pnoker.common.manager.entity.vo.EventVO;
import io.github.pnoker.common.manager.service.DeviceService;
import io.github.pnoker.common.manager.service.EventService;
import io.github.pnoker.common.manager.service.ProfileService;
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
 * REST controller exposing event management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "event", description = "事件")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.EVENT_URL_PREFIX)
@RequiredArgsConstructor
public class EventController implements BaseController {

    private final EventBuilder eventBuilder;

    private final EventService eventService;

    private final ProfileService profileService;

    private final DeviceService deviceService;

    @PreAuthorize("@perm.can('event', 'add')")
    @Operation(summary = "新增事件管理", description = "新增一条事件记录")
    @PostMapping("/add")
    public Mono<R<Long>> add(@Validated(Add.class) @RequestBody EventVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventBO entityBO = eventBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            eventService.add(entityBO);
            return R.ok(entityBO.getId());
        }));
    }

    @PreAuthorize("@perm.can('event', 'delete')")
    @Operation(summary = "删除事件管理", description = "删除指定ID的事件")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, eventService.getById(id));
            eventService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('event', 'update')")
    @Operation(summary = "更新事件管理", description = "更新事件信息")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody EventVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventBO entityBO = eventBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, eventService.getById(entityBO.getId()));
            eventService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('event', 'get')")
    @Operation(summary = "查询事件管理", description = "根据ID查询事件管理详细信息")
    @GetMapping("/get_by_id")
    public Mono<R<EventVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventBO entityBO = requireTenant(tenantId, eventService.getById(id));
            EventVO entityVO = eventBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    @PreAuthorize("@perm.can('event', 'list')")
    @Operation(summary = "查询事件列表", description = "根据关联条件查询事件管理列表")
    @GetMapping("/list_by_profile_id")
    public Mono<R<List<EventVO>>> listByProfileId(@NotNull @RequestParam(value = "profile_id") Long profileId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, profileService.getById(profileId));
            List<EventBO> entityBOList = filterTenant(tenantId, eventService.listByProfileId(profileId, tenantId));
            List<EventVO> entityVOList = eventBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    @PreAuthorize("@perm.can('event', 'list')")
    @Operation(summary = "查询事件列表", description = "根据关联条件查询事件管理列表")
    @GetMapping("/list_by_device_id")
    public Mono<R<List<EventVO>>> listByDeviceId(@NotNull @RequestParam(value = "device_id") Long deviceId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, deviceService.getById(deviceId));
            List<EventBO> entityBOList = filterTenant(tenantId, eventService.listByDeviceId(deviceId, tenantId));
            List<EventVO> entityVOList = eventBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    @PreAuthorize("@perm.can('event', 'list')")
    @Operation(summary = "查询事件列表", description = "分页查询事件管理列表")
    @PostMapping("/list")
    public Mono<R<Page<EventVO>>> list(@RequestBody(required = false) EventQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventQuery query = Objects.isNull(entityQuery) ? new EventQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<EventBO> entityPageBO = eventService.list(query);
            Page<EventVO> entityPageVO = eventBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}
