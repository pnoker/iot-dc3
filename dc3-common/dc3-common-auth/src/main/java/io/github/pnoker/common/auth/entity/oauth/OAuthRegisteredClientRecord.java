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

package io.github.pnoker.common.auth.entity.oauth;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Minimal OAuth registered client projection used by the OAuth endpoints.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Getter
@Setter
@ToString
public class OAuthRegisteredClientRecord {

    private Long id;

    private String clientId;

    private String clientName;

    private String clientType;

    private Long ownerPrincipalId;

    private Long serviceAccountPrincipalId;

    private Long tenantId;

    @ToString.Exclude
    private String clientSecretHash;

    private LocalDateTime clientSecretExpiresAt;

    private String clientAuthMethods;

    private String authorizationGrantTypes;

    private String redirectUris;

    private String scopes;

    private Byte requirePkce;

    private Byte requireConsent;

    private Byte enableFlag;

}
