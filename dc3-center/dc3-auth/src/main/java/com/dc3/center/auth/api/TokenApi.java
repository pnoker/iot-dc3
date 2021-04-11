/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.center.auth.api;

import com.dc3.api.center.auth.feign.TokenClient;
import com.dc3.center.auth.bean.TokenValid;
import com.dc3.center.auth.service.TokenService;
import com.dc3.common.bean.R;
import com.dc3.common.constant.Common;
import com.dc3.common.exception.UnAuthorizedException;
import com.dc3.common.model.User;
import com.dc3.common.utils.Dc3Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 令牌 Feign Client 接口实现
 *
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(Common.Service.DC3_AUTH_TOKEN_URL_PREFIX)
public class TokenApi implements TokenClient {
    @Resource
    private TokenService tokenService;

    @Override
    public R<String> generateSalt(String username) {
        String salt = tokenService.generateSalt(username);
        return null != salt ? R.ok(salt, "The salt will expire in 5 minutes") : R.fail();
    }

    @Override
    public R<String> generateToken(User user) {
        String token = tokenService.generateToken(user);
        return null != token ? R.ok(token, "The token will expire in 12 hours.") : R.fail();
    }

    @Override
    public R<Boolean> checkTokenValid(String username, String salt, String token) {
        TokenValid tokenValid = tokenService.checkTokenValid(username, salt, token);
        if (tokenValid.isValid()) {
            return R.ok("The token will expire in " + Dc3Util.formatData(tokenValid.getExpireTime()));
        }
        throw new UnAuthorizedException("Token invalid");
    }

    @Override
    public R<Boolean> cancelToken(String username) {
        return tokenService.cancelToken(username) ? R.ok() : R.fail();
    }
}
