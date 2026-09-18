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
package io.github.pnoker.common.gateway.security;

import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.McpConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.utils.OAuthJwtVerifier;
import io.jsonwebtoken.Claims;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Verifies an OAuth RS256 bearer ticket and projects it onto the shared
 * {@link RequestHeader.PrincipalHeader} every downstream service already trusts.
 *
 * <p>Tenant enablement is enforced by the auth center at issuance time (principal active
 * + tenant membership) and is not re-checked per request here — per decision Q3, no
 * introspection hop; staleness is bounded by the short access-ticket lifetime.
 *
 * @author pnoker
 */
@Slf4j
public class OAuthTokenResolver {

    /**
     * Authorization header scheme, with trailing space.
     */
    public static final String BEARER_PREFIX = "Bearer ";

    private final OAuthJwtVerifier verifier;

    /**
     * @param properties gateway-side OAuth verification configuration
     */
    public OAuthTokenResolver(GatewayOAuthProperties properties) {
        JwksKeySource keySource = new JwksKeySource(properties.getJwksUrl(), properties.getJwksCacheTtl());
        this.verifier = new OAuthJwtVerifier(properties.getIssuer(), properties.getAudience(), keySource);
    }

    /**
     * Whether the request carries a bearer ticket in the Authorization header.
     *
     * @param authorization raw Authorization header value
     * @return true when it starts with the bearer scheme
     */
    public static boolean isBearer(String authorization) {
        return StringUtils.startsWithIgnoreCase(authorization, "Bearer ");
    }

    /**
     * Verify the bearer ticket and map its claims onto a principal header.
     *
     * @param bearerToken token without the {@code Bearer } prefix
     * @return principal header carrying principalId, tenantId, principalType, clientId, connectionId, scopes
     * @throws UnAuthorizedException on any verification failure
     */
    public RequestHeader.PrincipalHeader resolve(String bearerToken) {
        Claims claims;
        try {
            claims = verifier.verify(bearerToken);
        } catch (Exception e) {
            log.debug("OAuth ticket rejected: {}", e.getMessage());
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }
        String subject = claims.getSubject();
        if (StringUtils.isBlank(subject)) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }
        try {
            Long.parseLong(subject);
        } catch (NumberFormatException e) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }

        RequestHeader.PrincipalHeader header = new RequestHeader.PrincipalHeader();
        header.setPrincipalId(Long.parseLong(subject));
        header.setTenantId(claims.get(McpConstant.Field.TENANT_ID, Long.class));
        if (header.getTenantId() == null || header.getTenantId() <= 0) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }
        String principalType = claims.get(McpConstant.Field.PRINCIPAL_TYPE, String.class);
        if (StringUtils.isNotBlank(principalType)) {
            header.setPrincipalType(principalType);
        }
        header.setClientId(claims.get(McpConstant.Field.CLIENT_ID, String.class));
        Object connectionId = claims.get(McpConstant.Field.MCP_CONNECTION_ID);
        if (connectionId instanceof Number number && number.longValue() > 0) {
            header.setConnectionId(number.longValue());
        }
        String scope = claims.get(McpConstant.Field.SCOPE, String.class);
        if (StringUtils.isNotBlank(scope)) {
            header.setScopes(Arrays.stream(scope.split("\\s+"))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .toList());
        }
        return header;
    }
}
