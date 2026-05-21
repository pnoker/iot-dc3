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
 * Alarm rule processing pipeline.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface AlarmRulePipelineService {

    /**
     * Evaluate a fact and execute notification side effects for matched rules.
     *
     * @param fact normalized fact
     * @return persisted notification records
     */
    List<NotifyHistoryBO> process(RuleFact fact);

    /**
     * Batch-evaluate facts and execute notification side effects for all
     * matches in a single transaction. Facts are grouped internally by
     * {@code (tenantId, alarmTargetTypeFlag, entityId)} so {@link RuleRegistry}
     * lookups are amortized, and side-effect writes (rule_state, entity_alarm,
     * notify_history) are collected into bulk operations.
     *
     * @param facts normalized facts
     * @return persisted notification records
     */
    List<NotifyHistoryBO> processBatch(List<RuleFact> facts);

}
