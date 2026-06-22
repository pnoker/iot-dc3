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

package io.github.pnoker.common.auth.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.enums.OAuthClientTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * View object for registered OAuth client API responses. The client secret hash is never exposed.
 *
 * @author pnoker
 * @version 2026.6.19
 * @since 2026.6.19
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(description = "OAuth registered client view object")
public class OAuthClientVO extends BaseVO {

    @Schema(description = "OAuth client identifier.", example = "mcp-abcd1234")
    private String clientId;

    @Schema(description = "Human-readable client name.", example = "Claude Desktop")
    private String clientName;

    @Schema(description = "Client type: PUBLIC or CONFIDENTIAL.", example = "CONFIDENTIAL")
    private OAuthClientTypeEnum clientType;

    @Schema(description = "Principal that owns the client.", example = "1024")
    private Long ownerPrincipalId;

    @Schema(description = "Service account principal bound to the client, for client_credentials.", example = "2048")
    private Long serviceAccountPrincipalId;

    @Schema(description = "Tenant the client belongs to.", example = "1")
    private Long tenantId;

    @Schema(description = "Timestamp when the client secret expires.", example = "2027-06-19 12:00:00")
    private LocalDateTime clientSecretExpiresAt;

    @Schema(description = "Supported token endpoint authentication methods.", example = "client_secret_basic")
    private String clientAuthMethods;

    @Schema(description = "Allowed authorization grant types.", example = "authorization_code,refresh_token")
    private String authorizationGrantTypes;

    @Schema(description = "Registered redirect URIs.", example = "https://app.example.com/callback")
    private String redirectUris;

    @Schema(description = "Granted scopes.", example = "mcp.read mcp.invoke")
    private String scopes;

    @Schema(description = "Whether PKCE is required: 1 yes, 0 no.", example = "1")
    private Byte requirePkce;

    @Schema(description = "Whether user consent is required: 1 yes, 0 no.", example = "0")
    private Byte requireConsent;

    @Schema(description = "Enable flag: 0 enabled, 1 disabled.", example = "0")
    private Byte enableFlag;

}
