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
import io.github.pnoker.common.manager.entity.bo.CommandAttributeBO;
import io.github.pnoker.common.manager.entity.bo.CommandAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.builder.CommandAttributeConfigBuilder;
import io.github.pnoker.common.manager.entity.query.CommandAttributeConfigQuery;
import io.github.pnoker.common.manager.entity.vo.CommandAttributeConfigVO;
import io.github.pnoker.common.manager.service.CommandAttributeConfigService;
import io.github.pnoker.common.manager.service.CommandAttributeService;
import io.github.pnoker.common.manager.service.CommandService;
import io.github.pnoker.common.manager.service.DeviceService;
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
 * REST controller exposing command attribute config management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@RestController
@RequestMapping(ManagerConstant.COMMAND_ATTRIBUTE_CONFIG_URL_PREFIX)
@RequiredArgsConstructor
public class CommandAttributeConfigController implements BaseController {

    private final CommandAttributeConfigBuilder commandAttributeConfigBuilder;

    private final CommandAttributeConfigService commandAttributeConfigService;

    private final DeviceService deviceService;

    private final CommandService commandService;

    private final CommandAttributeService commandAttributeService;

    /**
     * CommandConfig
     *
     * @param entityVO {@link CommandAttributeConfigVO}
     * @return R of String
     */
    @PreAuthorize("@perm.can('command_attribute_config', 'add')")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody CommandAttributeConfigVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandAttributeConfigBO entityBO = commandAttributeConfigBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            commandAttributeConfigService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * ID CommandConfig
     *
     * @param id ID
     * @return R of String
     */
    @PreAuthorize("@perm.can('command_attribute_config', 'delete')")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, commandAttributeConfigService.getById(id));
            commandAttributeConfigService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    /**
     * CommandConfig
     *
     * @param entityVO {@link CommandAttributeConfigVO}
     * @return R of String
     */
    @PreAuthorize("@perm.can('command_attribute_config', 'update')")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody CommandAttributeConfigVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandAttributeConfigBO entityBO = commandAttributeConfigBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, commandAttributeConfigService.getById(entityBO.getId()));
            commandAttributeConfigService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * ID CommandConfig
     *
     * @param id ID
     * @return CommandAttributeConfigVO {@link CommandAttributeConfigVO}
     */
    @PreAuthorize("@perm.can('command_attribute_config', 'get')")
    @GetMapping("/get_by_id")
    public Mono<R<CommandAttributeConfigVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandAttributeConfigBO entityBO = requireTenant(tenantId, commandAttributeConfigService.getById(id));
            CommandAttributeConfigVO entityVO = commandAttributeConfigBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * ID, Device ID Command ID CommandConfig
     *
     * @param attributeId Attribute ID
     * @param deviceId    Device ID
     * @param commandId   Command ID
     * @return CommandConfig
     */
    @PreAuthorize("@perm.can('command_attribute_config', 'get')")
    @GetMapping("/get_by_attribute_id_and_device_id_and_command_id")
    public Mono<R<CommandAttributeConfigVO>> getByAttributeIdAndDeviceIdAndCommandId(
            @NotNull @RequestParam(value = "attribute_id") Long attributeId,
            @NotNull @RequestParam(value = "device_id") Long deviceId,
            @NotNull @RequestParam(value = "command_id") Long commandId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireCommandConfigRelations(tenantId, deviceId, commandId, attributeId);
            CommandAttributeConfigBO entityBO = commandAttributeConfigService
                    .getByAttributeIdAndDeviceIdAndCommandId(attributeId, deviceId, commandId);
            requireTenant(tenantId, entityBO);
            CommandAttributeConfigVO entityVO = commandAttributeConfigBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Device ID Command ID CommandConfig
     *
     * @param deviceId  Device ID
     * @param commandId Command ID
     * @return CommandConfig
     */
    @PreAuthorize("@perm.can('command_attribute_config', 'list')")
    @GetMapping("/list_by_device_id_and_command_id")
    public Mono<R<List<CommandAttributeConfigVO>>> listByDeviceIdAndCommandId(
            @NotNull @RequestParam(value = "device_id") Long deviceId,
            @NotNull @RequestParam(value = "command_id") Long commandId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireCommandConfigRelations(tenantId, deviceId, commandId, null);
            List<CommandAttributeConfigBO> entityBOList = filterTenant(tenantId,
                    commandAttributeConfigService.listByDeviceIdAndCommandId(deviceId, commandId));
            List<CommandAttributeConfigVO> entityVOList = commandAttributeConfigBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    /**
     * Device ID CommandConfig
     *
     * @param deviceId Device ID
     * @return CommandConfig
     */
    @PreAuthorize("@perm.can('command_attribute_config', 'list')")
    @GetMapping("/list_by_device_id")
    public Mono<R<List<CommandAttributeConfigVO>>> listByDeviceId(
            @NotNull @RequestParam(value = "device_id") Long deviceId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, deviceService.getById(deviceId));
            List<CommandAttributeConfigBO> entityBOList = filterTenant(tenantId,
                    commandAttributeConfigService.listByDeviceId(deviceId));
            List<CommandAttributeConfigVO> entityVOList = commandAttributeConfigBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    /**
     * CommandConfig
     *
     * @param entityQuery CommandConfig Dto
     * @return Page Of CommandConfig
     */
    @PreAuthorize("@perm.can('command_attribute_config', 'list')")
    @PostMapping("/list")
    public Mono<R<Page<CommandAttributeConfigVO>>> list(
            @RequestBody(required = false) CommandAttributeConfigQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandAttributeConfigQuery query = Objects.isNull(entityQuery) ? new CommandAttributeConfigQuery()
                    : entityQuery;
            query.setTenantId(tenantId);
            Page<CommandAttributeConfigBO> entityPageBO = commandAttributeConfigService.list(query);
            Page<CommandAttributeConfigVO> entityPageVO = commandAttributeConfigBuilder
                    .buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    private void requireCommandConfigRelations(Long tenantId, Long deviceId, Long commandId, Long attributeId) {
        DeviceBO deviceBO = requireTenant(tenantId, deviceService.getById(deviceId));
        CommandBO commandBO = requireTenant(tenantId, commandService.getById(commandId));
        if (Objects.isNull(deviceBO.getProfileId()) || !Objects.equals(deviceBO.getProfileId(), commandBO.getProfileId())) {
            throw new NotFoundException("Resource does not exist");
        }

        if (Objects.nonNull(attributeId)) {
            CommandAttributeBO attributeBO = requireTenant(tenantId, commandAttributeService.getById(attributeId));
            if (!Objects.equals(deviceBO.getDriverId(), attributeBO.getDriverId())) {
                throw new NotFoundException("Resource does not exist");
            }
        }
    }

}
