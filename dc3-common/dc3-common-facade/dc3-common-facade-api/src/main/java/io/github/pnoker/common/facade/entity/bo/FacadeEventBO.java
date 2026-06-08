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
import io.github.pnoker.common.entity.ext.EventExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EventLevelFlagEnum;
import io.github.pnoker.common.enums.EventTypeFlagEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Facade-level event BO.
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
@Schema(description = "Facade Event business object")
public class FacadeEventBO extends BaseBO {

    @Schema(description = "event name")

    private String eventName;

    @Schema(description = "event code")

    private String eventCode;

    @Schema(description = "event type flag")

    private EventTypeFlagEnum eventTypeFlag;

    @Schema(description = "event level flag")

    private EventLevelFlagEnum eventLevelFlag;

    @Schema(description = "event extension information (JSON)")

    private EventExt eventExt;

    @Schema(description = "profile ID")

    private Long profileId;

    @Schema(description = "Enable flag: 0=enabled, 1=disabled")

    private EnableFlagEnum enableFlag;

    @Schema(description = "Tenant ID")

    private Long tenantId;

    @Schema(description = "signature")

    private String signature;

    @Schema(description = "Version number")

    private Integer version;

}
