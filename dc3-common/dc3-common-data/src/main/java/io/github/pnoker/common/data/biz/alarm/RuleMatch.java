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

import io.github.pnoker.common.constant.service.AlarmConstant;
import io.github.pnoker.common.data.entity.bo.RuleBO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Result produced when a normalized fact satisfies a rule.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RuleMatch {

    private RuleBO rule;

    private RuleFact fact;

    private String matchType;

    private String severity;

    private String eventType;

    private List<String> labels;

    /**
     * Create a firing match.
     *
     * @param rule rule
     * @param fact fact
     * @return match
     */
    public static RuleMatch firing(RuleBO rule, RuleFact fact) {
        return of(rule, fact, AlarmConstant.MATCH_TYPE_FIRING);
    }

    /**
     * Create a recovery match.
     *
     * @param rule rule
     * @param fact fact
     * @return match
     */
    public static RuleMatch recovery(RuleBO rule, RuleFact fact) {
        return of(rule, fact, AlarmConstant.MATCH_TYPE_RECOVERY);
    }

    private static RuleMatch of(RuleBO rule, RuleFact fact, String matchType) {
        RuleMatch match = new RuleMatch();
        match.setRule(rule);
        match.setFact(fact);
        match.setMatchType(matchType);
        if (rule != null && rule.getRuleExt() != null && rule.getRuleExt().getContent() != null) {
            match.setSeverity(rule.getRuleExt().getContent().getSeverity());
            match.setEventType(rule.getRuleExt().getContent().getEventType());
            match.setLabels(rule.getRuleExt().getContent().getLabels());
        }
        return match;
    }

}
