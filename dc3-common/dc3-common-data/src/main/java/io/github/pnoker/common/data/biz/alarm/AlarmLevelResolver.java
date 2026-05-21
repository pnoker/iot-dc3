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

package io.github.pnoker.common.data.biz.alarm;

import io.github.pnoker.common.constant.common.BaseConstant;
import io.github.pnoker.common.enums.AlarmMessageLevelFlagEnum;
import org.apache.commons.lang3.StringUtils;

/**
 * Resolves the free-form {@code RuleExt.Content.severity} string ("P0".."P3")
 * into a typed {@link AlarmMessageLevelFlagEnum}, falling back gracefully when
 * the value is missing or unrecognized.
 *
 * <p>The {@code dc3_entity_alarm.alarm_level_flag} column is a {@code SMALLINT}
 * with the same P0..P3 semantics as {@link AlarmMessageLevelFlagEnum}, so the
 * enum index can be persisted directly. {@code dc3_message.message_level}
 * remains a passive payload column for template rendering — notification
 * routing reads severity from the resolved alarm level, not from the message.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.21
 */
public final class AlarmLevelResolver {

    private AlarmLevelResolver() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

    /**
     * Returns the {@link AlarmMessageLevelFlagEnum} matching {@code severity},
     * accepting both the upper-case name ("P1") and the lower-case code ("p1").
     * Returns {@code fallback} when {@code severity} is blank or unrecognized.
     */
    public static AlarmMessageLevelFlagEnum resolve(String severity, AlarmMessageLevelFlagEnum fallback) {
        if (StringUtils.isBlank(severity)) {
            return fallback;
        }
        AlarmMessageLevelFlagEnum byName = AlarmMessageLevelFlagEnum.ofName(severity.trim().toUpperCase());
        if (byName != null) {
            return byName;
        }
        AlarmMessageLevelFlagEnum byCode = AlarmMessageLevelFlagEnum.ofCode(severity.trim().toLowerCase());
        return byCode != null ? byCode : fallback;
    }

}
