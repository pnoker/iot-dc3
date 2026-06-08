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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.constant.common.TimeConstant;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.entity.ext.RuleStateExt;
import io.github.pnoker.common.enums.AlarmTargetTypeFlagEnum;
import io.github.pnoker.common.enums.RuleStateFlagEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * View object for rule runtime state API responses.
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
@Schema(description = "Rule State view object")
public class RuleStateVO extends BaseVO {

    @Schema(description = "rule ID")

    private Long ruleId;

    @Schema(description = "alarm target type flag")

    private AlarmTargetTypeFlagEnum alarmTargetTypeFlag;

    @Schema(description = "Associated entity ID")

    private Long entityId;

    @Schema(description = "fingerprint")

    private String fingerprint;

    @Schema(description = "entity state flag")

    private RuleStateFlagEnum entityStateFlag;

    @Schema(description = "first trigger time")

    @JsonFormat(pattern = TimeConstant.COMPLETE_DATE_FORMAT, timezone = TimeConstant.DEFAULT_TIMEZONE)
    private LocalDateTime firstTriggerTime;

    @Schema(description = "last trigger time")

    @JsonFormat(pattern = TimeConstant.COMPLETE_DATE_FORMAT, timezone = TimeConstant.DEFAULT_TIMEZONE)
    private LocalDateTime lastTriggerTime;

    @Schema(description = "last recover time")

    @JsonFormat(pattern = TimeConstant.COMPLETE_DATE_FORMAT, timezone = TimeConstant.DEFAULT_TIMEZONE)
    private LocalDateTime lastRecoverTime;

    @Schema(description = "last notify time")

    @JsonFormat(pattern = TimeConstant.COMPLETE_DATE_FORMAT, timezone = TimeConstant.DEFAULT_TIMEZONE)
    private LocalDateTime lastNotifyTime;

    @Schema(description = "trigger count")

    private Long triggerCount;

    @Schema(description = "alarm ID")

    private Long alarmId;

    @Schema(description = "entity state extension information (JSON)")

    private RuleStateExt entityStateExt;

}
