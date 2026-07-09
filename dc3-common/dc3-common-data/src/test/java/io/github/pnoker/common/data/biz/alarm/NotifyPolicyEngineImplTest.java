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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

    // ---------- silence windows (the highest-risk, previously-uncovered logic) ----------
    // inWindow() handles three shapes: start==end (all-day), start<end (same-day),
    // start>end (cross-midnight), plus timezone conversion and day-of-week matching.
    // All times below are in the system default zone, since decide()'s `now` is treated
    // as a LocalDateTime in the system zone and converted to the window's zone.

    @ParameterizedTest(name = "[{0}] now={1} window={2}-{3} => silenced={4}")
    @CsvSource({
            // same-day window 09:00-17:00
            "same-day inside,    2026-07-09T12:00, 09:00, 17:00, true",
            "same-day at start,  2026-07-09T09:00, 09:00, 17:00, true",
            "same-day before,    2026-07-09T08:59, 09:00, 17:00, false",
            "same-day at end,    2026-07-09T17:00, 09:00, 17:00, false",
            "same-day after,     2026-07-09T18:00, 09:00, 17:00, false",
            // cross-midnight window 22:00-06:00
            "midnight late eve,  2026-07-09T23:00, 22:00, 06:00, true",
            "midnight at start,  2026-07-09T22:00, 22:00, 06:00, true",
            "midnight early am,  2026-07-09T03:00, 22:00, 06:00, true",
            "midnight at end,    2026-07-09T06:00, 22:00, 06:00, false",
            "midnight midday,    2026-07-09T12:00, 22:00, 06:00, false",
            // start==end means the whole day is silenced
            "all-day window,     2026-07-09T00:00, 00:00, 00:00, true",
            "all-day noon,       2026-07-09T12:00, 12:00, 12:00, true"
    })
    void silenceWindowBoundaryMatrix(String label, LocalDateTime now, String start, String end, boolean silenced) {
        NotifyBO notify = notifyWithSilenceWindow(null, start, end, null);
        NotifyDecision decision = engine.decide(match("P1", AlarmConstant.MATCH_TYPE_FIRING), notify,
                bind(null, true, null), new RuleStateBO(), now);

        assertThat(decision.isSend()).isEqualTo(!silenced);
        if (silenced) {
            assertThat(decision.getReason()).contains("silenced");
        }
    }

    @Test
    void silenceIsNotAppliedWhenDisabled() {
        // An enabled=false silence must never suppress, even if a window matches.
        NotifyExt ext = new NotifyExt();
        ext.setContent(new NotifyExt.Content(
                new NotifyExt.Dedup(true, "k"),
                new NotifyExt.RateLimit(300000L, 1),
                new NotifyExt.Silence(false, List.of(new NotifyExt.Window(null, "00:00", "23:59", null))),
                new NotifyExt.Repeat(false, 0L, 0),
                new NotifyExt.Recovery(true, true, false),
                List.of()));
        NotifyBO notify = new NotifyBO();
        notify.setNotifyInterval(300000L);
        notify.setNotifyExt(ext);

        NotifyDecision decision = engine.decide(match("P1", AlarmConstant.MATCH_TYPE_FIRING), notify,
                bind(null, true, null), new RuleStateBO(), LocalDateTime.of(2026, 7, 9, 12, 0));

        assertThat(decision.isSend()).isTrue();
    }

    @Test
    void silenceWindowRespectsDaysOfWeek() {
        // 2026-07-09 is a Thursday; window allowed only on Monday => not silenced.
        NotifyBO notify = notifyWithSilenceWindow(null, "00:00", "23:59", List.of("MONDAY"));
        NotifyDecision decisionThu = engine.decide(match("P1", AlarmConstant.MATCH_TYPE_FIRING), notify,
                bind(null, true, null), new RuleStateBO(), LocalDateTime.of(2026, 7, 9, 12, 0));
        assertThat(decisionThu.isSend()).isTrue();

        // 2026-07-06 is the Monday of that week => silenced.
        NotifyDecision decisionMon = engine.decide(match("P1", AlarmConstant.MATCH_TYPE_FIRING), notify,
                bind(null, true, null), new RuleStateBO(), LocalDateTime.of(2026, 7, 6, 12, 0));
        assertThat(decisionMon.isSend()).isFalse();
    }

    @Test
    void silenceWindowAcceptsShortDayNames() {
        // dayAllowed matches both the full enum name and the SHORT display name ("Mon").
        // 2026-07-06 is Monday.
        NotifyBO notify = notifyWithSilenceWindow(null, "00:00", "23:59", List.of("Mon"));
        NotifyDecision decision = engine.decide(match("P1", AlarmConstant.MATCH_TYPE_FIRING), notify,
                bind(null, true, null), new RuleStateBO(), LocalDateTime.of(2026, 7, 6, 12, 0));

        assertThat(decision.isSend()).isFalse();
    }

    @Test
    void silenceWindowWithMalformedTimesNeverSilences() {
        // A DateTimeException during LocalTime.parse must fail-open (not silence) rather
        // than crash the pipeline.
        NotifyBO notify = notifyWithSilenceWindow(null, "not-a-time", "25:99", null);
        NotifyDecision decision = engine.decide(match("P1", AlarmConstant.MATCH_TYPE_FIRING), notify,
                bind(null, true, null), new RuleStateBO(), LocalDateTime.of(2026, 7, 9, 12, 0));

        assertThat(decision.isSend()).isTrue();
    }

    @ParameterizedTest(name = "bind.rateLimitOverrideMs={0}, notify.rateLimit.intervalMs={1}, notifyInterval={2} => intervalMs={3}")
    @CsvSource({
            // bind override wins over both notify-level and the entity default
            "300000, 600000, 900000, 300000",
            // no bind override -> notify.rateLimit wins over entity default
            ", 600000, 900000, 600000",
            // no bind override, no notify.rateLimit -> falls back to entity default
            ", , 900000, 900000"
    })
    void rateLimitIntervalFallsBackInPriorityOrder(Long bindOverride, Long notifyIntervalMs, Long entityInterval,
                                                   Long expectedInterval) {
        RuleStateBO state = new RuleStateBO();
        // last notify exactly one interval ago => boundary case at the edge of allowed
        LocalDateTime lastNotify = LocalDateTime.of(2026, 7, 9, 12, 0);
        LocalDateTime now = lastNotify.plusNanos(expectedInterval * 1_000_000L);
        state.setLastNotifyTime(lastNotify);

        NotifyExt ext = new NotifyExt();
        ext.setContent(new NotifyExt.Content(
                new NotifyExt.Dedup(true, "k"),
                notifyIntervalMs != null ? new NotifyExt.RateLimit(notifyIntervalMs, 1) : null,
                new NotifyExt.Silence(false, List.of()),
                new NotifyExt.Repeat(false, 0L, 0),
                new NotifyExt.Recovery(true, true, false),
                List.of()));
        NotifyBO notify = new NotifyBO();
        notify.setNotifyInterval(entityInterval);
        notify.setNotifyExt(ext);

        NotifyDecision decision = engine.decide(match("P1", AlarmConstant.MATCH_TYPE_FIRING), notify,
                bind(null, true, bindOverride), state, now);

        // at exactly one interval after last notify, the boundary is allowed (>=), so it sends
        assertThat(decision.isSend()).isTrue();
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

    /**
     * Builds a NotifyBO with a single ENABLED silence window. The window's timezone defaults
     * to the system zone (null), so the supplied start/end are interpreted in local time.
     */
    private NotifyBO notifyWithSilenceWindow(String timezone, String start, String end, List<String> daysOfWeek) {
        NotifyExt ext = new NotifyExt();
        ext.setContent(new NotifyExt.Content(
                new NotifyExt.Dedup(true, "k"),
                new NotifyExt.RateLimit(300000L, 1),
                new NotifyExt.Silence(true, List.of(new NotifyExt.Window(timezone, start, end, daysOfWeek))),
                new NotifyExt.Repeat(false, 0L, 0),
                new NotifyExt.Recovery(true, true, false),
                List.of()));
        NotifyBO notify = new NotifyBO();
        notify.setNotifyInterval(300000L);
        notify.setNotifyExt(ext);
        return notify;
    }

}
