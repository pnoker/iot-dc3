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
import io.github.pnoker.common.enums.OAuthGrantTypeEnum;
import io.github.pnoker.common.enums.PrincipalTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * View object for MCP connection API responses.
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
@Schema(description = "MCP connection view object")
public class McpConnectionVO extends BaseVO {

    @Schema(description = "Connection display name.", example = "Claude Desktop - default")
    private String connectionName;

    @Schema(description = "Bound OAuth client identifier.", example = "mcp-abcd1234")
    private String clientId;

    @Schema(description = "Principal that owns the connection.", example = "1024")
    private Long principalId;

    @Schema(description = "Principal type: USER or SERVICE_ACCOUNT.", example = "USER")
    private PrincipalTypeEnum principalType;

    @Schema(description = "Tenant the connection belongs to.", example = "1")
    private Long tenantId;

    @Schema(description = "Authorization grant type.", example = "authorization_code")
    private OAuthGrantTypeEnum grantType;

    @Schema(description = "Enable flag: 0 enabled, 1 disabled.", example = "0")
    private Byte enableFlag;

    @Schema(description = "Timestamp when the connection expires.", example = "2027-06-19 12:00:00")
    private LocalDateTime expireTime;

    @Schema(description = "Timestamp when the connection was revoked.", example = "2026-06-19 12:00:00")
    private LocalDateTime revokeTime;

    @Schema(description = "Timestamp when the connection was last used.", example = "2026-06-19 12:00:00")
    private LocalDateTime lastUsedTime;

}
