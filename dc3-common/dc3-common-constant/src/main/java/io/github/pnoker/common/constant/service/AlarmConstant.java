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

package io.github.pnoker.common.constant.service;

import io.github.pnoker.common.constant.common.BaseConstant;

/**
 * Alarm service constants.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public class AlarmConstant {

    public static final String MATCH_TYPE_FIRING = "FIRING";

    public static final String MATCH_TYPE_RECOVERY = "RECOVERY";

    public static final String EXT_RULE_EVENT = "ALARM_RULE_EVENT";

    public static final String EXT_RULE_STATE = "ALARM_RULE_STATE";

    public static final String EXT_NOTIFY_HISTORY_REQUEST = "ALARM_NOTIFY_HISTORY_REQUEST";

    public static final String EXT_NOTIFY_HISTORY_RESPONSE = "ALARM_NOTIFY_HISTORY_RESPONSE";

    /**
     * The single rule-evaluation window mode supported in the current release.
     * Other modes (AVG/MIN/MAX/SUM/COUNT/ALL/ANY) are reserved for the upcoming
     * window-aggregation phase; configurations selecting them are rejected at
     * save time and skipped at evaluation time.
     */
    public static final String WINDOW_MODE_LAST = "LAST";

    private AlarmConstant() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

}
