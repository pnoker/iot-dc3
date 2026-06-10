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

package io.github.pnoker.common.facade.entity.bo;

import io.github.pnoker.common.entity.base.BaseBO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Facade-level user BO. Field set matches {@code api.center.auth.UserApi} wire shape —
 * {@code enableFlag} is intentionally absent because the {@code GrpcUserDTO} contract
 * does not expose it.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@Schema(description = "Facade User business object")
public class FacadeUserBO extends BaseBO {

    @Schema(description = "User nickname")

    private String nickName;

    @Schema(description = "Username")

    private String userName;

    @Schema(description = "Phone number")

    private String phone;

    @Schema(description = "Email address")

    private String email;

    @Schema(description = "Social extension information in JSON format")

    private String socialExt;

    @Schema(description = "Identity extension information in JSON format")

    private String identityExt;

}
