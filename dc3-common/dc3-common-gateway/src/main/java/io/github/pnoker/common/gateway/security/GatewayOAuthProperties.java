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

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Gateway-side verification of OAuth RS256 access tickets (Bearer tokens) on API routes.
 *
 * <p>Disabled by default: until enabled with a reachable JWKS url, only classic login
 * tickets are accepted and behaviour is unchanged. Revocation lag is bounded by the
 * short access-ticket lifetime (decision Q3 in docs/design/token-unification-mcp-first-cli.md).
 *
 * @author pnoker
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "dc3.gateway.oauth")
public class GatewayOAuthProperties {

    /**
     * Accept OAuth RS256 bearer tickets on Authentic-filtered routes.
     */
    private boolean enabled = false;

    /**
     * JWKS document url exposed by the auth center (must match its configured issuer).
     */
    private String jwksUrl = "http://localhost:8300/auth/oauth2/jwks";

    /**
     * Required iss claim; must equal the auth center value.
     */
    private String issuer = "http://localhost:8300/auth";

    /**
     * Required aud claim; must equal the auth center value.
     */
    private String audience = "dc3-mcp";

    /**
     * How long a fetched JWKS document is reused before refresh.
     */
    private Duration jwksCacheTtl = Duration.ofMinutes(5);
}
