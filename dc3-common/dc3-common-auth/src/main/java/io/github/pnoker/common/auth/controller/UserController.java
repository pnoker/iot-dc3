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
import io.github.pnoker.common.auth.dal.PrincipalManager;
import io.github.pnoker.common.auth.entity.model.PrincipalDO;
import io.github.pnoker.common.auth.entity.model.TenantMembershipDO;
import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.entity.builder.UserBuilder;
import io.github.pnoker.common.auth.entity.query.UserQuery;
import io.github.pnoker.common.auth.entity.vo.UserVO;
import io.github.pnoker.common.auth.service.TenantMembershipService;
import io.github.pnoker.common.auth.service.UserService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.MembershipStatusEnum;
import io.github.pnoker.common.enums.PrincipalSourceTypeEnum;
import io.github.pnoker.common.enums.PrincipalTypeEnum;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.NotFoundException;
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

import java.util.Objects;

/**
 * User Profile Controller (dc3_user)
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2016.10.1
 */
@Tag(name = "user", description = "Users")
@Slf4j
@RestController
@RequestMapping(AuthConstant.USER_PROFILE_URL_PREFIX)
@RequiredArgsConstructor
public class UserController implements BaseController {

    private final UserBuilder userBuilder;

    private final UserService userService;

    private final TenantMembershipService tenantMembershipService;

    private final PrincipalManager principalManager;

    @PreAuthorize("@perm.can('user', 'add')")
    @Operation(summary = "Add User", description = "Create a user record")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody UserVO entityVO) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            UserBO entityBO = userBuilder.buildBOByVO(entityVO);
            PrincipalDO principal = new PrincipalDO();
            principal.setPrincipalType(PrincipalTypeEnum.USER.getValue());
            principal.setPrincipalName(entityBO.getUserName());
            principal.setDisplayName(entityBO.getNickName());
            principal.setSourceType(PrincipalSourceTypeEnum.LOCAL.getValue());
            principal.setEnableFlag(EnableFlagEnum.ENABLE.getIndex());
            principal.setLockedFlag(EnableFlagEnum.ENABLE.getIndex());
            principal.setCreatorId(header.getUserId());
            principal.setCreatorName(header.getNickName());
            principal.setOperatorId(header.getUserId());
            principal.setOperatorName(header.getNickName());
            if (!principalManager.save(principal)) {
                throw new AddException("Failed to create principal");
            }
            entityBO.setPrincipalId(principal.getId());
            entityBO.setCreatorId(header.getUserId());
            entityBO.setCreatorName(header.getNickName());
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            userService.add(entityBO);
            UserBO saved = userService.getByUserName(entityBO.getUserName(), true);
            TenantMembershipDO membership = new TenantMembershipDO();
            membership.setTenantId(header.getTenantId());
            membership.setPrincipalId(saved.getPrincipalId());
            membership.setPrincipalType(PrincipalTypeEnum.USER.getValue());
            membership.setMembershipStatus(MembershipStatusEnum.ACTIVE.getValue());
            membership.setCreatorId(header.getUserId());
            membership.setCreatorName(header.getNickName());
            membership.setOperatorId(header.getUserId());
            membership.setOperatorName(header.getNickName());
            tenantMembershipService.add(membership);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('user', 'delete')")
    @Operation(summary = "Delete User", description = "Delete a user record by ID")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Record ID") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            UserBO user = userService.getById(id);
            tenantMembershipService.requireTenantMember(tenantId, user.getPrincipalId());
            TenantMembershipDO membership = tenantMembershipService.getByTenantIdAndPrincipalId(tenantId,
                    user.getPrincipalId());
            userService.delete(id);
            if (Objects.nonNull(membership)) {
                tenantMembershipService.delete(membership.getId());
            }
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('user', 'update')")
    @Operation(summary = "Update User", description = "Update a user record")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody UserVO entityVO) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            UserBO entityBO = userBuilder.buildBOByVO(entityVO);
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            UserBO current = userService.getById(entityBO.getId());
            tenantMembershipService.requireTenantMember(header.getTenantId(), current.getPrincipalId());
            entityBO.setPrincipalId(current.getPrincipalId());
            userService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('user', 'get')")
    @Operation(summary = "Get User by ID", description = "Get user details by ID")
    @GetMapping("/get_by_id")
    public Mono<R<UserVO>> getById(@Parameter(description = "Record ID") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            UserBO entityBO = userService.getById(id);
            tenantMembershipService.requireTenantMember(tenantId, entityBO.getPrincipalId());
            UserVO entityVO = userBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    @PreAuthorize("@perm.can('user', 'get')")
    @Operation(summary = "Get User by Name", description = "Get user details by name")
    @GetMapping("/get_by_name")
    public Mono<R<UserVO>> getByName(@Parameter(description = "Name") @NotNull @RequestParam(value = "name") String name) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            UserBO entityBO = userService.getByUserName(name, false);
            // Both "not found" and "wrong tenant" return the same 404 so the
            // response shape does not reveal whether a user name exists.
            if (Objects.isNull(entityBO)) {
                throw new NotFoundException("Resource does not exist");
            }
            tenantMembershipService.requireTenantMember(tenantId, entityBO.getPrincipalId());
            UserVO entityVO = userBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    @PreAuthorize("@perm.can('user', 'list')")
    @Operation(summary = "List Users", description = "List users with pagination")
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

}
