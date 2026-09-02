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
package io.github.pnoker.common.auth.biz.impl;

import io.github.pnoker.common.auth.biz.ReactiveTokenService;
import io.github.pnoker.common.auth.cache.TokenDenylistCache;
import io.github.pnoker.common.auth.entity.bean.TokenValid;
import io.github.pnoker.common.auth.entity.bo.LocalCredentialBO;
import io.github.pnoker.common.auth.entity.bo.TenantBO;
import io.github.pnoker.common.auth.service.ReactiveLocalCredentialCommandService;
import io.github.pnoker.common.auth.service.ReactiveLocalCredentialService;
import io.github.pnoker.common.auth.service.ReactivePrincipalService;
import io.github.pnoker.common.auth.service.ReactiveTenantService;
import io.github.pnoker.common.auth.support.ReactiveAuthScheduler;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.enums.ErrorCode;
import io.github.pnoker.common.enums.RequirePasswordChangeFlagEnum;
import io.github.pnoker.common.exception.PasswordChangeRequiredException;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.utils.KeyUtil;
import io.jsonwebtoken.Claims;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Reactive authentication workflow with explicit tenant scoping. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReactiveTokenServiceImpl implements ReactiveTokenService {

    private final ReactiveTenantService tenantService;
    private final ReactiveLocalCredentialService credentialService;
    private final ReactiveLocalCredentialCommandService credentialCommandService;
    private final ReactivePrincipalService principalService;
    private final TokenDenylistCache tokenDenylistCache;

    @Override
    public Mono<String> generateSalt(String loginName, String tenantCode) {
        return tenantService
                .getByCode(tenantCode)
                .switchIfEmpty(Mono.error(new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH)))
                .then(Mono.fromSupplier(() -> UUID.randomUUID().toString()));
    }

    @Override
    public Mono<String> generateToken(String loginName, String password, String tenantCode) {
        return tenantService
                .getByCode(tenantCode)
                .switchIfEmpty(Mono.error(new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH)))
                .flatMap(tenant -> credentialService
                        .getByLoginName(tenant.getId(), loginName)
                        .switchIfEmpty(Mono.error(new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH)))
                        .flatMap(credential -> authenticate(tenant, credential, password)))
                .flatMap(value -> principalService
                        .touchLastLogin(value.credential().getPrincipalId())
                        .then(generateJwt(value.credential(), value.tenant())))
                .onErrorMap(error ->
                        error instanceof PasswordChangeRequiredException || error instanceof UnAuthorizedException
                                ? error
                                : new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH));
    }

    @Override
    public Mono<Void> changePassword(String loginName, String currentPassword, String newPassword, String tenantCode) {
        return tenantService
                .getByCode(tenantCode)
                .switchIfEmpty(Mono.error(new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH)))
                .flatMap(tenant -> credentialCommandService.changePassword(
                        tenant.getId(), loginName, currentPassword, newPassword, 0L, "system"))
                .then();
    }

    @Override
    public Mono<Boolean> tryCancelToken(String loginName, String tenantCode) {
        return tenantService
                .getByCode(tenantCode)
                .flatMap(tenant -> credentialService
                        .getByLoginName(tenant.getId(), loginName)
                        .map(credential -> {
                            String principalKey = String.valueOf(credential.getPrincipalId());
                            long logoutEpochMs = System.currentTimeMillis();
                            tokenDenylistCache.markLogout(principalKey, tenantCode, logoutEpochMs);
                            log.info(
                                    "Principal logout, principalId={}, tenantCode={}, logoutEpochMs={}",
                                    principalKey,
                                    tenantCode,
                                    logoutEpochMs);
                            return Boolean.TRUE;
                        })
                        .defaultIfEmpty(false))
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<TokenValid> checkValid(String loginName, String token, String tenantCode) {
        return tenantService
                .getByCode(tenantCode)
                .switchIfEmpty(Mono.error(new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH)))
                .flatMap(tenant -> {
                    if (token == null || token.isBlank()) return Mono.just(new TokenValid(false, null));
                    return credentialService
                            .getByLoginName(tenant.getId(), loginName)
                            .flatMap(credential -> parseToken(credential, tenant, token))
                            .defaultIfEmpty(new TokenValid(false, null));
                });
    }

    private Mono<AuthContext> authenticate(TenantBO tenant, LocalCredentialBO credential, String password) {
        return credentialService.verifyPassword(credential, password).flatMap(valid -> {
            if (!valid) {
                return credentialCommandService
                        .recordFailedLogin(tenant.getId(), credential.getId())
                        .then(Mono.error(new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH)));
            }
            return credentialCommandService
                    .recordSuccessfulLogin(tenant.getId(), credential.getId())
                    .then(Mono.defer(() -> {
                        if (credential.getPasswordExpireTime() != null
                                && credential.getPasswordExpireTime().isBefore(LocalDateTime.now(Clock.systemUTC()))) {
                            return Mono.error(new PasswordChangeRequiredException(ErrorCode.PASSWORD_EXPIRED));
                        }
                        if (RequirePasswordChangeFlagEnum.REQUIRED == credential.getRequirePasswordChange()) {
                            return Mono.error(new PasswordChangeRequiredException(ErrorCode.PASSWORD_CHANGE_REQUIRED));
                        }
                        return Mono.just(new AuthContext(tenant, credential));
                    }));
        });
    }

    private Mono<String> generateJwt(LocalCredentialBO credential, TenantBO tenant) {
        return Mono.fromCallable(
                        () -> KeyUtil.generateToken(String.valueOf(credential.getPrincipalId()), tenant.getId()))
                .subscribeOn(ReactiveAuthScheduler.CRYPTO);
    }

    private Mono<TokenValid> parseToken(LocalCredentialBO credential, TenantBO tenant, String token) {
        return Mono.fromCallable(() -> {
                    String principalKey = String.valueOf(credential.getPrincipalId());
                    Claims claims = KeyUtil.parserToken(principalKey, token, tenant.getId());
                    Date issuedAt = claims.getIssuedAt();
                    long issuedAtEpochMs = issuedAt == null ? 0L : issuedAt.getTime();
                    if (tokenDenylistCache.isRevoked(principalKey, tenantCode(tenant), issuedAtEpochMs)) {
                        return new TokenValid(false, claims.getExpiration());
                    }
                    return new TokenValid(true, claims.getExpiration());
                })
                .subscribeOn(ReactiveAuthScheduler.CRYPTO)
                .onErrorResume(error -> {
                    log.warn("Token validation failed", error);
                    return Mono.just(new TokenValid(false, null));
                });
    }

    private String tenantCode(TenantBO tenant) {
        return tenant.getTenantCode();
    }

    private record AuthContext(TenantBO tenant, LocalCredentialBO credential) {}
}
