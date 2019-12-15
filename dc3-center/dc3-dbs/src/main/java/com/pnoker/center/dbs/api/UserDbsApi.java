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
import com.pnoker.api.center.dbs.user.feign.UserDbsFeignClient;
import com.pnoker.center.dbs.service.UserService;
import com.pnoker.common.bean.Response;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.auth.UserDto;
import com.pnoker.common.entity.auth.Token;
import com.pnoker.common.entity.auth.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @Autowired
    private UserService userService;

    @Override
    public Response<Long> add(User user) {
        return null != userService.add(user) ? Response.ok(user.getId()) : Response.fail();
    }

    @Override
    public Response<Boolean> delete(Long id) {
        return userService.delete(id) ? Response.ok() : Response.fail();
    }

    @Override
    public Response<Boolean> update(User user) {
        if (null == user.getId()) {
            return Response.fail("id is null");
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
        if (!Optional.ofNullable(userDto).isPresent()) {
            userDto = new UserDto();
        }
        return Response.ok(userService.list(userDto));
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
    public Response<Boolean> updateToken(Token token) {
        if (null == token.getId()) {
            return Response.fail("id is null");
        }
        return null != userService.updateToken(token) ? Response.ok() : Response.fail();
    }

    @Override
    public Response<Token> selectTokenById(Long id) {
        Token token = userService.selectTokenById(id);
        return null != token ? Response.ok(token) : Response.fail("id does not exist");
    }

    @Override
    public Response<Token> selectTokenByAppId(String appId) {
        Token token = userService.selectTokenByAppId(appId);
        return null != token ? Response.ok(token) : Response.fail("id does not exist");
    }

}
