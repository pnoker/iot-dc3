/*
 * Copyright 2022 Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.auth.api;

import io.github.pnoker.api.center.auth.feign.TokenClient;
import io.github.pnoker.center.auth.bean.TokenValid;
import io.github.pnoker.center.auth.service.TokenService;
import io.github.pnoker.common.bean.Login;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.ServiceConstant;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.utils.Dc3Util;
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
@RequestMapping(ServiceConstant.Auth.TOKEN_URL_PREFIX)
public class TokenApi implements TokenClient {

    @Resource
    private TokenService tokenService;

    @Override
    public R<String> generateSalt(Login login) {
        String salt = tokenService.generateSalt(login.getName(), login.getTenant());
        return null != salt ? R.ok(salt, "The salt will expire in 5 minutes") : R.fail();
    }

    @Override
    public R<String> generateToken(Login login) {
        String token = tokenService.generateToken(login.getName(), login.getSalt(), login.getPassword(), login.getTenant());
        return null != token ? R.ok(token, "The token will expire in 12 hours.") : R.fail();
    }

    @Override
    public R<String> checkTokenValid(Login login) {
        TokenValid tokenValid = tokenService.checkTokenValid(login.getName(), login.getSalt(), login.getToken(), login.getTenant());
        if (tokenValid.isValid()) {
            String expireTime = Dc3Util.formatCompleteData(tokenValid.getExpireTime());
            return R.ok(expireTime, "The token will expire in " + expireTime);
        }
        throw new UnAuthorizedException("Token invalid");
    }

    @Override
    public R<Boolean> cancelToken(Login login) {
        return tokenService.cancelToken(login.getName(), login.getTenant()) ? R.ok() : R.fail();
    }
}
