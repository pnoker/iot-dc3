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

/**
 * Persists rule alarm rows for rule matches so rule alarms are visible in the
 * existing event overview and can be linked from notification records.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface RuleAlarmPersistenceService {

    /**
     * Ensure the match has an alarm id. Existing alarm ids are reused; new rows are
     * created only for firing matches that do not already reference a runtime alarm.
     *
     * @param match rule match
     */
    void ensureAlarm(RuleMatch match);

}
