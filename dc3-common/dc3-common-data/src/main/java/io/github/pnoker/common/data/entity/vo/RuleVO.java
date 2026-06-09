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
import io.github.pnoker.common.entity.ext.RuleExt;
import io.github.pnoker.common.enums.AlarmTargetTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * View object for alarm rule API responses.
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
@Schema(description = "Rule view object")
public class RuleVO extends BaseVO {

    /**
     * Alarm target type flag
     */
    @Schema(description = "alarm target type flag")
    private AlarmTargetTypeEnum alarmTargetTypeFlag;

    /**
     * Rule name
     */
    @Schema(description = "rule name")
    private String ruleName;

    /**
     * Rule code
     */
    @Schema(description = "rule code")
    private String ruleCode;

    /**
     * Entity ID
     */
    @Schema(description = "Associated entity ID")
    private Long entityId;

    /**
     * Alarm notification template ID
     */
    @Schema(description = "notify ID")
    private Long notifyId;

    /**
     * Alarm message template ID
     */
    @Schema(description = "message ID")
    private Long messageId;

    /**
     * Alarm rule
     */
    @Schema(description = "rule extension information (JSON)")
    private RuleExt ruleExt;

    /**
     * Enable flag
     */
    @Schema(description = "Enable flag: 0=enabled, 1=disabled")
    private EnableFlagEnum enableFlag;

}
