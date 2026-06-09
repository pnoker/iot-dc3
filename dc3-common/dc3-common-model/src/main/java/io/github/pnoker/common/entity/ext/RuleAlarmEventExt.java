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

package io.github.pnoker.common.entity.ext;

import io.github.pnoker.common.enums.AlarmTargetTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * JSON extension object for rule alarm event context.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "JSON extension object that carries the rule alarm event context, embedded inside VO extension fields")
public class RuleAlarmEventExt extends BaseExt {

    /**
     * Extended content.
     */
    @Schema(description = "Extended content holding the rule alarm event snapshot")
    private Content content;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Snapshot of the rule alarm event context captured when the event was created")
    public static class Content {

        /**
         * Rule ID at the time the event was created.
         */
        @Schema(description = "Rule ID at the time the event was created", example = "1024")
        private Long ruleId;

        /**
         * Rule code at the time the event was created.
         */
        @Schema(description = "Rule code at the time the event was created")
        private String ruleCode;

        /**
         * Rule name at the time the event was created.
         */
        @Schema(description = "Rule name at the time the event was created")
        private String ruleName;

        /**
         * Target type evaluated by the rule.
         */
        @Schema(description = "Target type evaluated by the rule", example = "POINT")
        private AlarmTargetTypeEnum targetType;

        /**
         * Target entity ID evaluated by the rule.
         */
        @Schema(description = "Target entity ID evaluated by the rule", example = "1024")
        private Long entityId;

        /**
         * Alarm severity at the time the event was created.
         */
        @Schema(description = "Alarm severity at the time the event was created")
        private String severity;

        /**
         * Business event type produced by the rule.
         */
        @Schema(description = "Business event type produced by the rule")
        private String eventType;

        /**
         * Runtime match type, for example FIRING or RECOVERY.
         */
        @Schema(description = "Runtime match type, for example FIRING or RECOVERY")
        private String matchType;

        /**
         * Normalized fact values used by the rule match.
         */
        @Schema(description = "Normalized fact values used by the rule match")
        private Map<String, Object> values;

    }

}
