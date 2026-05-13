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
 * Controller
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(AuthConstant.USER_URL_PREFIX)
public class UserLoginController implements BaseController {

    private final UserLoginBuilder userLoginBuilder;

    private final UserLoginService userLoginService;

    private final UserPasswordService userPasswordService;

    private final TenantBindService tenantBindService;

    public UserLoginController(UserLoginBuilder userLoginBuilder, UserLoginService userLoginService,
                               UserPasswordService userPasswordService, TenantBindService tenantBindService) {
        this.userLoginBuilder = userLoginBuilder;
        this.userLoginService = userLoginService;
        this.userPasswordService = userPasswordService;
        this.tenantBindService = tenantBindService;
    }

    /**
     * @param entityVO {@link UserLoginVO}
     * @return R of String
     */
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody UserLoginVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            UserLoginBO entityBO = userLoginBuilder.buildBOByVO(entityVO);
            requireTenantMember(tenantId, entityBO.getUserId());
            userLoginService.save(entityBO);
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
            UserLoginBO entityBO = userLoginService.selectById(id);
            requireTenantMember(tenantId, entityBO.getUserId());
            userLoginService.remove(id);
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
            UserLoginBO current = userLoginService.selectById(entityBO.getId());
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
            if (userLoginService.selectByPage(query).getRecords().isEmpty()) {
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
    @GetMapping("/select_by_id")
    public Mono<R<UserLoginVO>> selectById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            UserLoginBO entityBO = userLoginService.selectById(id);
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
    @GetMapping("/select_by_name")
    public Mono<R<UserLoginVO>> selectByName(@NotNull @RequestParam(value = "name") String name) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            UserLoginBO entityBO = userLoginService.selectByLoginName(name, false);
            if (Objects.isNull(entityBO)) {
                return R.fail(ResponseEnum.NO_RESOURCE.getText());
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
            Page<UserLoginBO> entityPageBO = userLoginService.selectByPage(query);
            Page<UserLoginVO> entityPageVO = userLoginBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    /**
     * Name
     *
     * @param name Name
     * @return
     */
    @GetMapping("/check")
    public Mono<R<Boolean>> checkLoginNameValid(@NotNull @RequestParam(value = "name") String name) {
        return async(() -> Boolean.TRUE.equals(userLoginService.checkLoginNameValid(name)) ? R.ok() : R.fail());
    }

    private void requireTenantMember(Long tenantId, Long userId) {
        TenantBindBO tenantBind = tenantBindService.selectByTenantIdAndUserId(tenantId, userId);
        if (Objects.isNull(tenantBind)) {
            throw new NotFoundException("Resource does not exist");
        }
    }

}
