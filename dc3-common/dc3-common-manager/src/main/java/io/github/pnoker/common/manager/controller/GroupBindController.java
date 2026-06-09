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
import io.github.pnoker.common.dal.entity.bo.GroupBO;
import io.github.pnoker.common.dal.entity.bo.GroupBindBO;
import io.github.pnoker.common.dal.entity.builder.GroupBindBuilder;
import io.github.pnoker.common.dal.entity.query.GroupBindQuery;
import io.github.pnoker.common.dal.entity.vo.GroupBindVO;
import io.github.pnoker.common.dal.service.GroupBindService;
import io.github.pnoker.common.dal.service.GroupService;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.service.DeviceService;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.PointService;
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

import java.util.Objects;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing group binding management endpoints.
 *
 * @author pnoker
 * @version 2026.5.11
 * @since 2026.5.11
 */
@Tag(name = "group_bind", description = "分组绑定")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.GROUP_BIND_URL_PREFIX)
@RequiredArgsConstructor
public class GroupBindController implements BaseController {

    private final GroupBindBuilder groupBindBuilder;

    private final GroupBindService groupBindService;

    private final GroupService groupService;

    private final DriverService driverService;

    private final ProfileService profileService;

    private final PointService pointService;

    private final DeviceService deviceService;

    /**
     * @param entityVO {@link GroupBindVO}
     * @return R of String
     */
    @PreAuthorize("@perm.can('group_bind', 'add')")
    @Operation(summary = "新增GroupBind", description = "新增一条GroupBind记录")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody GroupBindVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            GroupBindBO entityBO = groupBindBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            validateBind(tenantId, entityBO);
            groupBindService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * @param id ID
     * @return R of String
     */
    @PreAuthorize("@perm.can('group_bind', 'delete')")
    @Operation(summary = "删除GroupBind", description = "删除指定ID的GroupBind")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, groupBindService.getById(id));
            groupBindService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    /**
     * @param entityVO {@link GroupBindVO}
     * @return R of String
     */
    @PreAuthorize("@perm.can('group_bind', 'update')")
    @Operation(summary = "更新GroupBind", description = "更新GroupBind信息")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody GroupBindVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            GroupBindBO entityBO = groupBindBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, groupBindService.getById(entityBO.getId()));
            validateBind(tenantId, entityBO);
            groupBindService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * @param id ID
     * @return GroupBindVO {@link GroupBindVO}
     */
    @PreAuthorize("@perm.can('group_bind', 'get')")
    @Operation(summary = "查询GroupBind", description = "根据ID查询GroupBind详细信息")
    @GetMapping("/get_by_id")
    public Mono<R<GroupBindVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            GroupBindBO entityBO = requireTenant(tenantId, groupBindService.getById(id));
            GroupBindVO entityVO = groupBindBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * @param entityQuery {@link GroupBindQuery}
     * @return R Of GroupBindVO Page
     */
    @PreAuthorize("@perm.can('group_bind', 'list')")
    @Operation(summary = "查询GroupBind列表", description = "分页查询GroupBind列表")
    @PostMapping("/list")
    public Mono<R<Page<GroupBindVO>>> list(@RequestBody(required = false) GroupBindQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            GroupBindQuery query = Objects.isNull(entityQuery) ? new GroupBindQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<GroupBindBO> entityPageBO = groupBindService.list(query);
            Page<GroupBindVO> entityPageVO = groupBindBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    private void validateBind(Long tenantId, GroupBindBO entityBO) {
        EntityTypeEnum entityTypeFlag = entityBO.getEntityTypeFlag();
        GroupBO groupBO = requireTenant(tenantId, groupService.getById(entityBO.getGroupId()));
        if (!Objects.equals(groupBO.getGroupTypeFlag(), entityTypeFlag)) {
            throw new NotFoundException("Resource does not exist");
        }
        requireEntityTenant(tenantId, entityTypeFlag, entityBO.getEntityId());
    }

    private void requireEntityTenant(Long tenantId, EntityTypeEnum entityTypeFlag, Long entityId) {
        switch (entityTypeFlag) {
            case DRIVER -> requireTenant(tenantId, driverService.getById(entityId));
            case PROFILE -> requireTenant(tenantId, profileService.getById(entityId));
            case POINT -> requireTenant(tenantId, pointService.getById(entityId));
            case DEVICE -> requireTenant(tenantId, deviceService.getById(entityId));
            default -> throw new NotFoundException("Resource does not exist");
        }
    }

}
