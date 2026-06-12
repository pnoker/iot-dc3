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

import io.github.pnoker.common.entity.common.RequestHeader;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serial;
import java.util.Collection;
import java.util.Objects;

/**
 * Spring Security Authentication token derived from the Gateway-issued
 * X-Auth-Principal header. Carries the caller's identity and the full set of
 * resource-code authorities.
 *
 * @author pnoker
 * @version 2026.6.0
 * @since 2016.10.1
 */
public class GatewayAuthenticationToken extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = 1L;

    private final RequestHeader.PrincipalHeader principalHeader;

    public GatewayAuthenticationToken(RequestHeader.PrincipalHeader principalHeader,
                                      Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principalHeader = Objects.requireNonNull(principalHeader, "principalHeader must not be null");
        setAuthenticated(true);
    }

    @Override
    public Object getPrincipal() {
        return principalHeader;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    public RequestHeader.PrincipalHeader getPrincipalHeader() {
        return principalHeader;
    }

    public Long getTenantId() {
        return principalHeader.getTenantId();
    }

    public Long getUserId() {
        return principalHeader.getPrincipalId();
    }

    public Long getPrincipalId() {
        return principalHeader.getPrincipalId();
    }
}
