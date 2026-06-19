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

package io.github.pnoker.common.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.pnoker.common.constant.service.McpConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Supported OAuth dynamic client registration fields for MCP clients.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OAuth dynamic client registration request for MCP clients")
public class OAuthClientRegistrationRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Human-readable name for the OAuth client.", example = "Claude Desktop")
    @JsonProperty(McpConstant.Field.CLIENT_NAME_META)
    private String clientName;

    @Schema(description = "Client type: public or confidential.", example = "public")
    @JsonProperty(McpConstant.Field.CLIENT_TYPE)
    private String clientType;

    @Schema(description = "Authorized OAuth grant types.", example = "[\"authorization_code\"]")
    @JsonProperty(McpConstant.Field.GRANT_TYPES)
    private List<String> grantTypes;

    @Schema(description = "Allowed redirect URIs for the authorization code flow.", example = "[\"https://example.com/callback\"]")
    @JsonProperty(McpConstant.Field.REDIRECT_URIS)
    private List<String> redirectUris;

    @Schema(description = "Space-separated list of OAuth scopes the client may request.", example = "mcp:read mcp:write")
    private String scope;

    @Schema(description = "Tenant the client belongs to; defaults to the caller tenant.", example = "1")
    @JsonProperty(McpConstant.Field.TENANT_ID)
    private Long tenantId;

    @Schema(description = "Service-account principal id when the client acts on behalf of a service account.", example = "2048")
    @JsonProperty(McpConstant.Field.SERVICE_ACCOUNT_PRINCIPAL_ID)
    private Long serviceAccountPrincipalId;

}
