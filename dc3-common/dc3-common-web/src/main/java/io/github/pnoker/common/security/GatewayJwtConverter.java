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

package io.github.pnoker.common.security;

import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.utils.HmacAuthSigner;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.RequestUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converts the Gateway-issued X-Auth-Principal header to a
 * {@link GatewayAuthenticationToken}. Extracts identity, verifies the HMAC
 * signature, and eagerly loads the user's full permission set.
 * <p>
 * If the permission provider is temporarily unavailable, the token is still
 * created with an empty authority set.  This fails closed: the user is
 * authenticated but has no permissions, so any {@code @PreAuthorize} guard
 * returns 403 rather than masking a transient outage as a 401.
 *
 * @author pnoker
 * @version 2026.6.0
 * @since 2016.10.1
 */
@Slf4j
@RequiredArgsConstructor
public class GatewayJwtConverter implements ServerAuthenticationConverter {

    private final HmacAuthSigner hmacAuthSigner;

    private final PermissionProvider permissionProvider;

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        String principal = RequestUtil.getRequestHeader(
                exchange.getRequest(), RequestConstant.Header.X_AUTH_PRINCIPAL);

        if (StringUtils.isBlank(principal)) {
            if (log.isDebugEnabled()) {
                log.debug("No X-Auth-Principal header, proceeding as anonymous");
            }
            return Mono.empty();
        }

        // HMAC signature verification
        if (hmacAuthSigner.isEnabled()) {
            String sign = RequestUtil.getRequestHeader(
                    exchange.getRequest(), RequestConstant.Header.X_AUTH_SIGN);
            if (!hmacAuthSigner.verify(principal, sign)) {
                log.warn("Rejecting request — invalid HMAC signature, Url: {}",
                        exchange.getRequest().getURI());
                return Mono.empty();
            }
        }

        RequestHeader.PrincipalHeader principalHeader;
        try {
            principalHeader = JsonUtil.parseObject(principal, RequestHeader.PrincipalHeader.class);
        } catch (Exception e) {
            log.warn("Rejecting request — malformed X-Auth-Principal, Url: {}",
                    exchange.getRequest().getURI(), e);
            return Mono.empty();
        }

        if (principalHeader == null || principalHeader.getTenantId() == null
                || principalHeader.getPrincipalId() == null) {
            log.warn("Rejecting request — invalid principal header: {}",
                    JsonUtil.toJsonString(principalHeader));
            return Mono.empty();
        }

        Long tenantId = principalHeader.getTenantId();
        Long principalId = principalHeader.getPrincipalId();

        // Load authorities reactively — no blocking.
        // On transient failure, fall back to empty authorities (fail closed).
        return permissionProvider
                .listPermissionCodes(tenantId, principalId)
                .map(codes -> {
                    Set<GrantedAuthority> authorities = codes.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toSet());
                    if (log.isDebugEnabled()) {
                        log.debug("Gateway authentication, tenant={}, principal={}, authorities={}",
                                tenantId, principalId, authorities.size());
                    }
                    return (Authentication) new GatewayAuthenticationToken(principalHeader, authorities);
                })
                .onErrorResume(e -> {
                    log.error("Failed to load permissions, falling back to empty authorities"
                            + " (tenant={}, principal={})", tenantId, principalId, e);
                    return Mono.just(
                            new GatewayAuthenticationToken(principalHeader, Set.of()));
                });
    }
}
