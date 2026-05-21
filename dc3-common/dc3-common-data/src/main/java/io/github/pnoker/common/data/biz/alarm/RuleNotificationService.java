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

import io.github.pnoker.common.data.entity.bo.NotifyHistoryBO;

import java.util.List;

/**
 * Handles notification side effects for rule matches.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface RuleNotificationService {

    /**
     * Notify channels for one rule match.
     *
     * @param match rule match
     * @return persisted notify histories
     */
    List<NotifyHistoryBO> notify(RuleMatch match);

    /**
     * Batch notify channels for multiple rule matches in a single transaction.
     * Config lookups (notify / message / channel / bind) are amortized via
     * {@link NotifyConfigCache}, and rule_state transitions use atomic
     * database-level increments.
     *
     * @param matches rule matches
     * @return persisted notify histories
     */
    List<NotifyHistoryBO> notifyBatch(List<RuleMatch> matches);

}
