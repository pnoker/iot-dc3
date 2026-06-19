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

package io.github.pnoker.common.auth.entity.query;

import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.CredentialTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * Query parameters for local credentials.
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
@Schema(description = "Local credential query parameters")
public class LocalCredentialQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Pagination parameters including page number, page size, time range, and sort order")
    private Pages page;

    @Schema(description = "Tenant identifier used to scope credential queries; all results are restricted to this tenant.", example = "1024")
    private Long tenantId;

    @Schema(description = "Identifier of the principal (user) whose local credentials are being queried; must belong to the current tenant.", example = "2048")
    private Long principalId;

    @Schema(description = "Login name of the credential to filter by; supports exact match.", example = "alice")
    private String loginName;

    @Schema(description = "Type of local credential to filter by.", example = "PASSWORD")
    private CredentialTypeEnum credentialType;

    @Schema(description = "Enable/disable status filter for local credentials.", example = "ENABLE")
    private EnableFlagEnum enableFlag;

}
