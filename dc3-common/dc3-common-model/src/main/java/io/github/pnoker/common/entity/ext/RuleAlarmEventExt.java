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

import io.github.pnoker.common.enums.AlarmTargetTypeFlagEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Rule alarm event Ext.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RuleAlarmEventExt extends BaseExt {

    /**
     * Extended content.
     */
    private Content content;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {

        /**
         * Rule ID at the time the event was created.
         */
        private Long ruleId;

        /**
         * Rule code at the time the event was created.
         */
        private String ruleCode;

        /**
         * Rule name at the time the event was created.
         */
        private String ruleName;

        /**
         * Target type evaluated by the rule.
         */
        private AlarmTargetTypeFlagEnum targetType;

        /**
         * Target entity ID evaluated by the rule.
         */
        private Long entityId;

        /**
         * Alarm severity at the time the event was created.
         */
        private String severity;

        /**
         * Business event type produced by the rule.
         */
        private String eventType;

        /**
         * Runtime match type, for example FIRING or RECOVERY.
         */
        private String matchType;

        /**
         * Normalized fact values used by the rule match.
         */
        private Map<String, Object> values;

    }

}
