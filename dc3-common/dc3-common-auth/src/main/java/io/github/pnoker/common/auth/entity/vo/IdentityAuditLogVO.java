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

import io.github.pnoker.common.entity.ext.JsonExt;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * View object for identity and authorization audit log API responses.
 *
 * @author pnoker
 * @version 2026.6.14
 * @since 2026.6.14
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Schema(description = "Identity audit log view object")
public class IdentityAuditLogVO {

    /**
     * Primary key ID
     */
    @Schema(description = "Primary key ID")
    private Long id;

    /**
     * Tenant ID
     */
    @Schema(description = "Tenant ID")
    private Long tenantId;

    /**
     * Principal ID
     */
    @Schema(description = "Principal ID")
    private Long principalId;

    /**
     * Principal type
     */
    @Schema(description = "Principal type")
    private String principalType;

    /**
     * Action
     */
    @Schema(description = "Audited action")
    private String action;

    /**
     * Resource type
     */
    @Schema(description = "Resource type")
    private String resourceType;

    /**
     * Resource ID
     */
    @Schema(description = "Resource ID")
    private Long resourceId;

    /**
     * Resource name
     */
    @Schema(description = "Resource name")
    private String resourceName;

    /**
     * Outcome status
     */
    @Schema(description = "Outcome status")
    private String status;

    /**
     * Error code
     */
    @Schema(description = "Error code")
    private String errorCode;

    /**
     * Detail extension, serialized as JSON
     */
    @Schema(description = "Detail extension information, serialized as JSON")
    private JsonExt detailExt;

    /**
     * Create time
     */
    @Schema(description = "Record creation timestamp")
    private LocalDateTime createTime;

}
