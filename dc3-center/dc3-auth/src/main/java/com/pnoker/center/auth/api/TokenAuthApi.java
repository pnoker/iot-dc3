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

import com.pnoker.api.center.auth.token.feign.TokenAuthFeignClient;
import com.pnoker.center.auth.service.TokenAuthService;
import com.pnoker.common.bean.Response;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.auth.TokenDto;
import com.pnoker.common.entity.auth.Token;
import com.pnoker.common.entity.auth.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>TokenAuthApi
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@RestController
@RequestMapping(Common.Service.DC3_TOKEN_URL_PREFIX)
public class TokenAuthApi implements TokenAuthFeignClient {
    @Autowired
    private TokenAuthService tokenAuthService;

    @Override
    public Response<TokenDto> update(User user) {
        return tokenAuthService.update(user);
    }

    @Override
    public Response<Token> selectById(Long id) {
        return tokenAuthService.selectById(id);
    }

    @Override
    public Response<Boolean> checkTokenValid(TokenDto tokenDto) {
        return tokenAuthService.checkTokenValid(tokenDto);
    }

}
