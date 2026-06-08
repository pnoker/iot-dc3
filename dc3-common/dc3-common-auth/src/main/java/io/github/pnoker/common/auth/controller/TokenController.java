/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.auth.controller;

import io.github.pnoker.common.annotation.PublicEndpoint;
import io.github.pnoker.common.auth.biz.TokenService;
import io.github.pnoker.common.auth.entity.bean.TokenValid;
import io.github.pnoker.common.auth.entity.query.TokenQuery;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.utils.TimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * REST controller for token validation and management.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@RestController
@RequestMapping(AuthConstant.TOKEN_URL_PREFIX)
@RequiredArgsConstructor
public class TokenController implements BaseController {

    private final TokenService tokenService;

    /**
     * @param entityVO {@link TokenQuery}
     * @return
     */
    @PublicEndpoint
    @PreAuthorize("@perm.can('token', 'get')")
    @PostMapping("/salt")
    public Mono<R<String>> generateSalt(@Validated @RequestBody TokenQuery entityVO) {
        return async(() -> {
            String salt = tokenService.generateSalt(entityVO.getName(), entityVO.getTenant());
            return Objects.nonNull(salt) ? R.ok(salt, "The salt will expire in 5 minutes") : R.fail();
        });
    }

    /**
     * Token
     *
     * @param entityVO {@link TokenQuery}
     * @return Token
     */
    @PublicEndpoint
    @PreAuthorize("@perm.can('token', 'add')")
    @PostMapping("/generate")
    public Mono<R<String>> generateToken(@Validated @RequestBody TokenQuery entityVO) {
        return async(() -> {
            String token = tokenService.generateToken(entityVO.getName(), entityVO.getSalt(), entityVO.getPassword(),
                    entityVO.getTenant());
            return Objects.nonNull(token) ? R.ok(token, "The token will expire in 12 hours.") : R.fail();
        });
    }

    /**
     * Acknowledge a client-initiated logout for the current token.
     *
     * @param entityVO {@link TokenQuery}
     * @return true when the logout was accepted
     */
    @PreAuthorize("@perm.can('token', 'delete')")
    @PostMapping("/cancel")
    public Mono<R<Boolean>> cancelToken(@Validated @RequestBody TokenQuery entityVO) {
        return async(() -> {
            boolean ok = tokenService.tryCancelToken(entityVO.getName(), entityVO.getTenant());
            return ok ? R.ok(true, "Token cancelled") : R.fail("Cancel token failed");
        });
    }

    /**
     * Token
     *
     * @param entityVO {@link TokenQuery}
     * @return ,
     */
    @PreAuthorize("@perm.can('token', 'get')")
    @PostMapping("/check")
    public Mono<R<Boolean>> checkValid(@Validated @RequestBody TokenQuery entityVO) {
        return async(() -> {
            TokenValid tokenValid = tokenService.checkValid(entityVO.getName(), entityVO.getSalt(), entityVO.getToken(),
                    entityVO.getTenant());

            boolean valid = tokenValid.isValid();
            String message = "The token has expired";
            if (valid && Objects.nonNull(tokenValid.getExpireTime())) {
                String expireTime = TimeUtil.completeFormat(tokenValid.getExpireTime());
                message = "The token will expire in " + expireTime;
            } else if (!valid && Objects.nonNull(tokenValid.getExpireTime())) {
                String expireTime = TimeUtil.completeFormat(tokenValid.getExpireTime());
                message = "The token has expired in " + expireTime;
            }

            return R.ok(valid, message);
        });
    }

}
