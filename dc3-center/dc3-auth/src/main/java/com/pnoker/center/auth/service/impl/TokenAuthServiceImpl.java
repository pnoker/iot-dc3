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
import com.pnoker.api.center.dbs.token.feign.TokenDbsFeignClient;
import com.pnoker.api.center.dbs.user.feign.UserDbsFeignClient;
import com.pnoker.center.auth.service.TokenAuthService;
import com.pnoker.common.bean.Response;
import com.pnoker.common.dto.auth.TokenDto;
import com.pnoker.common.entity.auth.Token;
import com.pnoker.common.entity.auth.User;
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
    private UserDbsFeignClient userDbsFeignClient;
    @Resource
    private TokenDbsFeignClient tokenDbsFeignClient;

    @Override
    public Response<TokenDto> update(User user) {
        Response<User> userResponse = userDbsFeignClient.selectById(user.getId());
        if (userResponse.isOk()) {
            user = userResponse.getData();
            Response<Token> tokenResponse = tokenDbsFeignClient.selectById(user.getTokenId());
            if (tokenResponse.isOk()) {
                // 生成Token
                Token token = tokenResponse.getData().expireTime(6).setToken(IdUtil.simpleUUID());
                Response<Token> response = tokenDbsFeignClient.update(token);
                if (response.isOk()) {
                    TokenDto tokenDto = new TokenDto();
                    tokenDto.convertToDto(tokenResponse.getData());
                    return Response.ok(tokenDto);
                }
                return Response.fail(response.getMessage());
            }
            return Response.fail(tokenResponse.getMessage());
        }
        return Response.fail(userResponse.getMessage());
    }

    @Override
    public Response<Token> selectById(Long id) {
        Response<Token> response = tokenDbsFeignClient.selectById(id);
        return response;
    }

    @Override
    public Response<Boolean> checkTokenValid(TokenDto tokenDto) {
        Response<User> userResponse = userDbsFeignClient.selectById(tokenDto.getUserId());
        if (userResponse.isOk()) {
            User user = userResponse.getData();
            Response<Token> tokenResponse = tokenDbsFeignClient.selectById(user.getTokenId());
            if (tokenResponse.isOk()) {
                Token token = tokenResponse.getData();
                if (tokenDto.getToken().equals(token.getToken()) && token.getExpireTime().getTime() > (new Date()).getTime()) {
                    return Response.ok(true);
                }
                return Response.fail("token invalid");
            }
            return Response.fail(tokenResponse.getMessage());
        }
        return Response.fail(userResponse.getMessage());
    }
}
