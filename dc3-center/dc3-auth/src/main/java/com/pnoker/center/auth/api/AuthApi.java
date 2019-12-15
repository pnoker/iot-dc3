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

package com.pnoker.center.auth.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.center.auth.feign.AuthFeignClient;
import com.pnoker.center.auth.service.AuthService;
import com.pnoker.common.bean.Response;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.auth.TokenDto;
import com.pnoker.common.dto.auth.UserDto;
import com.pnoker.common.entity.auth.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * <p>auth rest api
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@RestController
@RequestMapping(Common.Service.DC3_AUTH_URL_PREFIX)
public class AuthApi implements AuthFeignClient {
    @Autowired
    private AuthService authService;

    @Override
    public Response<Long> add(User user) {
        boolean exist = authService.checkUserExist(user.getUsername());
        if (exist) {
            return Response.fail("user already exists");
        }
        return null != authService.add(user) ? Response.ok(user.getId()) : Response.fail();
    }

    @Override
    public Response<Boolean> delete(Long id) {
        return authService.delete(id) ? Response.ok() : Response.fail("id does not exist");
    }

    @Override
    public Response<Boolean> update(User user) {
        if (null == user.getId()) {
            return Response.fail("id is null");
        }
        return authService.update(user.setUsername(null)) ? Response.ok() : Response.fail("id does not exist");
    }

    @Override
    public Response<User> selectById(Long id) {
        User user = authService.selectById(id);
        return null != user ? Response.ok(user) : Response.fail("id does not exist");
    }

    @Override
    public Response<Page<User>> list(UserDto userDto) {
        if (!Optional.ofNullable(userDto).isPresent()) {
            userDto = new UserDto();
        }
        return Response.ok(authService.list(userDto));
    }

    @Override
    public Response<Boolean> checkUserExist(String username) {
        return authService.checkUserExist(username) ? Response.ok() : Response.fail();
    }

    @Override
    public Response<TokenDto> generateToken(User user) {
        TokenDto tokenDto = authService.generateToken(user);
        return null != tokenDto ? Response.ok(tokenDto) : Response.fail();
    }

    @Override
    public Response<Boolean> checkTokenValid(TokenDto tokenDto) {
        return authService.checkTokenValid(tokenDto) ? Response.ok() : Response.fail();
    }
}
