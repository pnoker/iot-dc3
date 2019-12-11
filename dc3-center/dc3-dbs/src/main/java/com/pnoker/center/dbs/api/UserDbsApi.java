/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pnoker.center.dbs.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.center.dbs.service.UserService;
import com.pnoker.common.base.bean.Response;
import com.pnoker.common.base.constant.Common;
import com.pnoker.common.base.bean.PageInfo;
import com.pnoker.common.base.dto.auth.UserDto;
import com.pnoker.common.base.entity.auth.User;
import com.pnoker.dbs.api.user.feign.UserDbsFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * <p>user dbs rest api
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@RestController
@RequestMapping(Common.Service.DC3_DBS_USER_URL_PREFIX)
public class UserDbsApi implements UserDbsFeignClient {
    @Resource
    private UserService userService;

    @Override
    public Response<Long> add(User user) {
        if (!Optional.ofNullable(user).isPresent()) {
            return Response.fail("body is null");
        }
        return null != userService.add(user) ? Response.ok(user.getId()) : Response.fail();
    }

    @Override
    public Response<Boolean> delete(Long id) {
        return userService.delete(id) ? Response.ok() : Response.fail();
    }

    @Override
    public Response<Boolean> update(User user) {
        if (!Optional.ofNullable(user).isPresent()) {
            return Response.fail("body is null");
        }
        return null != userService.update(user) ? Response.ok() : Response.fail();
    }

    @Override
    public Response<User> selectById(Long id) {
        User user = userService.selectById(id);
        return null != user ? Response.ok(user) : Response.fail("id does not exist");
    }

    @Override
    public Response<Page<User>> list(UserDto userDto) {
        User user = new User();
        PageInfo page = new PageInfo();
        Optional.ofNullable(userDto).ifPresent(r -> {
            BeanUtils.copyProperties(r, user);
            Optional.ofNullable(userDto.getPage()).ifPresent(p -> BeanUtils.copyProperties(p, page));
        });
        return Response.ok(userService.list(user, page));
    }

    @Override
    public Response<User> username(String username) {
        if (StringUtils.isBlank(username)) {
            return Response.fail("username can not be empty");
        }
        User user = userService.selectByUsername(username);
        return null != user ? Response.ok(user) : Response.fail("username does not exist");
    }

    @Override
    public Response<User> phone(String phone) {
        if (StringUtils.isBlank(phone)) {
            return Response.fail("phone can not be empty");
        }
        User user = userService.selectByPhone(phone);
        return null != user ? Response.ok(user) : Response.fail("phone does not exist");
    }

    @Override
    public Response<User> email(String email) {
        if (StringUtils.isBlank(email)) {
            return Response.fail("email can not be empty");
        }
        User user = userService.selectByEmail(email);
        return null != user ? Response.ok(user) : Response.fail("email does not exist");
    }

}
