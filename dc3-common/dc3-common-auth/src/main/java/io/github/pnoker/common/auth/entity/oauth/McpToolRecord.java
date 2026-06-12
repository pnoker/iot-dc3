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

/**
 * MCP tool catalog projection.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Getter
@Setter
@ToString
public class McpToolRecord {

    private Long id;

    private String toolId;

    private String toolName;

    private String toolTitle;

    private String toolCategory;

    private String serviceName;

    private String apiCode;

    private String permissionCode;

    private String httpMethod;

    private String apiPath;

    private String schemaHash;

    private String riskLevel;

    private Byte readOnlyHint;

    private Byte destructiveHint;

    private Byte idempotentHint;

    private Byte openWorldHint;

    private Byte enableFlag;

    private String remark;

}
