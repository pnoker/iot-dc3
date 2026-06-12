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
 * OAuth authorization projection for authorization code, access token, refresh
 * token and revocation checks.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Getter
@Setter
@ToString
public class OAuthAuthorizationRecord {

    private Long id;

    private Long registeredClientId;

    private String clientId;

    private Long principalId;

    private String principalType;

    private Long tenantId;

    private Long mcpConnectionId;

    private String authorizationGrantType;

    private String authorizedScopes;

    private String stateHash;

    @ToString.Exclude
    private String authorizationCodeHash;

    private LocalDateTime authorizationCodeIssued;

    private LocalDateTime authorizationCodeExpires;

    private String accessTokenJti;

    private LocalDateTime accessTokenIssued;

    private LocalDateTime accessTokenExpires;

    @ToString.Exclude
    private String refreshTokenHash;

    private LocalDateTime refreshTokenIssued;

    private LocalDateTime refreshTokenExpires;

    private String tokenMetadata;

    private LocalDateTime revokedTime;

    private String revokeReason;

}
