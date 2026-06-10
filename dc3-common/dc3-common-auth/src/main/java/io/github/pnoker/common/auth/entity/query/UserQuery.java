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
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Query parameters for user listing and filtering.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User query parameters")
public class UserQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Pagination object")

    private Pages page;

    /**
     * Tenant scope. Populated by the controller from the request context; any value
     * supplied by the client is overwritten so a caller cannot reach across tenants.
     */
    @Schema(description = "Tenant ID", example = "1024")
    private Long tenantId;

    /**
     *
     */
    @Schema(description = "User nickname")
    private String nickName;

    /**
     * Name
     */
    @Schema(description = "Username")
    private String userName;

    /**
     *
     */
    @Schema(description = "Phone number", example = "13800138000")
    private String phone;

    /**
     *
     */
    @Schema(description = "Email address", example = "user@example.com")
    private String email;

    /**
     * Enable flag.
     */
    @Schema(description = "Enable flag enum (ENABLE or DISABLE)", example = "ENABLE")
    private EnableFlagEnum enableFlag;

}
