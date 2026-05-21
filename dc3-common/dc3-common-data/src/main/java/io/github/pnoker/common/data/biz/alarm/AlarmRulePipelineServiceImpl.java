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

import lombok.RequiredArgsConstructor;
import io.github.pnoker.common.data.entity.bo.NotifyHistoryBO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Alarm rule processing pipeline implementation.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Service
@RequiredArgsConstructor
public class AlarmRulePipelineServiceImpl implements AlarmRulePipelineService {

    private final RuleEngine ruleEngine;

    private final RuleNotificationService ruleNotificationService;

    private final AlarmEventRecordService alarmEventRecordService;

    @Override
    public List<NotifyHistoryBO> process(RuleFact fact) {
        List<NotifyHistoryBO> histories = new ArrayList<>();
        for (RuleMatch match : ruleEngine.evaluate(fact)) {
            alarmEventRecordService.ensureEvent(match);
            histories.addAll(ruleNotificationService.notify(match));
        }
        return histories;
    }

}
