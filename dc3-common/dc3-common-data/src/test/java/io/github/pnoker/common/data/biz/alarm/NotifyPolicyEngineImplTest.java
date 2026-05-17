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
import io.github.pnoker.common.data.entity.bo.NotifyBO;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBindBO;
import io.github.pnoker.common.data.entity.bo.RuleStateBO;
import io.github.pnoker.common.entity.ext.NotifyChannelBindExt;
import io.github.pnoker.common.entity.ext.NotifyExt;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotifyPolicyEngineImplTest {

    private final NotifyPolicyEngineImpl engine = new NotifyPolicyEngineImpl();

    @Test
    void sendsWhenPolicyAllowsSeverityAndRateLimit() {
        NotifyDecision decision = engine.decide(match("P1", AlarmConstant.MATCH_TYPE_FIRING), notifyPolicy(),
                bind(List.of("P0", "P1"), true, 300000L), new RuleStateBO(), LocalDateTime.now());

        assertThat(decision.isSend()).isTrue();
    }

    @Test
    void skipsSeverityNotAllowedByBinding() {
        NotifyDecision decision = engine.decide(match("P3", AlarmConstant.MATCH_TYPE_FIRING), notifyPolicy(),
                bind(List.of("P0", "P1"), true, 300000L), new RuleStateBO(), LocalDateTime.now());

        assertThat(decision.isSend()).isFalse();
        assertThat(decision.getReason()).contains("Severity");
    }

    @Test
    void skipsWhenRateLimited() {
        RuleStateBO state = new RuleStateBO();
        state.setLastNotifyTime(LocalDateTime.now());

        NotifyDecision decision = engine.decide(match("P1", AlarmConstant.MATCH_TYPE_FIRING), notifyPolicy(),
                bind(List.of("P0", "P1"), true, 300000L), state, LocalDateTime.now());

        assertThat(decision.isSend()).isFalse();
        assertThat(decision.getReason()).contains("rate-limited");
    }

    @Test
    void skipsRecoveryWhenBindingDisablesRecovery() {
        NotifyDecision decision = engine.decide(match("P1", AlarmConstant.MATCH_TYPE_RECOVERY), notifyPolicy(),
                bind(List.of("P0", "P1"), false, 300000L), new RuleStateBO(), LocalDateTime.now());

        assertThat(decision.isSend()).isFalse();
        assertThat(decision.getReason()).contains("Recovery");
    }

    private RuleMatch match(String severity, String matchType) {
        RuleMatch match = new RuleMatch();
        match.setSeverity(severity);
        match.setMatchType(matchType);
        return match;
    }

    private NotifyBO notifyPolicy() {
        NotifyExt ext = new NotifyExt();
        ext.setContent(new NotifyExt.Content(
                new NotifyExt.Dedup(true, "${tenantId}:${ruleCode}:${entityId}"),
                new NotifyExt.RateLimit(300000L, 1),
                new NotifyExt.Silence(false, List.of()),
                new NotifyExt.Repeat(false, 0L, 0),
                new NotifyExt.Recovery(true, true, false),
                List.of()));
        NotifyBO notify = new NotifyBO();
        notify.setNotifyInterval(300000L);
        notify.setNotifyExt(ext);
        return notify;
    }

    private NotifyChannelBindBO bind(List<String> levels, Boolean sendRecovery, Long rateLimitOverrideMs) {
        NotifyChannelBindExt ext = new NotifyChannelBindExt();
        ext.setContent(new NotifyChannelBindExt.Content(levels, sendRecovery, rateLimitOverrideMs));
        NotifyChannelBindBO bind = new NotifyChannelBindBO();
        bind.setBindExt(ext);
        return bind;
    }

}
