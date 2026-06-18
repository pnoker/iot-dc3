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
    @Schema(description = "Unique identifier of this audit log entry.", example = "1024")
    private Long id;

    /**
     * Tenant ID
     */
    @Schema(description = "Identifier of the tenant this audit log entry belongs to; scopes the record to a single tenant.", example = "1024")
    private Long tenantId;

    /**
     * Principal ID
     */
    @Schema(description = "Identifier of the principal (user or service account) that performed the audited action; must belong to the same tenant.", example = "1024")
    private Long principalId;

    /**
     * Principal type
     */
    @Schema(description = "Type of the principal that performed the action, e.g. USER or SERVICE_ACCOUNT.", example = "USER")
    private String principalType;

    /**
     * Action
     */
    @Schema(description = "Audited action performed by the principal, e.g. LOGIN, LOGOUT, or PERMISSION_CHANGE.", example = "LOGIN")
    private String action;

    /**
     * Resource type
     */
    @Schema(description = "Type of the resource that was acted upon, e.g. DEVICE, DRIVER, or POINT.", example = "DEVICE")
    private String resourceType;

    /**
     * Resource ID
     */
    @Schema(description = "Identifier of the specific resource that was acted upon; must belong to the same tenant.", example = "1024")
    private Long resourceId;

    /**
     * Resource name
     */
    @Schema(description = "Human-readable name of the resource that was acted upon at the time of the audit event.", example = "temperature-sensor-01")
    private String resourceName;

    /**
     * Outcome status
     */
    @Schema(description = "Outcome status of the audited action, e.g. SUCCESS or FAILURE.", example = "SUCCESS")
    private String status;

    /**
     * Error code
     */
    @Schema(description = "Machine-readable error code when the action failed; absent on success.", example = "ERR_TIMEOUT")
    private String errorCode;

    /**
     * Detail extension, serialized as JSON
     */
    @Schema(description = "Additional structured details about the audit event, serialized as a JSON extension object.")
    private JsonExt detailExt;

    /**
     * Create time
     */
    @Schema(description = "Timestamp when this audit log entry was created.", example = "2026-06-18T08:00:00")
    private LocalDateTime createTime;

}
