/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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

package io.github.pnoker.common.auth.controller;

import io.github.pnoker.common.auth.biz.TokenService;
import io.github.pnoker.common.auth.entity.bean.TokenValid;
import io.github.pnoker.common.auth.entity.query.TokenQuery;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.utils.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * 令牌 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(AuthConstant.TOKEN_URL_PREFIX)
public class TokenController implements BaseController {

    private final TokenService tokenService;

    public TokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * 生成用户随机盐值
     *
     * @param entityVO 生成盐值请求体 {@link TokenQuery}
     * @return 盐值
     */
    @PostMapping("/salt")
    public Mono<R<String>> generateSalt(@Validated @RequestBody TokenQuery entityVO) {
        String salt = tokenService.generateSalt(entityVO.getName(), entityVO.getTenant());
        return Objects.nonNull(salt) ? Mono.just(R.ok(salt, "The salt will expire in 5 minutes")) : Mono.just(R.fail());
    }

    /**
     * 生成用户 Token 令牌
     *
     * @param entityVO 生成令牌请求体 {@link TokenQuery}
     * @return Token 令牌
     */
    @PostMapping("/generate")
    public Mono<R<String>> generateToken(@Validated @RequestBody TokenQuery entityVO) {
        String token = tokenService.generateToken(entityVO.getName(), entityVO.getSalt(), entityVO.getPassword(), entityVO.getTenant());
        return Objects.nonNull(token) ? Mono.just(R.ok(token, "The token will expire in 12 hours.")) : Mono.just(R.fail());
    }

    /**
     * 检测用户 Token 令牌是否有效
     *
     * @param entityVO 校验令牌请求体 {@link TokenQuery}
     * @return 是否有效, 并返回过期时间
     */
    @PostMapping("/check")
    public Mono<R<Boolean>> checkValid(@Validated @RequestBody TokenQuery entityVO) {
        TokenValid tokenValid = tokenService.checkValid(entityVO.getName(), entityVO.getSalt(), entityVO.getToken(), entityVO.getTenant());

        boolean valid = tokenValid.isValid();
        String message = "The token has expired";
        if (valid && Objects.nonNull(tokenValid.getExpireTime())) {
            String expireTime = TimeUtil.completeFormat(tokenValid.getExpireTime());
            message = "The token will expire in " + expireTime;
        } else if (!valid && Objects.nonNull(tokenValid.getExpireTime())) {
            String expireTime = TimeUtil.completeFormat(tokenValid.getExpireTime());
            message = "The token has expired in " + expireTime;
        }

        return Mono.just(R.ok(valid, message));
    }

    /**
     * 注销用户的Token令牌
     *
     * @param entityVO 注销令牌请求体 {@link TokenQuery}
     * @return 是否注销成功
     */
    @PostMapping("/cancel")
    public Mono<R<Boolean>> cancelToken(@Validated @RequestBody TokenQuery entityVO) {
        return Boolean.TRUE.equals(tokenService.cancelToken(entityVO.getName(), entityVO.getTenant())) ? Mono.just(R.ok()) : Mono.just(R.fail());
    }
}
