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

import io.github.pnoker.common.entity.ext.JsonExt;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Business object for identity and authorization audit log entries.
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
public class IdentityAuditLogBO {

    /**
     * Primary key ID
     */
    private Long id;

    /**
     * Tenant ID
     */
    private Long tenantId;

    /**
     * Principal ID
     */
    private Long principalId;

    /**
     * Principal type
     */
    private String principalType;

    /**
     * Action
     */
    private String action;

    /**
     * Resource type
     */
    private String resourceType;

    /**
     * Resource ID
     */
    private Long resourceId;

    /**
     * Resource name
     */
    private String resourceName;

    /**
     * Outcome status
     */
    private String status;

    /**
     * Error code
     */
    private String errorCode;

    /**
     * Detail extension, serialized as JSON
     */
    private JsonExt detailExt;

    /**
     * Create time
     */
    private LocalDateTime createTime;

}
