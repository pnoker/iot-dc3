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

package io.github.pnoker.common.auth.entity.bo;

import io.github.pnoker.common.entity.base.BaseBO;
import io.github.pnoker.common.enums.OAuthGrantTypeEnum;
import io.github.pnoker.common.enums.PrincipalTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Business object for creating an MCP connection (write-path input, domain enums).
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
public class McpConnectionAddBO extends BaseBO {

    private String connectionName;

    private String clientId;

    private Long principalId;

    private PrincipalTypeEnum principalType;

    private Long tenantId;

    private OAuthGrantTypeEnum grantType;

    private LocalDateTime expireTime;

}
