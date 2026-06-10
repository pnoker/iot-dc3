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
import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.common.manager.entity.builder.CommandBuilder;
import io.github.pnoker.common.manager.entity.query.CommandQuery;
import io.github.pnoker.common.manager.entity.vo.CommandVO;
import io.github.pnoker.common.manager.service.CommandService;
import io.github.pnoker.common.manager.service.DeviceService;
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
 * REST controller exposing command management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "command", description = "Commands")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.COMMAND_URL_PREFIX)
@RequiredArgsConstructor
public class CommandController implements BaseController {

    private final CommandBuilder commandBuilder;

    private final CommandService commandService;

    private final ProfileService profileService;

    private final DeviceService deviceService;

    @PreAuthorize("@perm.can('command', 'add')")
    @Operation(summary = "Add Command", description = "Create a command record")
    @PostMapping("/add")
    public Mono<R<Long>> add(@Validated(Add.class) @RequestBody CommandVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandBO entityBO = commandBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            commandService.add(entityBO);
            return R.ok(entityBO.getId());
        }));
    }

    @PreAuthorize("@perm.can('command', 'delete')")
    @Operation(summary = "Delete Command", description = "Delete a command record by ID")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Record ID") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, commandService.getById(id));
            commandService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('command', 'update')")
    @Operation(summary = "Update Command", description = "Update a command record")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody CommandVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandBO entityBO = commandBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, commandService.getById(entityBO.getId()));
            commandService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('command', 'get')")
    @Operation(summary = "Get Command by ID", description = "Get command details by ID")
    @GetMapping("/get_by_id")
    public Mono<R<CommandVO>> getById(@Parameter(description = "Record ID") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandBO entityBO = requireTenant(tenantId, commandService.getById(id));
            CommandVO entityVO = commandBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    @PreAuthorize("@perm.can('command', 'list')")
    @Operation(summary = "List Commands by Profile ID", description = "List commands by profile ID")
    @GetMapping("/list_by_profile_id")
    public Mono<R<List<CommandVO>>> listByProfileId(@Parameter(description = "Profile ID") @NotNull @RequestParam(value = "profile_id") Long profileId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, profileService.getById(profileId));
            List<CommandBO> entityBOList = filterTenant(tenantId, commandService.listByProfileId(profileId, tenantId));
            List<CommandVO> entityVOList = commandBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    @PreAuthorize("@perm.can('command', 'list')")
    @Operation(summary = "List Commands by Device ID", description = "List commands by device ID")
    @GetMapping("/list_by_device_id")
    public Mono<R<List<CommandVO>>> listByDeviceId(@Parameter(description = "Device ID") @NotNull @RequestParam(value = "device_id") Long deviceId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, deviceService.getById(deviceId));
            List<CommandBO> entityBOList = filterTenant(tenantId, commandService.listByDeviceId(deviceId, tenantId));
            List<CommandVO> entityVOList = commandBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    @PreAuthorize("@perm.can('command', 'list')")
    @Operation(summary = "List Commands", description = "List commands with pagination")
    @PostMapping("/list")
    public Mono<R<Page<CommandVO>>> list(@RequestBody(required = false) CommandQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandQuery query = Objects.isNull(entityQuery) ? new CommandQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<CommandBO> entityPageBO = commandService.list(query);
            Page<CommandVO> entityPageVO = commandBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}
