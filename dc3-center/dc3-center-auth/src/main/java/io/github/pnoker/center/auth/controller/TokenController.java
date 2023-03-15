/*
 * Copyright 2016-present the original author or authors.
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

package io.github.pnoker.center.auth.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.center.auth.entity.bean.TokenValid;
import io.github.pnoker.center.auth.service.TokenService;
import io.github.pnoker.common.constant.service.AuthServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.auth.Login;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.utils.TimeUtil;
import io.github.pnoker.common.valid.Auth;
import io.github.pnoker.common.valid.Check;
import io.github.pnoker.common.valid.Update;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 令牌 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(AuthServiceConstant.TOKEN_URL_PREFIX)
public class TokenController {

    @Resource
    private TokenService tokenService;

    /**
     * 生成用户随机盐值
     *
     * @param login 登录信息
     * @return 盐值
     */
    @PostMapping("/salt")
    public R<String> generateSalt(@Validated(Update.class) @RequestBody Login login) {
        String salt = tokenService.generateSalt(login.getName(), login.getTenant());
        return ObjectUtil.isNotNull(salt) ? R.ok(salt, "The salt will expire in 5 minutes") : R.fail();
    }

    /**
     * 生成用户 Token 令牌
     *
     * @param login 登录信息
     * @return Token 令牌
     */
    @PostMapping("/generate")
    public R<String> generateToken(@Validated(Auth.class) @RequestBody Login login) {
        String token = tokenService.generateToken(login.getName(), login.getSalt(), login.getPassword(), login.getTenant());
        return ObjectUtil.isNotNull(token) ? R.ok(token, "The token will expire in 12 hours.") : R.fail();
    }

    /**
     * 检测用户 Token 令牌是否有效
     *
     * @param login 登录信息
     * @return 如果有效，返回过期时间
     */
    @PostMapping("/check")
    public R<String> checkTokenValid(@Validated(Check.class) @RequestBody Login login) {
        TokenValid tokenValid = tokenService.checkTokenValid(login.getName(), login.getSalt(), login.getToken(), login.getTenant());
        if (tokenValid.isValid()) {
            String expireTime = TimeUtil.completeFormat(tokenValid.getExpireTime());
            return R.ok(expireTime, "The token will expire in " + expireTime);
        }
        throw new UnAuthorizedException("Token invalid");
    }

    /**
     * 注销用户的Token令牌
     *
     * @param login 登录信息
     * @return 是否注销
     */
    @PostMapping("/cancel")
    public R<Boolean> cancelToken(@Validated(Update.class) @RequestBody Login login) {
        return Boolean.TRUE.equals(tokenService.cancelToken(login.getName(), login.getTenant())) ? R.ok() : R.fail();
    }
}
