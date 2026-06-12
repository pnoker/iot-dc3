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

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Gateway filter service implementation.
 *
 * @author pnoker
 * @version 2025.9.0
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

    private final Cache<String, Optional<FacadeTenantBO>> tenantCache = Caffeine.newBuilder()
            .expireAfterWrite(AUTH_LOOKUP_TTL).maximumSize(10_000).build();

    private final Cache<String, Optional<FacadeLocalCredentialBO>> credentialCache = Caffeine.newBuilder()
            .expireAfterWrite(AUTH_LOOKUP_TTL).maximumSize(10_000).build();

    private final Cache<Long, Optional<FacadeUserBO>> userCache = Caffeine.newBuilder()
            .expireAfterWrite(AUTH_LOOKUP_TTL).maximumSize(10_000).build();

    private final TenantFacade tenantFacade;

    private final LocalCredentialFacade localCredentialFacade;

    private final UserFacade userFacade;

    private final TokenFacade tokenFacade;

    @Override
    public FacadeTenantBO getTenant(ServerHttpRequest request) {
        String code = RequestUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_TENANT);
        if (StringUtils.isEmpty(code)) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }

        FacadeTenantBO tenant = tenantCache.get(code, key -> Optional.ofNullable(tenantFacade.getByCode(key)))
                .orElse(null);
        if (Objects.isNull(tenant) || tenant.getEnableFlag() != EnableFlagEnum.ENABLE) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }
        return tenant;
    }

    @Override
    public FacadeLocalCredentialBO getLocalCredential(ServerHttpRequest request) {
        String name = RequestUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_LOGIN);
        if (StringUtils.isEmpty(name)) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }

        FacadeLocalCredentialBO credential = credentialCache
                .get(name, key -> Optional.ofNullable(localCredentialFacade.getByLoginName(key)))
                .orElse(null);
        if (Objects.isNull(credential) || credential.getEnableFlag() != EnableFlagEnum.ENABLE) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }
        return credential;
    }

    @Override
    public RequestHeader.PrincipalHeader getUser(FacadeLocalCredentialBO credential, FacadeTenantBO tenant) {
        Long principalId = credential.getPrincipalId();
        if (Objects.isNull(principalId)) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }

        FacadeUserBO user = userCache.get(principalId, id -> Optional.ofNullable(userFacade.getByPrincipalId(id)))
                .orElse(null);
        if (Objects.isNull(user) || !principalId.equals(user.getPrincipalId())) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }

        RequestHeader.PrincipalHeader header = new RequestHeader.PrincipalHeader();
        header.setPrincipalId(principalId);
        header.setPrincipalType(PrincipalTypeEnum.USER.getValue());
        header.setDisplayName(user.getNickName());
        header.setPrincipalName(user.getUserName());
        header.setTenantId(tenant.getId());
        return header;
    }

    @Override
    public void checkValid(ServerHttpRequest request, FacadeTenantBO tenant, FacadeLocalCredentialBO credential) {
        String token = RequestUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_TOKEN);
        RequestHeader.TokenHeader header;
        try {
            header = JsonUtil.parseObject(token, RequestHeader.TokenHeader.class);
        } catch (Exception e) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST, e);
        }
        if (Objects.isNull(header) || StringUtils.isAnyEmpty(header.getSalt(), header.getToken())) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }

        // Token validity is intentionally NOT cached — it's the freshness check.
        boolean valid = tokenFacade.checkValid(tenant.getTenantCode(), credential.getLoginName(), header.getSalt(),
                header.getToken());
        if (!valid) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }
    }

}
