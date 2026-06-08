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
public class UserHeaderUtil {

    private UserHeaderUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Get user header from Spring Security context.
     *
     * @return User header as Mono
     */
    public static Mono<RequestHeader.UserHeader> getUserHeader() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth instanceof GatewayAuthenticationToken)
                .cast(GatewayAuthenticationToken.class)
                .map(GatewayAuthenticationToken::getUserHeader)
                .switchIfEmpty(Mono.error(
                        new UnAuthorizedException("Unable to get user header from security context")));
    }

    /**
     * Get tenant ID from user header.
     *
     * @return Tenant ID as Mono
     */
    public static Mono<Long> getTenantId() {
        return getUserHeader().flatMap(userHeader -> {
            Long tenantId = userHeader.getTenantId();
            if (Objects.isNull(tenantId)) {
                return Mono.error(
                        new UnAuthorizedException("Unable to get tenant id of user header"));
            }
            return Mono.just(tenantId);
        });
    }

    /**
     * Get user ID from user header.
     *
     * @return User ID as Mono
     */
    public static Mono<Long> getUserId() {
        return getUserHeader().flatMap(userHeader -> {
            Long userId = userHeader.getUserId();
            if (Objects.isNull(userId)) {
                return Mono.error(
                        new UnAuthorizedException("Unable to get user id of user header"));
            }
            return Mono.just(userId);
        });
    }

    /**
     * Get user nickname from user header.
     *
     * @return User nickname as Mono
     */
    public static Mono<String> getNickName() {
        return getUserHeader().flatMap(userHeader -> {
            String nickName = userHeader.getNickName();
            if (StringUtils.isEmpty(nickName)) {
                return Mono.error(
                        new UnAuthorizedException("Unable to get nick name of user header"));
            }
            return Mono.just(nickName);
        });
    }

    /**
     * Get username from user header.
     *
     * @return Username as Mono
     */
    public static Mono<String> getUserName() {
        return getUserHeader().flatMap(userHeader -> {
            String userName = userHeader.getUserName();
            if (StringUtils.isEmpty(userName)) {
                return Mono.error(
                        new UnAuthorizedException("Unable to get user name of user header"));
            }
            return Mono.just(userName);
        });
    }
}
