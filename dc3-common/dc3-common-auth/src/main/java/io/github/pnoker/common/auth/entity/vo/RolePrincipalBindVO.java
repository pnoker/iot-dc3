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
import io.github.pnoker.common.enums.PrincipalTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * View object for role-principal bindings.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(description = "Role principal bind view object")
public class RolePrincipalBindVO extends BaseVO {

    @Schema(description = "Identifier of the tenant this binding belongs to; all related role and principal must share the same tenant.", example = "1024")
    private Long tenantId;

    @Schema(description = "Identifier of the role being bound to the principal; must exist within the current tenant.", example = "2048")
    private Long roleId;

    @Schema(description = "Identifier of the principal (user, service account, or system identity) bound to the role; must belong to the current tenant.", example = "3072")
    private Long principalId;

    @Schema(description = "Type of principal bound to the role, indicating whether it is a human user, service account, or internal system identity.", example = "USER")
    private PrincipalTypeEnum principalType;

}
