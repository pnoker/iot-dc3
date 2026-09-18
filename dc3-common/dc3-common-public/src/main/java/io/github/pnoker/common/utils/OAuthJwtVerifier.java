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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Shared RS256 access-token verifier for OAuth/MCP tickets.
 *
 * <p>Single place that knows how an issued ticket is validated: RS256 signature against
 * a key looked up by the token's {@code kid} header, plus issuer and audience claims.
 * Standard {@code exp}/{@code nbf} handling comes from the JWT parser itself.
 *
 * <p>Consumers supply different {@link KeySource}s: the auth center serves its own key
 * material; the gateway resolves keys from a cached JWKS document. Claim names are owned
 * by {@code McpConstant.Field}.
 *
 * @author pnoker
 */
public final class OAuthJwtVerifier {

    /**
     * Resolves the verification key for a given {@code kid} header value.
     */
    public interface KeySource {

        /**
         * Resolve the verification key identified by the token header.
         *
         * @param kid key id from the JWT header, never null
         * @return the RSA public key, or null when the kid is unknown (caller rejects)
         */
        RSAPublicKey forKeyId(String kid);
    }

    private final String issuer;

    private final String audience;

    private final KeySource keys;

    /**
     * Verifier for tokens minted under one issuer/audience pair.
     *
     * @param issuer   required iss claim
     * @param audience required aud claim
     * @param keys     key lookup by kid
     */
    public OAuthJwtVerifier(String issuer, String audience, KeySource keys) {
        this.issuer = issuer;
        this.audience = audience;
        this.keys = keys;
    }

    /**
     * Parse and fully verify signature, issuer, audience, and standard time claims.
     *
     * @param token compact JWS
     * @return verified claims
     * @throws JwtException on any validation failure (bad signature, unknown kid,
     *                      wrong iss/aud, expired, not yet valid, malformed)
     */
    public Claims verify(String token) {
        return Jwts.parser()
                .requireIssuer(issuer)
                .requireAudience(audience)
                .keyLocator(header -> {
                    Object kid = header.get("kid");
                    PublicKey key = kid == null ? null : keys.forKeyId(String.valueOf(kid));
                    if (!(key instanceof RSAPublicKey rsa)) {
                        throw new JwtException("Unknown or non-RSA signing key" + (kid == null ? "" : ": " + kid));
                    }
                    return rsa;
                })
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
