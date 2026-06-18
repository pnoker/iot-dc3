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
package io.github.pnoker.common.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuration properties for the OAuth runtime.
 *
 * <p>
 * Prefix: {@code dc3.oauth}
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "dc3.oauth")
public class OAuthProperties {

    /**
     * Issuer identifier advertised in OAuth metadata and signed into issued tokens.
     */
    private String issuer = "http://localhost:8300/auth";

    /**
     * Audience required on issued and verified access tokens.
     */
    private String audience = "dc3-mcp";

    /**
     * Lifetime of an authorization code before it expires.
     */
    private Duration authorizationCodeTtl = Duration.ofMinutes(5);

    /**
     * Lifetime of an issued access token.
     */
    private Duration accessTokenTtl = Duration.ofMinutes(15);

    /**
     * Lifetime of an issued refresh token.
     */
    private Duration refreshTokenTtl = Duration.ofDays(30);

    /**
     * JWT signing key material. When both keys are blank an ephemeral key pair is generated.
     */
    private Jwt jwt = new Jwt();

    /**
     * JWT signing key material, bound under {@code dc3.oauth.jwt}.
     */
    @Getter
    @Setter
    public static class Jwt {

        /**
         * Base64-encoded PKCS8 private key. Blank means an ephemeral key pair is generated.
         */
        private String privateKey = "";

        /**
         * Base64-encoded X509 public key. Blank means an ephemeral key pair is generated.
         */
        private String publicKey = "";

    }

}
