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

package io.github.pnoker.common.gateway.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PrincipalTypeEnum;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.facade.api.LocalCredentialFacade;
import io.github.pnoker.common.facade.api.TenantFacade;
import io.github.pnoker.common.facade.api.TokenFacade;
import io.github.pnoker.common.facade.api.UserFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeLocalCredentialBO;
import io.github.pnoker.common.facade.entity.bo.FacadeTenantBO;
import io.github.pnoker.common.facade.entity.bo.FacadeUserBO;
import io.github.pnoker.common.gateway.service.FilterService;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.RequestUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Locale;

/**
 * Resolves tenant, local credential, and principal from the incoming request and
 * validates bearer tokens through the auth facade before the gateway forwards it.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FilterServiceImpl implements FilterService {

    /**
     * Short TTL — long enough to amortise the gRPC round-trip across a burst of requests
     * from the same tenant/user, short enough that disabling a tenant or user takes effect
     * within a minute. Optional<> wraps the value so we can negative-cache misses too.
     */
    private static final Duration AUTH_LOOKUP_TTL = Duration.ofSeconds(60);




    private final Cache<String, Mono<Optional<FacadeTenantBO>>> reactiveTenantCache = Caffeine.newBuilder()
            .expireAfterWrite(AUTH_LOOKUP_TTL).maximumSize(10_000).build();

    private final Cache<String, Mono<Optional<FacadeLocalCredentialBO>>> reactiveCredentialCache = Caffeine.newBuilder()
            .expireAfterWrite(AUTH_LOOKUP_TTL).maximumSize(10_000).build();

    private final Cache<String, Mono<Optional<FacadeUserBO>>> reactiveUserCache = Caffeine.newBuilder()
            .expireAfterWrite(AUTH_LOOKUP_TTL).maximumSize(10_000).build();





    private final TenantFacade tenantFacade;

    private final LocalCredentialFacade localCredentialFacade;

    private final UserFacade userFacade;

    private final TokenFacade tokenFacade;

    @Override
    public Mono<FacadeTenantBO> getTenantReactive(ServerHttpRequest request) {
        String code = RequestUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_TENANT);
        if (StringUtils.isBlank(code)) {
            return Mono.error(new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST));
        }
        String normalizedCode = code.trim();
        Mono<Optional<FacadeTenantBO>> lookup = reactiveTenantCache.get(normalizedCode, key ->
                Mono.defer(() -> tenantFacade.getByCode(key))
                        .filter(tenant -> tenant.getEnableFlag() == EnableFlagEnum.ENABLE)
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty())
                        .cache(AUTH_LOOKUP_TTL));
        return lookup.flatMap(this::optionalValue)
                .switchIfEmpty(Mono.error(new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST)));
    }

    @Override
    public Mono<FacadeLocalCredentialBO> getLocalCredentialReactive(ServerHttpRequest request, Long tenantId) {
        String name = RequestUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_LOGIN);
        if (StringUtils.isBlank(name) || tenantId == null || tenantId <= 0) {
            return Mono.error(new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST));
        }
        String normalizedName = name.trim().toLowerCase(Locale.ROOT);
        String key = tenantId + ":" + normalizedName;
        Mono<Optional<FacadeLocalCredentialBO>> lookup = reactiveCredentialCache.get(key, ignored ->
                Mono.defer(() -> localCredentialFacade.getByLoginName(tenantId, normalizedName))
                        .filter(credential -> credential.getEnableFlag() == EnableFlagEnum.ENABLE)
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty())
                        .cache(AUTH_LOOKUP_TTL));
        return lookup.flatMap(this::optionalValue)
                .switchIfEmpty(Mono.error(new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST)));
    }

    @Override
    public Mono<RequestHeader.PrincipalHeader> getUserReactive(FacadeLocalCredentialBO credential,
                                                               FacadeTenantBO tenant) {
        if (credential == null || tenant == null || credential.getPrincipalId() == null || tenant.getId() == null) {
            return Mono.error(new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST));
        }
        Long principalId = credential.getPrincipalId();
        String key = tenant.getId() + ":" + principalId;
        Mono<Optional<FacadeUserBO>> lookup = reactiveUserCache.get(key, ignored ->
                Mono.defer(() -> userFacade.getByPrincipalId(tenant.getId(), principalId))
                        .filter(user -> principalId.equals(user.getPrincipalId()))
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty())
                        .cache(AUTH_LOOKUP_TTL));
        return lookup.flatMap(this::optionalValue)
                .switchIfEmpty(Mono.error(new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST)))
                .map(user -> {
                    RequestHeader.PrincipalHeader header = new RequestHeader.PrincipalHeader();
                    header.setPrincipalId(principalId);
                    header.setPrincipalType(PrincipalTypeEnum.USER.getValue());
                    header.setDisplayName(user.getNickName());
                    header.setPrincipalName(user.getUserName());
                    header.setTenantId(tenant.getId());
                    return header;
                });
    }

    @Override
    public Mono<Void> checkValidReactive(ServerHttpRequest request, FacadeTenantBO tenant,
                                         FacadeLocalCredentialBO credential) {
        return Mono.defer(() -> {
            String token = RequestUtil.getRequestCookie(request, RequestConstant.Header.TOKEN_COOKIE);
            if (StringUtils.isBlank(token)) {
                String headerToken = RequestUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_TOKEN);
                try {
                    RequestHeader.TokenHeader header = JsonUtil.parseObject(headerToken, RequestHeader.TokenHeader.class);
                    token = header == null ? null : header.getToken();
                } catch (Exception error) {
                    return Mono.error(new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST, error));
                }
            }
            if (StringUtils.isBlank(token) || tenant == null || credential == null) {
                return Mono.error(new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST));
            }
            return tokenFacade.checkValid(tenant.getTenantCode(), credential.getLoginName(), token)
                    .flatMap(valid -> Boolean.TRUE.equals(valid)
                            ? Mono.<Void>empty()
                            : Mono.error(new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST)));
        });
    }

    private <T> Mono<T> optionalValue(Optional<T> value) {
        return value.map(Mono::just).orElseGet(Mono::empty);
    }

}
