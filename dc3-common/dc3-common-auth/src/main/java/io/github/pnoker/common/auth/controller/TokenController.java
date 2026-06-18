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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "token", description = "Token-based authentication: login, logout, token refresh, and session invalidation for user and service account sessions")
@Slf4j
@RestController
@RequestMapping(AuthConstant.TOKEN_URL_PREFIX)
@RequiredArgsConstructor
public class TokenController implements BaseController {

    private final TokenService tokenService;

    /**
     * Generate a random password salt for a user under the given tenant.
     *
     * @param entityVO query carrying the user name and tenant used to mint the salt
     * @return the salt string (expires in 5 minutes), or a failure when the user cannot be resolved
     */
    // Public endpoint: invoked before login, so no @PreAuthorize. Path is also
    // permitted in WebFluxSecurityConfig (POST /token/salt).
    @PublicEndpoint
    @SecurityRequirements
    @Operation(summary = "Generate Token Salt", description = "Generate a random salt for a user under the given tenant, " +
            "used to salt the password on the subsequent token-generation call. The salt expires in 5 minutes; returns it as a string.")
    @PostMapping("/salt")
    public Mono<R<String>> generateSalt(@Validated @RequestBody TokenQuery entityVO) {
        return async(() -> {
            String salt = tokenService.generateSalt(entityVO.getName(), entityVO.getTenant());
            return Objects.nonNull(salt) ? R.ok(salt, "The salt will expire in 5 minutes") : R.fail();
        });
    }

    /**
     * Issue an access token for a user after validating name, salt, password and tenant.
     *
     * @param entityVO query carrying the user name, salt, password and tenant
     * @return the access token (valid for 12 hours), or a failure when credentials are invalid
     */
    // Public endpoint: invoked during login (before a token exists), so no
    // @PreAuthorize. Path is also permitted in WebFluxSecurityConfig (POST /token/generate).
    @PublicEndpoint
    @SecurityRequirements
    @Operation(summary = "Generate Token", description = "Issue an access token for a user by validating name, salt, password and tenant. " +
            "Call after generating a salt; the returned token authenticates the user for 12 hours.")
    @PostMapping("/generate")
    public Mono<R<String>> generateToken(@Validated @RequestBody TokenQuery entityVO) {
        return async(() -> {
            String token = tokenService.generateToken(entityVO.getName(), entityVO.getSalt(), entityVO.getPassword(),
                    entityVO.getTenant());
            return Objects.nonNull(token) ? R.ok(token, "The token will expire in 12 hours.") : R.fail();
        });
    }

    /**
     * Self-service password change. Public because a credential flagged for a mandatory
     * change or expired password cannot obtain a token until the password is changed.
     *
     * @param entityVO {@link TokenQuery} carrying name, tenant, current password and newPassword
     * @return true when the password was changed
     */
    // Public endpoint: invoked during login when no token can be issued yet, so no
    // @PreAuthorize. Path is also permitted in WebFluxSecurityConfig (POST /token/change_password).
    @PublicEndpoint
    @SecurityRequirements
    @Operation(summary = "Change Password", description = "Change a user's password using the current password and a new one, scoped to the given tenant. " +
            "Use during login when a token cannot be issued because the credential is expired or flagged for a mandatory change.")
    @PostMapping("/change_password")
    public Mono<R<Boolean>> changePassword(@Validated @RequestBody TokenQuery entityVO) {
        return async(() -> {
            tokenService.changePassword(entityVO.getName(), entityVO.getPassword(), entityVO.getNewPassword(),
                    entityVO.getTenant());
            return R.ok(true, "Password changed");
        });
    }

    /**
     * Acknowledge a client-initiated logout for the current token.
     *
     * @param entityVO {@link TokenQuery} carrying the name and tenant identifying the token to cancel
     * @return true when the logout was accepted
     */
    @PreAuthorize("@perm.can('token', 'delete')")
    @Operation(summary = "Cancel Token", description = "Acknowledge a client-initiated logout for the current token of the named user under the given tenant. " +
            "Requires token:delete permission; returns true when the session is cancelled.")
    @PostMapping("/cancel")
    public Mono<R<Boolean>> cancelToken(@Validated @RequestBody TokenQuery entityVO) {
        return async(() -> {
            boolean ok = tokenService.tryCancelToken(entityVO.getName(), entityVO.getTenant());
            return ok ? R.ok(true, "Token cancelled") : R.fail("Cancel token failed");
        });
    }

    /**
     * Check whether the supplied token for the named user and tenant is still valid.
     *
     * @param entityVO query carrying the user name, salt, token and tenant to validate
     * @return the token validity flag, with a message stating its expiry or expiration time
     */
    @PreAuthorize("@perm.can('token', 'get')")
    @Operation(summary = "Validate Token", description = "Check whether the supplied token for the named user and tenant is still valid. " +
            "Requires token:get permission; returns validity plus the token's expiry time.")
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
