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
import io.github.pnoker.common.auth.entity.bo.UserLoginBO;
import io.github.pnoker.common.auth.entity.builder.UserLoginBuilder;
import io.github.pnoker.common.auth.entity.query.UserLoginQuery;
import io.github.pnoker.common.auth.entity.vo.UserLoginVO;
import io.github.pnoker.common.auth.service.UserLoginService;
import io.github.pnoker.common.auth.service.UserPasswordService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
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

    public UserLoginController(UserLoginBuilder userLoginBuilder, UserLoginService userLoginService,
                               UserPasswordService userPasswordService) {
        this.userLoginBuilder = userLoginBuilder;
        this.userLoginService = userLoginService;
        this.userPasswordService = userPasswordService;
    }

    /**
     * @param entityVO {@link UserLoginVO}
     * @return R of String
     */
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody UserLoginVO entityVO) {
        return async(() -> {
            UserLoginBO entityBO = userLoginBuilder.buildBOByVO(entityVO);
            userLoginService.save(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        });
    }

    /**
     * ID
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public Mono<R<String>> delete(@NotNull @PathVariable(value = "id") Long id) {
        return async(() -> {
            userLoginService.remove(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        });
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
        return async(() -> {
            UserLoginBO entityBO = userLoginBuilder.buildBOByVO(entityVO);
            userLoginService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        });
    }

    /**
     * ID
     *
     * @param id ID
     * @return
     */
    @PostMapping("/reset/{id}")
    public Mono<R<Boolean>> restPassword(@NotNull @PathVariable(value = "id") Long id) {
        return async(() -> {
            userPasswordService.restPassword(id);
            return R.ok();
        });
    }

    /**
     * ID
     *
     * @param id ID
     * @return UserLoginVO {@link UserLoginVO}
     */
    @GetMapping("/id/{id}")
    public Mono<R<UserLoginVO>> selectById(@NotNull @PathVariable(value = "id") Long id) {
        return async(() -> {
            UserLoginBO entityBO = userLoginService.selectById(id);
            UserLoginVO entityVO = userLoginBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        });
    }

    /**
     * Name User
     *
     * @param name Name
     * @return {@link UserLoginBO}
     */
    @GetMapping("/name/{name}")
    public Mono<R<UserLoginVO>> selectByName(@NotNull @PathVariable(value = "name") String name) {
        return async(() -> {
            UserLoginBO entityBO = userLoginService.selectByLoginName(name, false);
            UserLoginVO entityVO = userLoginBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        });
    }

    /**
     * User
     *
     * @param entityQuery
     * @return {@link UserLoginBO}
     */
    @PostMapping("/list")
    public Mono<R<Page<UserLoginVO>>> list(@RequestBody(required = false) UserLoginQuery entityQuery) {
        return async(() -> {
            UserLoginQuery query = Objects.isNull(entityQuery) ? new UserLoginQuery() : entityQuery;
            Page<UserLoginBO> entityPageBO = userLoginService.selectByPage(query);
            Page<UserLoginVO> entityPageVO = userLoginBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        });
    }

    /**
     * Name
     *
     * @param name Name
     * @return
     */
    @GetMapping("/check/{name}")
    public Mono<R<Boolean>> checkLoginNameValid(@NotNull @PathVariable(value = "name") String name) {
        return async(() -> Boolean.TRUE.equals(userLoginService.checkLoginNameValid(name)) ? R.ok() : R.fail());
    }

}
