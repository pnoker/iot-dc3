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

package io.github.pnoker.common.auth.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.entity.bo.TenantBindBO;
import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.entity.builder.UserBuilder;
import io.github.pnoker.common.auth.entity.query.UserQuery;
import io.github.pnoker.common.auth.entity.vo.UserVO;
import io.github.pnoker.common.auth.service.TenantBindService;
import io.github.pnoker.common.auth.service.UserService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.exception.NotFoundException;
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
 * User Profile Controller (dc3_user)
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2016.10.1
 */
@Tag(name = "user", description = "用户")
@Slf4j
@RestController
@RequestMapping(AuthConstant.USER_PROFILE_URL_PREFIX)
@RequiredArgsConstructor
public class UserController implements BaseController {

    private final UserBuilder userBuilder;

    private final UserService userService;

    private final TenantBindService tenantBindService;

    @PreAuthorize("@perm.can('user', 'add')")
    @Operation(summary = "新增用户管理", description = "新增一条用户记录")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody UserVO entityVO) {
        return getUserHeader().flatMap(header -> async(() -> {
            UserBO entityBO = userBuilder.buildBOByVO(entityVO);
            entityBO.setCreatorId(header.getUserId());
            entityBO.setCreatorName(header.getNickName());
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            userService.add(entityBO);
            UserBO saved = userService.getByUserName(entityBO.getUserName(), true);
            TenantBindBO tenantBindBO = new TenantBindBO();
            tenantBindBO.setTenantId(header.getTenantId());
            tenantBindBO.setUserId(saved.getId());
            tenantBindBO.setCreatorId(header.getUserId());
            tenantBindBO.setCreatorName(header.getNickName());
            tenantBindBO.setOperatorId(header.getUserId());
            tenantBindBO.setOperatorName(header.getNickName());
            tenantBindService.add(tenantBindBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('user', 'delete')")
    @Operation(summary = "删除用户管理", description = "删除指定ID的用户")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenantMember(tenantId, id);
            TenantBindBO tenantBind = tenantBindService.getByTenantIdAndUserId(tenantId, id);
            userService.delete(id);
            if (Objects.nonNull(tenantBind)) {
                tenantBindService.delete(tenantBind.getId());
            }
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('user', 'update')")
    @Operation(summary = "更新用户管理", description = "更新用户信息")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody UserVO entityVO) {
        return getUserHeader().flatMap(header -> async(() -> {
            UserBO entityBO = userBuilder.buildBOByVO(entityVO);
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            requireTenantMember(header.getTenantId(), entityBO.getId());
            userService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('user', 'get')")
    @Operation(summary = "查询用户管理", description = "根据ID查询用户管理详细信息")
    @GetMapping("/get_by_id")
    public Mono<R<UserVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenantMember(tenantId, id);
            UserBO entityBO = userService.getById(id);
            UserVO entityVO = userBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    @PreAuthorize("@perm.can('user', 'get')")
    @Operation(summary = "查询用户管理", description = "根据条件查询用户管理")
    @GetMapping("/get_by_name")
    public Mono<R<UserVO>> getByName(@NotNull @RequestParam(value = "name") String name) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            UserBO entityBO = userService.getByUserName(name, false);
            // Both "not found" and "wrong tenant" return the same 404 so the
            // response shape does not reveal whether a user name exists.
            if (Objects.isNull(entityBO)) {
                throw new NotFoundException("Resource does not exist");
            }
            requireTenantMember(tenantId, entityBO.getId());
            UserVO entityVO = userBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    @PreAuthorize("@perm.can('user', 'list')")
    @Operation(summary = "查询用户列表", description = "分页查询用户管理列表")
    @PostMapping("/list")
    public Mono<R<Page<UserVO>>> list(@RequestBody(required = false) UserQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            UserQuery query = Objects.isNull(entityQuery) ? new UserQuery() : entityQuery;
            // Overwrite whatever the client sent. Tenant scope is a hard
            // boundary, not a filter — a caller cannot reach across tenants.
            query.setTenantId(tenantId);
            Page<UserBO> entityPageBO = userService.list(query);
            Page<UserVO> entityPageVO = userBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    private void requireTenantMember(Long tenantId, Long userId) {
        TenantBindBO tenantBind = tenantBindService.getByTenantIdAndUserId(tenantId, userId);
        if (Objects.isNull(tenantBind)) {
            throw new NotFoundException("Resource does not exist");
        }
    }

}
