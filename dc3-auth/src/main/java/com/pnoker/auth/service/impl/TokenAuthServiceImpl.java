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
import com.pnoker.api.center.dbs.token.feign.TokenDbsFeignClient;
import com.pnoker.auth.service.TokenAuthService;
import com.pnoker.auth.service.UserAuthService;
import com.pnoker.common.bean.Response;
import com.pnoker.common.dto.auth.TokenDto;
import com.pnoker.common.entity.auth.Token;
import com.pnoker.common.entity.auth.User;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * <p>TokenAuthServiceImpl
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Service
public class TokenAuthServiceImpl implements TokenAuthService {
    @Resource
    private TokenDbsFeignClient tokenDbsFeignClient;

    @Resource
    private UserAuthService userAuthService;

    @Override
    @Caching(
            put = {
                    @CachePut(value = "token", key = "#token.userId", condition = "#result.ok==true"),
                    @CachePut(value = "auth_token", key = "#token.id", condition = "#result.ok==true"),
                    @CachePut(value = "auth_token_user_id", key = "#token.userId", condition = "#result.ok==true")
            },
            evict = {@CacheEvict(value = "auth_token_list", allEntries = true)}
    )
    public Response<Token> add(Token token) {
        Response<Token> response = tokenDbsFeignClient.add(token);
        if (response.isOk()) {
            token.setId(response.getData().getId());
            return response;
        }
        return Response.fail(response.getMessage());
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = "auth_token", key = "#id"),
                    @CacheEvict(value = "auth_token_user_id", allEntries = true),
                    @CacheEvict(value = "auth_token_list", allEntries = true)
            }
    )
    public Response<Boolean> delete(Long id) {
        return tokenDbsFeignClient.delete(id);
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = "token", key = "#token.userId", condition = "#result.ok==true"),
                    @CachePut(value = "auth_token", key = "#token.id", condition = "#result.ok==true"),
                    @CachePut(value = "auth_token_user_id", key = "#token.userId", condition = "#result.ok==true")
            },
            evict = {@CacheEvict(value = "auth_token_list", allEntries = true)}
    )
    public Response<Token> update(Token token) {
        Response<Token> tokenResponse = tokenDbsFeignClient.selectByUserId(token.getUserId());
        if (tokenResponse.isOk()) {
            Token result = tokenResponse.getData();
            token.setId(result.getId());
            Response<Token> response = tokenDbsFeignClient.update(result.generate(token.isNewKey(), token.getDuration()));
            if (response.isOk()) {
                return Response.ok(response.getData());
            }
            return response;
        }
        return tokenResponse;
    }

    @Override
    @Cacheable(value = "auth_token", key = "#id", unless = "#result.ok==false")
    public Response<Token> selectById(Long id) {
        Response<Token> response = tokenDbsFeignClient.selectById(id);
        return response;
    }

    @Override
    @Cacheable(value = "auth_token_user_id", key = "#id", unless = "#result.ok==false")
    public Response<Token> selectByUserId(Long id) {
        Response<Token> response = tokenDbsFeignClient.selectByUserId(id);
        return response;
    }

    @Override
    @Cacheable(value = "auth_token_list", keyGenerator = "commonKeyGenerator", unless = "#result.ok==false")
    public Response<Page<Token>> list(TokenDto tokenDto) {
        Response<Page<Token>> response = tokenDbsFeignClient.list(tokenDto);
        return response;
    }

    @Override
    public Response<Boolean> checkTokenValid(TokenDto tokenDto) {
        Response<User> userResponse = userAuthService.selectById(tokenDto.getUserId());
        if (userResponse.isOk()) {
            Response<Token> tokenResponse = selectByUserId(tokenDto.getUserId());
            if (tokenResponse.isOk()) {
                Token token = tokenResponse.getData();
                if (tokenDto.getToken().equals(token.getToken()) && token.getExpireTime().getTime() > (new Date()).getTime()) {
                    return Response.ok();
                }
                return Response.fail("token invalid");
            }
            return Response.fail(tokenResponse.getMessage());
        }
        return Response.fail(userResponse.getMessage());
    }
}
