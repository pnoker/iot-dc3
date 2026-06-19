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
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.MembershipStatusEnum;
import io.github.pnoker.common.enums.PrincipalTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * View object for tenant memberships.
 *
 * @author pnoker
 * @version 2026.6.13
 * @since 2026.6.13
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(description = "Tenant membership view object", example = "1024")
public class TenantMembershipVO extends BaseVO {

    @Schema(description = "Identifier of the tenant this membership belongs to.", example = "1024")
    private Long tenantId;

    @Schema(description = "Identifier of the principal (user or service account) attached to the tenant.", example = "2048")
    private Long principalId;

    @Schema(description = "Classification of the principal: USER, SERVICE_ACCOUNT, or SYSTEM.", example = "USER")
    private PrincipalTypeEnum principalType;

    @Schema(description = "Lifecycle status of the membership: ACTIVE, SUSPENDED, or INVITED.", example = "ACTIVE")
    private MembershipStatusEnum membershipStatus;

    @Schema(description = "Timestamp when the principal joined (was attached to) the tenant.")
    private LocalDateTime joinedTime;

    @Schema(description = "Extended JSON metadata for this membership (e.g. custom roles, invitation context); structure is tenant-defined.")
    private JsonExt membershipExt;
}
