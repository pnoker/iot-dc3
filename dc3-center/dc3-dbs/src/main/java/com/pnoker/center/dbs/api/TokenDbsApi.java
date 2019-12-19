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
import com.pnoker.api.center.dbs.token.feign.TokenDbsFeignClient;
import com.pnoker.center.dbs.service.TokenDbsService;
import com.pnoker.common.bean.Response;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.auth.TokenDto;
import com.pnoker.common.entity.auth.Token;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * <p>TokenDbsApi
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@RestController
@RequestMapping(Common.Service.DC3_DBS_TOKEN_URL_PREFIX)
public class TokenDbsApi implements TokenDbsFeignClient {
    private final TokenDbsService tokenDbsService;

    public TokenDbsApi(TokenDbsService tokenDbsService) {
        this.tokenDbsService = tokenDbsService;
    }

    @Override
    public Response<Token> add(Token token) {
        try {
            token = tokenDbsService.add(token);
            return null != token ? Response.ok(token) : Response.fail("token record add failed");
        } catch (Exception e) {
            return Response.fail(e.getMessage());
        }
    }

    @Override
    public Response<Boolean> delete(Long id) {
        try {
            return tokenDbsService.delete(id) ? Response.ok() : Response.fail("token record delete failed");
        } catch (Exception e) {
            return Response.fail(e.getMessage());
        }
    }

    @Override
    public Response<Token> update(Token token) {
        if (null == token.getId()) {
            return Response.fail("token id is null");
        }
        try {
            token = tokenDbsService.update(token);
            return null != token ? Response.ok(token) : Response.fail("token record update failed");
        } catch (Exception e) {
            return Response.fail(e.getMessage());
        }
    }

    @Override
    public Response<Token> selectById(Long id) {
        try {
            Token token = tokenDbsService.selectById(id);
            return null != token ? Response.ok(token) : Response.fail(String.format("token record does not exist for id(%s)", id));
        } catch (Exception e) {
            return Response.fail(e.getMessage());
        }
    }

    @Override
    public Response<Token> selectByUserId(Long id) {
        try {
            Token token = tokenDbsService.selectByUserId(id);
            return null != token ? Response.ok(token) : Response.fail(String.format("token record does not exist for user id(%s)", id));
        } catch (Exception e) {
            return Response.fail(e.getMessage());
        }
    }

    @Override
    public Response<Page<Token>> list(TokenDto tokenDto) {
        if (!Optional.ofNullable(tokenDto).isPresent()) {
            tokenDto = new TokenDto();
        }
        try {
            return Response.ok(tokenDbsService.list(tokenDto));
        } catch (Exception e) {
            return Response.fail(e.getMessage());
        }
    }

}
