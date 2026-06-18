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

package io.github.pnoker.common.data.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.entity.ext.NotifyExt;
import io.github.pnoker.common.enums.AutoConfirmFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * View object for alarm notification template API responses.
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
@Schema(description = "Notify view object")
public class NotifyVO extends BaseVO {

    /**
     * Alarm notification template name
     */
    @Schema(description = "Notification template name; must be unique within the current tenant.", example = "Over Temperature Alert")
    private String notifyName;

    /**
     * Alarm notification template code
     */
    @Schema(description = "Stable business code identifying this notification template; should not change once deployed.", example = "OVER_TEMP")
    private String notifyCode;

    /**
     * Auto confirm flag
     */
    @Schema(description = "Whether alarms raised by this template are auto-confirmed (AUTO) or require manual confirmation (MANUAL).", example = "AUTO")
    private AutoConfirmFlagEnum autoConfirmFlag;

    /**
     * Alarm notification interval, milliseconds
     */
    @Schema(description = "Minimum cooldown interval, in seconds, between successive notification deliveries to prevent alert storms.", example = "300")
    private Long notifyInterval;

    /**
     * Alarm notification template configuration
     */
    @Schema(description = "Notification extension information, serialized as JSON for custom delivery logic (channels, templates, recipients).")
    private NotifyExt notifyExt;

    /**
     * Enable flag
     */
    @Schema(description = "Whether this notification template is active: ENABLE (0) to enable or DISABLE (1) to disable.", example = "ENABLE")
    private EnableFlagEnum enableFlag;

}
