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
import io.github.pnoker.common.enums.AlarmTargetTypeEnum;
import io.github.pnoker.common.enums.RuleStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

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

    @Schema(description = "ID of the rule whose execution state is recorded.", example = "1024")

    private Long ruleId;

    @Schema(description = "Alarm target type enum", example = "DEVICE")

    private AlarmTargetTypeEnum alarmTargetTypeFlag;

    @Schema(description = "Associated entity ID", example = "2048")

    private Long entityId;

    @Schema(description = "Alarm fingerprint", example = "rule_HIGH_TEMP_ALERT_device_1024")

    private String fingerprint;

    @Schema(description = "Entity state enum", example = "TRIGGERED")

    private RuleStatusEnum entityStateFlag;

    @Schema(description = "Timestamp when the rule first triggered after activation.")

    @JsonFormat(pattern = TimeConstant.COMPLETE_DATE_FORMAT, timezone = TimeConstant.DEFAULT_TIMEZONE)
    private LocalDateTime firstTriggerTime;

    @Schema(description = "Timestamp of the most recent rule trigger.")

    @JsonFormat(pattern = TimeConstant.COMPLETE_DATE_FORMAT, timezone = TimeConstant.DEFAULT_TIMEZONE)
    private LocalDateTime lastTriggerTime;

    @Schema(description = "Timestamp when the rule last transitioned from triggered to recovered state.")

    @JsonFormat(pattern = TimeConstant.COMPLETE_DATE_FORMAT, timezone = TimeConstant.DEFAULT_TIMEZONE)
    private LocalDateTime lastRecoverTime;

    @Schema(description = "Timestamp when the last notification was dispatched for this rule.")

    @JsonFormat(pattern = TimeConstant.COMPLETE_DATE_FORMAT, timezone = TimeConstant.DEFAULT_TIMEZONE)
    private LocalDateTime lastNotifyTime;

    @Schema(description = "Cumulative count of times this rule has triggered since activation.", example = "42")

    private Long triggerCount;

    @Schema(description = "ID of the active alarm associated with this rule state.", example = "512")

    private Long alarmId;

    @Schema(description = "Entity state extension information, serialized as JSON for custom runtime metadata.")

    private RuleStateExt entityStateExt;

}
