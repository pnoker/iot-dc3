/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.auth.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.auth.entity.bo.UserLoginBO;
import io.github.pnoker.center.auth.entity.builder.UserLoginBuilder;
import io.github.pnoker.center.auth.entity.query.UserLoginQuery;
import io.github.pnoker.center.auth.entity.vo.UserLoginVO;
import io.github.pnoker.center.auth.service.UserLoginService;
import io.github.pnoker.center.auth.service.UserPasswordService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

/**
 * 用户 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@Tag(name = "接口-用户登录")
@RequestMapping(AuthConstant.USER_URL_PREFIX)
public class UserLoginController implements BaseController {

    @Resource
    private UserLoginBuilder userLoginBuilder;

    @Resource
    private UserLoginService userLoginService;
    @Resource
    private UserPasswordService userPasswordService;

    /**
     * 新增用户
     *
     * @param entityVO {@link UserLoginVO}
     * @return R of String
     */
    @PostMapping("/add")
    @Operation(summary = "新增-用户登录")
    public R<String> add(@Validated(Add.class) @RequestBody UserLoginVO entityVO) {
        try {
            UserLoginBO entityBO = userLoginBuilder.buildBOByVO(entityVO);
            userLoginService.save(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 删除用户
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public R<String> delete(@NotNull @PathVariable(value = "id") Long id) {
        try {
            userLoginService.remove(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新用户
     * <ol>
     * <li>支持更新: Enable,Password</li>
     * <li>不支持更新: Name</li>
     * </ol>
     *
     * @param entityVO {@link UserLoginVO}
     * @return R of String
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody UserLoginVO entityVO) {
        try {
            UserLoginBO entityBO = userLoginBuilder.buildBOByVO(entityVO);
            userLoginService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 重置用户密码
     *
     * @param id 用户ID
     * @return 是否重置
     */
    @PostMapping("/reset/{id}")
    public R<Boolean> restPassword(@NotNull @PathVariable(value = "id") Long id) {
        try {
            userPasswordService.restPassword(id);
            return R.ok();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询用户
     *
     * @param id ID
     * @return UserLoginVO {@link UserLoginVO}
     */
    @GetMapping("/id/{id}")
    public R<UserLoginVO> selectById(@NotNull @PathVariable(value = "id") Long id) {
        try {
            UserLoginBO entityBO = userLoginService.selectById(id);
            UserLoginVO entityVO = userLoginBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 Name 查询 User
     *
     * @param name 用户名称
     * @return {@link UserLoginBO}
     */
    @GetMapping("/name/{name}")
    public R<UserLoginVO> selectByName(@NotNull @PathVariable(value = "name") String name) {
        try {
            UserLoginBO entityBO = userLoginService.selectByLoginName(name, false);
            UserLoginVO entityVO = userLoginBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 分页查询 User
     *
     * @param entityQuery 用户和分页参数
     * @return 带分页的 {@link UserLoginBO}
     */
    @PostMapping("/list")
    public R<Page<UserLoginVO>> list(@RequestBody(required = false) UserLoginQuery entityQuery) {
        try {
            if (ObjectUtil.isEmpty(entityQuery)) {
                entityQuery = new UserLoginQuery();
            }
            Page<UserLoginBO> entityPageBO = userLoginService.selectByPage(entityQuery);
            Page<UserLoginVO> entityPageVO = userLoginBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 检测登录名称是否有效
     *
     * @param name 用户名称
     * @return 是否有效
     */
    @GetMapping("/check/{name}")
    public R<Boolean> checkLoginNameValid(@NotNull @PathVariable(value = "name") String name) {
        try {
            return Boolean.TRUE.equals(userLoginService.checkLoginNameValid(name)) ? R.ok() : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

}
