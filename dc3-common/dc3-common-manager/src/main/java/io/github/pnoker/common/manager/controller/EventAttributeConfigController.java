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
import io.github.pnoker.common.manager.entity.bo.EventAttributeBO;
import io.github.pnoker.common.manager.entity.bo.EventAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.common.manager.entity.builder.EventAttributeConfigBuilder;
import io.github.pnoker.common.manager.entity.query.EventAttributeConfigQuery;
import io.github.pnoker.common.manager.entity.vo.EventAttributeConfigVO;
import io.github.pnoker.common.manager.service.DeviceService;
import io.github.pnoker.common.manager.service.EventAttributeConfigService;
import io.github.pnoker.common.manager.service.EventAttributeService;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing event attribute config management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "event_attribute_config", description = "事件属性配置")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.EVENT_ATTRIBUTE_CONFIG_URL_PREFIX)
@RequiredArgsConstructor
public class EventAttributeConfigController implements BaseController {

    private final EventAttributeConfigBuilder eventAttributeConfigBuilder;

    private final EventAttributeConfigService eventAttributeConfigService;

    private final DeviceService deviceService;

    private final EventService eventService;

    private final EventAttributeService eventAttributeService;

    /**
     * EventConfig
     *
     * @param entityVO {@link EventAttributeConfigVO}
     * @return R of String
     */
    @PreAuthorize("@perm.can('event_attribute_config', 'add')")
    @Operation(summary = "新增事件属性配置", description = "新增一条事件属性配置记录")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody EventAttributeConfigVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventAttributeConfigBO entityBO = eventAttributeConfigBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            eventAttributeConfigService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * ID EventConfig
     *
     * @param id ID
     * @return R of String
     */
    @PreAuthorize("@perm.can('event_attribute_config', 'delete')")
    @Operation(summary = "删除事件属性配置", description = "删除指定ID的事件属性配置")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, eventAttributeConfigService.getById(id));
            eventAttributeConfigService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    /**
     * EventConfig
     *
     * @param entityVO {@link EventAttributeConfigVO}
     * @return R of String
     */
    @PreAuthorize("@perm.can('event_attribute_config', 'update')")
    @Operation(summary = "更新事件属性配置", description = "更新事件属性配置信息")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody EventAttributeConfigVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventAttributeConfigBO entityBO = eventAttributeConfigBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, eventAttributeConfigService.getById(entityBO.getId()));
            eventAttributeConfigService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * ID EventConfig
     *
     * @param id ID
     * @return EventAttributeConfigVO {@link EventAttributeConfigVO}
     */
    @PreAuthorize("@perm.can('event_attribute_config', 'get')")
    @Operation(summary = "查询事件属性配置", description = "根据ID查询事件属性配置详细信息")
    @GetMapping("/get_by_id")
    public Mono<R<EventAttributeConfigVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventAttributeConfigBO entityBO = requireTenant(tenantId, eventAttributeConfigService.getById(id));
            EventAttributeConfigVO entityVO = eventAttributeConfigBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * ID, Device ID Event ID EventConfig
     *
     * @param attributeId Attribute ID
     * @param deviceId    Device ID
     * @param eventId     Event ID
     * @return EventConfig
     */
    @PreAuthorize("@perm.can('event_attribute_config', 'get')")
    @Operation(summary = "查询事件属性配置", description = "根据属性ID、设备ID和事件ID查询事件属性配置")
    @GetMapping("/get_by_attribute_id_and_device_id_and_event_id")
    public Mono<R<EventAttributeConfigVO>> getByAttributeIdAndDeviceIdAndEventId(
            @NotNull @RequestParam(value = "attribute_id") Long attributeId,
            @NotNull @RequestParam(value = "device_id") Long deviceId,
            @NotNull @RequestParam(value = "event_id") Long eventId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireEventConfigRelations(tenantId, deviceId, eventId, attributeId);
            EventAttributeConfigBO entityBO = eventAttributeConfigService
                    .getByAttributeIdAndDeviceIdAndEventId(attributeId, deviceId, eventId);
            requireTenant(tenantId, entityBO);
            EventAttributeConfigVO entityVO = eventAttributeConfigBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Device ID Event ID EventConfig
     *
     * @param deviceId Device ID
     * @param eventId  Event ID
     * @return EventConfig
     */
    @PreAuthorize("@perm.can('event_attribute_config', 'list')")
    @Operation(summary = "查询事件属性配置列表", description = "根据设备ID和事件ID查询事件属性配置列表")
    @GetMapping("/list_by_device_id_and_event_id")
    public Mono<R<List<EventAttributeConfigVO>>> listByDeviceIdAndEventId(
            @NotNull @RequestParam(value = "device_id") Long deviceId,
            @NotNull @RequestParam(value = "event_id") Long eventId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireEventConfigRelations(tenantId, deviceId, eventId, null);
            List<EventAttributeConfigBO> entityBOList = filterTenant(tenantId,
                    eventAttributeConfigService.listByDeviceIdAndEventId(deviceId, eventId));
            List<EventAttributeConfigVO> entityVOList = eventAttributeConfigBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    /**
     * Device ID EventConfig
     *
     * @param deviceId Device ID
     * @return EventConfig
     */
    @PreAuthorize("@perm.can('event_attribute_config', 'list')")
    @Operation(summary = "查询事件属性配置列表", description = "根据设备ID查询事件属性配置列表")
    @GetMapping("/list_by_device_id")
    public Mono<R<List<EventAttributeConfigVO>>> listByDeviceId(
            @NotNull @RequestParam(value = "device_id") Long deviceId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, deviceService.getById(deviceId));
            List<EventAttributeConfigBO> entityBOList = filterTenant(tenantId,
                    eventAttributeConfigService.listByDeviceId(deviceId));
            List<EventAttributeConfigVO> entityVOList = eventAttributeConfigBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    /**
     * EventConfig
     *
     * @param entityQuery EventConfig Dto
     * @return Page Of EventConfig
     */
    @PreAuthorize("@perm.can('event_attribute_config', 'list')")
    @Operation(summary = "查询事件属性配置列表", description = "分页查询事件属性配置列表")
    @PostMapping("/list")
    public Mono<R<Page<EventAttributeConfigVO>>> list(
            @RequestBody(required = false) EventAttributeConfigQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventAttributeConfigQuery query = Objects.isNull(entityQuery) ? new EventAttributeConfigQuery()
                    : entityQuery;
            query.setTenantId(tenantId);
            Page<EventAttributeConfigBO> entityPageBO = eventAttributeConfigService.list(query);
            Page<EventAttributeConfigVO> entityPageVO = eventAttributeConfigBuilder
                    .buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    private void requireEventConfigRelations(Long tenantId, Long deviceId, Long eventId, Long attributeId) {
        DeviceBO deviceBO = requireTenant(tenantId, deviceService.getById(deviceId));
        EventBO eventBO = requireTenant(tenantId, eventService.getById(eventId));
        if (Objects.isNull(deviceBO.getProfileId()) || !Objects.equals(deviceBO.getProfileId(), eventBO.getProfileId())) {
            throw new NotFoundException("Resource does not exist");
        }

        if (Objects.nonNull(attributeId)) {
            EventAttributeBO attributeBO = requireTenant(tenantId, eventAttributeService.getById(attributeId));
            if (!Objects.equals(deviceBO.getDriverId(), attributeBO.getDriverId())) {
                throw new NotFoundException("Resource does not exist");
            }
        }
    }

}
