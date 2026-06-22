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
import io.github.pnoker.common.enums.McpAuditStatusEnum;
import io.github.pnoker.common.enums.McpRiskLevelEnum;
import io.github.pnoker.common.enums.PrincipalTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * View object for MCP tool-call audit API responses.
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
@Schema(description = "MCP audit view object")
public class McpAuditVO extends BaseVO {

    @Schema(description = "Trace id of the tool call.", example = "a1b2c3d4e5f6")
    private String traceId;

    @Schema(description = "Tenant the audit record belongs to.", example = "1")
    private Long tenantId;

    @Schema(description = "Principal that invoked the tool.", example = "1024")
    private Long principalId;

    @Schema(description = "Principal type: USER or SERVICE_ACCOUNT.", example = "USER")
    private PrincipalTypeEnum principalType;

    @Schema(description = "OAuth client identifier.", example = "mcp-abcd1234")
    private String clientId;

    @Schema(description = "MCP connection id.", example = "1024")
    private Long connectionId;

    @Schema(description = "Invoked tool id.", example = "tool_read_device")
    private String toolId;

    @Schema(description = "Invoked tool name.", example = "read_device")
    private String toolName;

    @Schema(description = "Permission code checked for the call.", example = "device:get")
    private String permissionCode;

    @Schema(description = "Tool risk level: LOW, MEDIUM or HIGH.", example = "LOW")
    private McpRiskLevelEnum riskLevel;

    @Schema(description = "Confirmation id for high-risk calls.", example = "cfm-abcd1234")
    private String confirmId;

    @Schema(description = "Idempotency key supplied by the caller.", example = "idem-abcd1234")
    private String idempotencyKey;

    @Schema(description = "Digest of the call arguments.", example = "a1b2c3d4")
    private String argumentDigest;

    @Schema(description = "Invocation outcome: SUCCESS, DENIED, POLICY_DENIED, ERROR or UNKNOWN.", example = "SUCCESS")
    private McpAuditStatusEnum status;

    @Schema(description = "Error code when the call failed.", example = "policy_denied")
    private String errorCode;

    @Schema(description = "Call duration in milliseconds.", example = "42")
    private Long durationMs;

    @Schema(description = "Client name reported by the caller.", example = "Claude Desktop")
    private String clientName;

    @Schema(description = "Client version reported by the caller.", example = "1.0.0")
    private String clientVersion;

    @Schema(description = "Remote IP of the caller.", example = "203.0.113.1")
    private String remoteIp;

}
