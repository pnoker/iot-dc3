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

package com.pnoker.auth.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.center.dbs.user.feign.UserDbsFeignClient;
import com.pnoker.auth.service.TokenAuthService;
import com.pnoker.auth.service.UserAuthService;
import com.pnoker.common.bean.Response;
import com.pnoker.common.dto.auth.UserDto;
import com.pnoker.common.entity.auth.Token;
import com.pnoker.common.entity.auth.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * <p>UserAuthServiceImpl
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@Service
public class UserAuthServiceImpl implements UserAuthService {
    @Resource
    private UserDbsFeignClient userDbsFeignClient;

    @Resource
    private TokenAuthService tokenAuthService;

    @Override
    @Caching(
            put = {
                    @CachePut(value = "auth_user", key = "#user.id", unless = "#result==null"),
                    @CachePut(value = "auth_user_username", key = "#user.username", unless = "#result==null")
            },
            evict = {@CacheEvict(value = "auth_user_list", allEntries = true)}
    )
    public Response<User> add(User user) {
        Response<Boolean> exist = checkUserValid(user.getUsername());
        if (!exist.isOk()) {
            Token token = new Token(6);
            Response<Token> tokenResponse = tokenAuthService.add(token);
            if (tokenResponse.isOk()) {
                token = tokenResponse.getData();
                Response<User> userResponse = userDbsFeignClient.add(user.setTokenId(token.getId()));
                return userResponse;
            }
            return Response.fail(tokenResponse.getMessage());
        }
        return Response.fail("user already exists");
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = "auth_user", key = "#id"),
                    @CacheEvict(value = "auth_user_username", allEntries = true),
                    @CacheEvict(value = "token", key = "#id"),
                    @CacheEvict(value = "auth_user_list", allEntries = true)
            }
    )
    public Response<Boolean> delete(Long id) {
        Response<User> userResponse = userDbsFeignClient.selectById(id);
        if (userResponse.isOk()) {
            User user = userResponse.getData();
            Response<Boolean> userDelete = userDbsFeignClient.delete(user.getId());
            if (userDelete.isOk()) {
                Response<Boolean> tokenDelete = tokenAuthService.delete(user.getTokenId());
                return tokenDelete;
            }
            return Response.fail(userDelete.getMessage());
        }
        return Response.fail(userResponse.getMessage());
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = "auth_user", key = "#user.id", unless = "#result==null"),
                    @CachePut(value = "auth_user_username", key = "#user.username", unless = "#result==null")
            },
            evict = {
                    @CacheEvict(value = "token", key = "#id"),
                    @CacheEvict(value = "auth_user_list", allEntries = true)
            }
    )
    public Response<User> update(User user) {
        Response<User> userResponse = userDbsFeignClient.selectById(user.getId());
        if (userResponse.isOk()) {
            Response<User> response = userDbsFeignClient.update(user);
            return response;
        }
        return Response.fail(userResponse.getMessage());
    }

    @Override
    @Cacheable(value = "auth_user", key = "#id", unless = "#result==null")
    public Response<User> selectById(Long id) {
        Response<User> response = userDbsFeignClient.selectById(id);
        return response;
    }

    @Override
    @Cacheable(value = "auth_user_username", key = "#username", unless = "#result==null")
    public Response<User> selectByUsername(String username) {
        Response<User> response = userDbsFeignClient.selectByUsername(username);
        return response;
    }

    @Override
    @Cacheable(value = "auth_user_list", keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Response<Page<User>> list(UserDto userDto) {
        Response<Page<User>> response = userDbsFeignClient.list(userDto);
        return response;
    }

    @Override
    public Response<Boolean> checkUserValid(String username) {
        Response<User> response = selectByUsername(username);
        return response.isOk() ? Response.ok() : Response.fail(response.getMessage());
    }

}
