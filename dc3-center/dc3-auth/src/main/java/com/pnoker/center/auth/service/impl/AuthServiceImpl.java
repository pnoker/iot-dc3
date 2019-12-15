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

package com.pnoker.center.auth.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.center.dbs.user.feign.UserDbsFeignClient;
import com.pnoker.center.auth.service.AuthService;
import com.pnoker.common.bean.Response;
import com.pnoker.common.dto.auth.TokenDto;
import com.pnoker.common.dto.auth.UserDto;
import com.pnoker.common.entity.auth.Token;
import com.pnoker.common.entity.auth.User;
import com.pnoker.common.tool.AesTools;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * <p>Auth 接口实现
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {
    @Autowired
    private UserDbsFeignClient userDbsFeignClient;

    @Override
    @Caching(
            put = {@CachePut(value = "auth_user", key = "#user.id", unless = "#result==null")},
            evict = {@CacheEvict(value = "auth_user_list", allEntries = true)}
    )
    public User add(User user) {
        Response<Long> response = userDbsFeignClient.add(user);
        if (response.isOk()) {
            user.setId(response.getData());
            return user;
        }
        // 防止cache提示key为空
        user.setId(0L);
        return null;
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = "auth_user", key = "#id"),
                    @CacheEvict(value = "auth_check_user", allEntries = true),
                    @CacheEvict(value = "auth_token", allEntries = true),
                    @CacheEvict(value = "auth_user_list", allEntries = true)
            }
    )
    public boolean delete(Long id) {
        return userDbsFeignClient.delete(id).isOk();
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = "auth_user", key = "#user.id"),
                    @CacheEvict(value = "auth_check_user", allEntries = true),
                    @CacheEvict(value = "auth_token", allEntries = true),
                    @CacheEvict(value = "auth_user_list", allEntries = true)
            }
    )
    public boolean update(User user) {
        return userDbsFeignClient.update(user).isOk();
    }

    @Override
    @Cacheable(value = "auth_user", key = "#id", unless = "#result==null")
    public User selectById(Long id) {
        Response<User> response = userDbsFeignClient.selectById(id);
        return response.isOk() ? response.getData() : null;
    }

    @Override
    @Cacheable(value = "auth_user_list", keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<User> list(UserDto userDto) {
        Response<Page<User>> response = userDbsFeignClient.list(userDto);
        return response.isOk() ? response.getData() : null;
    }

    @Override
    @CachePut(value = "auth_check_user", key = "#username")
    public boolean checkUserExist(String username) {
        return userDbsFeignClient.username(username).isOk();
    }

    @Override
    @SneakyThrows
    @CacheEvict(value = "auth_token", allEntries = true)
    public TokenDto generateToken(User user) {
        Response<User> userResponse = userDbsFeignClient.username(user.getUsername());
        if (userResponse.isOk()) {
            user = userResponse.getData();
            Response<Token> tokenResponse = userDbsFeignClient.selectTokenById(user.getTokenId());
            if (tokenResponse.isOk()) {
                // 生成Token
                Token token = tokenResponse.getData();
                token.expireTime(0);
                token.setToken(AesTools.encrypt(IdUtil.simpleUUID(), token.getPrivateKey()));
                userDbsFeignClient.updateToken(token);

                TokenDto tokenDto = new TokenDto();
                tokenDto.convertToDto(tokenResponse.getData());
                return tokenDto;
            }
        }
        return null;
    }

    @Override
    @SneakyThrows
    @CachePut(value = "auth_token", key = "#tokenDto.appId")
    public boolean checkTokenValid(TokenDto tokenDto) {
        Response<Token> tokenResponse = userDbsFeignClient.selectTokenByAppId(tokenDto.getAppId());
        if (tokenResponse.isOk()) {
            Token temp = tokenResponse.getData();
            if (tokenDto.getToken().equals(temp.getToken()) && temp.getExpireTime().getTime() > (new Date()).getTime()) {
                return true;
            }
        }
        return false;
    }
}
