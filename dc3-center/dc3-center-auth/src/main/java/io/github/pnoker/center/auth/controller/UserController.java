/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.auth.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.auth.entity.query.UserPageQuery;
import io.github.pnoker.center.auth.service.UserPasswordService;
import io.github.pnoker.center.auth.service.UserService;
import io.github.pnoker.common.constant.service.AuthServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.model.User;
import io.github.pnoker.common.valid.Insert;
import io.github.pnoker.common.valid.Update;
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
@RequestMapping(AuthServiceConstant.USER_URL_PREFIX)
public class UserController {

    @Resource
    private UserService userService;
    @Resource
    private UserPasswordService userPasswordService;

    /**
     * 新增用户
     *
     * @param user 用户
     * @return {@link User}
     */
    @PostMapping("/add")
    public R<User> add(@Validated(Insert.class) @RequestBody User user) {
        try {
            User add = userService.add(user);
            if (ObjectUtil.isNotNull(add)) {
                return R.ok(add);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 根据 ID 删除用户
     *
     * @param id 用户ID
     * @return 是否删除
     */
    @PostMapping("/delete/{id}")
    public R<Boolean> delete(@NotNull @PathVariable(value = "id") String id) {
        try {
            return userService.delete(id) ? R.ok() : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 修改用户
     * <ol>
     * <li>支持修改: Enable,Password</li>
     * <li>不支持修改: Name</li>
     * </ol>
     *
     * @param user 用户
     * @return {@link User}
     */
    @PostMapping("/update")
    public R<User> update(@Validated(Update.class) @RequestBody User user) {
        try {
            user.setLoginName(null);
            User update = userService.update(user);
            if (ObjectUtil.isNotNull(update)) {
                return R.ok(update);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 根据 ID 重置用户密码
     *
     * @param id 用户ID
     * @return 是否重置
     */
    @PostMapping("/reset/{id}")
    public R<Boolean> restPassword(@NotNull @PathVariable(value = "id") String id) {
        try {
            return userPasswordService.restPassword(id) ? R.ok() : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询用户
     *
     * @param id 用户ID
     * @return {@link User}
     */
    @GetMapping("/id/{id}")
    public R<User> selectById(@NotNull @PathVariable(value = "id") String id) {
        try {
            User select = userService.selectById(id);
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail(ResponseEnum.NO_RESOURCE.getMessage());
    }

    /**
     * 根据 Name 查询 User
     *
     * @param name 用户名称
     * @return {@link User}
     */
    @GetMapping("/name/{name}")
    public R<User> selectByName(@NotNull @PathVariable(value = "name") String name) {
        try {
            User select = userService.selectByLoginName(name, false);
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail(ResponseEnum.NO_RESOURCE.getMessage());
    }

    /**
     * 模糊分页查询 User
     *
     * @param userPageQuery 用户和分页参数
     * @return 带分页的 {@link User}
     */
    @PostMapping("/list")
    public R<Page<User>> list(@RequestBody(required = false) UserPageQuery userPageQuery) {
        try {
            if (ObjectUtil.isEmpty(userPageQuery)) {
                userPageQuery = new UserPageQuery();
            }
            Page<User> page = userService.list(userPageQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail(ResponseEnum.NO_RESOURCE.getMessage());
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
            return userService.checkLoginNameValid(name) ? R.ok() : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

}
