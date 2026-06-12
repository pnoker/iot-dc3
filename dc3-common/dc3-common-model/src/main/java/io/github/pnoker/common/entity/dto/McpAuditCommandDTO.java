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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * Gateway-to-auth MCP audit insert command.
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
public class McpAuditCommandDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String traceId;

    private Long tenantId;

    private Long principalId;

    private String principalType;

    private String clientId;

    private Long connectionId;

    private String toolId;

    private String toolName;

    private String permissionCode;

    private String riskLevel;

    private String confirmId;

    private String idempotencyKey;

    private String argumentDigest;

    private String status;

    private String errorCode;

    private Long durationMs;

    private String clientName;

    private String clientVersion;

    private String remoteIp;

}
