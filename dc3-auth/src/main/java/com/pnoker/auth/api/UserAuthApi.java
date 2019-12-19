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

package com.pnoker.auth.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.auth.user.feign.UserAuthFeignClient;
import com.pnoker.auth.service.UserAuthService;
import com.pnoker.common.bean.Response;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.auth.UserDto;
import com.pnoker.common.entity.auth.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>UserAuthApi
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@RestController
@RequestMapping(Common.Service.DC3_USER_URL_PREFIX)
public class UserAuthApi implements UserAuthFeignClient {
    @Resource
    private UserAuthService userAuthService;

    @Override
    public Response<User> add(User user) {
        return userAuthService.add(user);
    }

    @Override
    public Response<Boolean> delete(Long id) {
        return userAuthService.delete(id);
    }

    @Override
    public Response<User> update(User user) {
        return userAuthService.update(user);
    }

    @Override
    public Response<User> selectById(Long id) {
        return userAuthService.selectById(id);
    }

    @Override
    public Response<Page<User>> list(UserDto userDto) {
        return userAuthService.list(userDto);
    }

    @Override
    public Response<Boolean> checkUserValid(String username) {
        return userAuthService.checkUserValid(username);
    }

}
