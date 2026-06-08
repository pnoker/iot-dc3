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

package io.github.pnoker.common.manager.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * View object for event attribute configuration API responses.
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
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(description = "Event Attribute Config view object")
public class EventAttributeConfigVO extends BaseVO {

    /**
     * ID
     */
    @Schema(description = "attribute ID")
    @NotNull(message = "Event attribute ID can't be empty", groups = {Add.class, Update.class})
    private Long attributeId;

    /**
     *
     */
    @Schema(description = "config value")
    @NotNull(message = "Event attribute config value can't be empty")
    private String configValue;

    /**
     * Device ID
     */
    @Schema(description = "device ID")
    @NotNull(message = "Device ID can't be empty", groups = {Add.class, Update.class})
    private Long deviceId;

    /**
     *
     */
    @Schema(description = "config extension information (JSON)")
    private JsonExt configExt;

    /**
     * Event ID
     */
    @Schema(description = "event ID")
    @NotNull(message = "Event ID can't be empty", groups = {Add.class, Update.class})
    private Long eventId;

    /**
     * Enable flag
     */
    @Schema(description = "Enable flag: 0=enabled, 1=disabled")
    private EnableFlagEnum enableFlag;

    /**
     *
     */
    @Schema(description = "signature")
    private String signature;

    /**
     *
     */
    @Schema(description = "Version number")
    private Integer version;

}
