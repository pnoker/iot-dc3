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

package io.github.pnoker.common.utils;

import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.security.GatewayAuthenticationToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * User Header Utility Class
 * <p>
 * Utility class for extracting user information from the Spring Security
 * {@link ReactiveSecurityContextHolder} in reactive applications.
 * </p>
 *
 * @author pnoker
 * @version 2026.6.0
 * @since 2016.10.1
 */
@Slf4j
public class PrincipalHeaderUtil {

    private PrincipalHeaderUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Get principal header from Spring Security context.
     *
     * @return Principal header as Mono
     */
    public static Mono<RequestHeader.PrincipalHeader> getPrincipalHeader() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth instanceof GatewayAuthenticationToken)
                .cast(GatewayAuthenticationToken.class)
                .map(GatewayAuthenticationToken::getPrincipalHeader)
                .switchIfEmpty(Mono.error(
                        new UnAuthorizedException("Unable to get principal header from security context")));
    }

    /**
     * Get tenant ID from principal header.
     *
     * @return Tenant ID as Mono
     */
    public static Mono<Long> getTenantId() {
        return getPrincipalHeader().flatMap(principalHeader -> {
            Long tenantId = principalHeader.getTenantId();
            if (Objects.isNull(tenantId)) {
                return Mono.error(
                        new UnAuthorizedException("Unable to get tenant id of principal header"));
            }
            return Mono.just(tenantId);
        });
    }

    /**
     * Get principal ID from principal header.
     *
     * @return Principal ID as Mono
     */
    public static Mono<Long> getUserId() {
        return getPrincipalHeader().flatMap(principalHeader -> {
            Long userId = principalHeader.getPrincipalId();
            if (Objects.isNull(userId)) {
                return Mono.error(
                        new UnAuthorizedException("Unable to get principal id of principal header"));
            }
            return Mono.just(userId);
        });
    }

    /**
     * Get principal display name from principal header.
     *
     * @return Principal display name as Mono
     */
    public static Mono<String> getNickName() {
        return getPrincipalHeader().flatMap(principalHeader -> {
            String nickName = principalHeader.getDisplayName();
            if (StringUtils.isEmpty(nickName)) {
                return Mono.error(
                        new UnAuthorizedException("Unable to get display name of principal header"));
            }
            return Mono.just(nickName);
        });
    }

    /**
     * Get principal name from principal header.
     *
     * @return Principal name as Mono
     */
    public static Mono<String> getUserName() {
        return getPrincipalHeader().flatMap(principalHeader -> {
            String userName = principalHeader.getPrincipalName();
            if (StringUtils.isEmpty(userName)) {
                return Mono.error(
                        new UnAuthorizedException("Unable to get principal name of principal header"));
            }
            return Mono.just(userName);
        });
    }
}
