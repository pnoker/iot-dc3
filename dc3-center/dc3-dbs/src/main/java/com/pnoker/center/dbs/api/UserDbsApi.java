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

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.center.dbs.user.feign.UserDbsFeignClient;
import com.pnoker.center.dbs.service.UserDbsService;
import com.pnoker.common.bean.Response;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.auth.UserDto;
import com.pnoker.common.model.auth.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * <p>UserDbsApi
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@RestController
@RequestMapping(Common.Service.DC3_DBS_USER_URL_PREFIX)
public class UserDbsApi implements UserDbsFeignClient {
    @Resource
    private UserDbsService userDbsService;

    @Override
    public Response<User> add(User user) {
        try {
            user.setUsername(IdUtil.simpleUUID().substring(0,11));
            user = userDbsService.add(user);
            return null != user ? Response.ok(user) : Response.fail("user record add failed");
        } catch (Exception e) {
            return Response.fail(e.getMessage());
        }
    }

    @Override
    public Response<Boolean> delete(Long id) {
        try {
            return userDbsService.delete(id) ? Response.ok() : Response.fail("user record delete failed");
        } catch (Exception e) {
            return Response.fail(e.getMessage());
        }
    }

    @Override
    public Response<User> update(User user) {
        try {
            user = userDbsService.update(user);
            return null != user ? Response.ok(user) : Response.fail("user record update failed");
        } catch (Exception e) {
            return Response.fail(e.getMessage());
        }
    }

    @Override
    public Response<User> selectById(Long id) {
        try {
            User user = userDbsService.selectById(id);
            return null != user ? Response.ok(user) : Response.fail(String.format("user record does not exist for id(%s)", id));
        } catch (Exception e) {
            return Response.fail(e.getMessage());
        }
    }

    @Override
    public Response<User> selectByUsername(String username) {
        try {
            User user = userDbsService.selectByUsername(username);
            return null != user ? Response.ok(user) : Response.fail(String.format("user record does not exist for username(%s)", username));
        } catch (Exception e) {
            return Response.fail(e.getMessage());
        }
    }

    @Override
    public Response<Page<User>> list(UserDto userDto) {
        if (!Optional.ofNullable(userDto).isPresent()) {
            userDto = new UserDto();
        }
        try {
            return Response.ok(userDbsService.list(userDto));
        } catch (Exception e) {
            return Response.fail(e.getMessage());
        }
    }

}
