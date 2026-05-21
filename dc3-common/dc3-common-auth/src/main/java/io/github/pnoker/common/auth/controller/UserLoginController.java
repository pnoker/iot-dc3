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
import io.github.pnoker.common.auth.entity.bo.UserLoginBO;
import io.github.pnoker.common.auth.entity.builder.UserLoginBuilder;
import io.github.pnoker.common.auth.entity.query.UserLoginQuery;
import io.github.pnoker.common.auth.entity.vo.UserLoginVO;
import io.github.pnoker.common.auth.service.TenantBindService;
import io.github.pnoker.common.auth.service.UserLoginService;
import io.github.pnoker.common.auth.service.UserPasswordService;
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
 * REST controller exposing user login record endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@RestController
@RequestMapping(AuthConstant.USER_URL_PREFIX)
@RequiredArgsConstructor
public class UserLoginController implements BaseController {

    private final UserLoginBuilder userLoginBuilder;

    private final UserLoginService userLoginService;

    private final UserPasswordService userPasswordService;

    private final TenantBindService tenantBindService;

    /**
     * @param entityVO {@link UserLoginVO}
     * @return R of String
     */
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody UserLoginVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            UserLoginBO entityBO = userLoginBuilder.buildBOByVO(entityVO);
            requireTenantMember(tenantId, entityBO.getUserId());
            userLoginService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * ID
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            UserLoginBO entityBO = userLoginService.getById(id);
            requireTenantMember(tenantId, entityBO.getUserId());
            userLoginService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    /**
     *
     * <ol>
     * <li>: Enable,Password</li>
     * <li>: Name</li>
     * </ol>
     *
     * @param entityVO {@link UserLoginVO}
     * @return R of String
     */
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody UserLoginVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            UserLoginBO entityBO = userLoginBuilder.buildBOByVO(entityVO);
            UserLoginBO current = userLoginService.getById(entityBO.getId());
            requireTenantMember(tenantId, current.getUserId());
            requireTenantMember(tenantId, entityBO.getUserId());
            userLoginService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * ID
     *
     * @param id ID
     * @return
     */
    @PostMapping("/reset")
    public Mono<R<Boolean>> restPassword(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            UserLoginQuery query = new UserLoginQuery();
            query.setUserPasswordId(id);
            query.setTenantId(tenantId);
            if (userLoginService.list(query).getRecords().isEmpty()) {
                throw new NotFoundException("Resource does not exist");
            }
            userPasswordService.restPassword(id);
            return R.ok();
        }));
    }

    /**
     * ID
     *
     * @param id ID
     * @return UserLoginVO {@link UserLoginVO}
     */
    @GetMapping("/get_by_id")
    public Mono<R<UserLoginVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            UserLoginBO entityBO = userLoginService.getById(id);
            requireTenantMember(tenantId, entityBO.getUserId());
            UserLoginVO entityVO = userLoginBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Name User
     *
     * @param name Name
     * @return {@link UserLoginBO}
     */
    @GetMapping("/get_by_name")
    public Mono<R<UserLoginVO>> getByName(@NotNull @RequestParam(value = "name") String name) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            UserLoginBO entityBO = userLoginService.getByLoginName(name, false);
            // Both "not found" and "wrong tenant" return the same 404 so the
            // response shape does not reveal whether a login name exists.
            if (Objects.isNull(entityBO)) {
                throw new NotFoundException("Resource does not exist");
            }
            requireTenantMember(tenantId, entityBO.getUserId());
            UserLoginVO entityVO = userLoginBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * User
     *
     * @param entityQuery
     * @return {@link UserLoginBO}
     */
    @PostMapping("/list")
    public Mono<R<Page<UserLoginVO>>> list(@RequestBody(required = false) UserLoginQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            UserLoginQuery query = Objects.isNull(entityQuery) ? new UserLoginQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<UserLoginBO> entityPageBO = userLoginService.list(query);
            Page<UserLoginVO> entityPageVO = userLoginBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    /**
     * Check whether a login name is available (not yet taken) within the
     * caller's tenant. Requires authentication so the name cannot be used to
     * probe for accounts in other tenants.
     *
     * @param name login name to check
     * @return {@code true} when the name is free to use
     */
    @GetMapping("/check")
    public Mono<R<Boolean>> checkLoginNameValid(@NotNull @RequestParam(value = "name") String name) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            boolean available = userLoginService.checkLoginNameAvailable(name, tenantId);
            return R.ok(available);
        }));
    }

    private void requireTenantMember(Long tenantId, Long userId) {
        TenantBindBO tenantBind = tenantBindService.getByTenantIdAndUserId(tenantId, userId);
        if (Objects.isNull(tenantBind)) {
            throw new NotFoundException("Resource does not exist");
        }
    }

}
