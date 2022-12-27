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

package io.github.pnoker.center.auth.api;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.auth.dto.UserDto;
import io.github.pnoker.api.center.auth.feign.UserClient;
import io.github.pnoker.center.auth.service.UserPasswordService;
import io.github.pnoker.center.auth.service.UserService;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.service.AuthServiceConstant;
import io.github.pnoker.common.entity.User;
import io.github.pnoker.common.enums.ResponseEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 用户 Feign Client 接口实现
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(AuthServiceConstant.USER_URL_PREFIX)
public class UserApi implements UserClient {

    @Resource
    private UserService userService;
    @Resource
    private UserPasswordService userPasswordService;

    @Override
    public R<User> add(User user) {
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

    @Override
    public R<Boolean> delete(String id) {
        try {
            return userService.delete(id) ? R.ok() : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<User> update(User user) {
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

    @Override
    public R<Boolean> restPassword(String id) {
        try {
            return userPasswordService.restPassword(id) ? R.ok() : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<User> selectById(String id) {
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

    @Override
    public R<User> selectByName(String name) {
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

    @Override
    public R<Page<User>> list(UserDto userDto) {
        try {
            if (ObjectUtil.isEmpty(userDto)) {
                userDto = new UserDto();
            }
            Page<User> page = userService.list(userDto);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail(ResponseEnum.NO_RESOURCE.getMessage());
    }

    @Override
    public R<Boolean> checkLoginNameValid(String name) {
        try {
            return userService.checkLoginNameValid(name) ? R.ok() : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

}
