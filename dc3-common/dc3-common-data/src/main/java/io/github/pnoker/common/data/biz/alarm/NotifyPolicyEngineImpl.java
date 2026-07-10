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
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Deterministic notification policy engine.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Service
public class NotifyPolicyEngineImpl implements NotifyPolicyEngine {

    @Override
    public NotifyDecision decide(RuleMatch match, NotifyBO notify, NotifyChannelBindBO bind, RuleStateBO state,
                                 LocalDateTime now) {
        if (Objects.isNull(match) || Objects.isNull(notify) || Objects.isNull(bind)) {
            return NotifyDecision.skip("Notification context is incomplete");
        }
        if (isRecovery(match) && !recoveryEnabled(notify, bind)) {
            return NotifyDecision.skip("Recovery notification is disabled");
        }
        if (!levelAllowed(match, bind)) {
            return NotifyDecision.skip("Severity is not allowed by channel binding");
        }
        if (isSilenced(notify, now)) {
            return NotifyDecision.skip("Notification is silenced by policy");
        }
        if (!rateLimitAllowed(notify, bind, state, now)) {
            return NotifyDecision.skip("Notification is rate-limited");
        }
        return NotifyDecision.send();
    }

    private boolean isRecovery(RuleMatch match) {
        return StringUtils.equalsIgnoreCase(match.getMatchType(), AlarmConstant.MATCH_TYPE_RECOVERY);
    }

    private boolean recoveryEnabled(NotifyBO notify, NotifyChannelBindBO bind) {
        NotifyExt.Content notifyContent = content(notify);
        boolean policyEnabled = Objects.nonNull(notifyContent)
                && Objects.nonNull(notifyContent.getRecovery())
                && Boolean.TRUE.equals(notifyContent.getRecovery().getEnabled())
                && Boolean.TRUE.equals(notifyContent.getRecovery().getSendRecoveryMessage());
        NotifyChannelBindExt.Content bindContent = bindContent(bind);
        boolean bindEnabled = Objects.isNull(bindContent) || !Boolean.FALSE.equals(bindContent.getSendRecovery());
        return policyEnabled && bindEnabled;
    }

    /**
     * Compares the rule-derived severity against the channel binding's allowed
     * levels. Severity is sourced from {@code rule_ext.severity} (which is
     * persisted onto {@code dc3_entity_alarm.alarm_level_flag} when the rule
     * fires); {@code dc3_message.message_level} is intentionally not consulted
     * here because that column is for template-rendering defaults only —
     * routing decisions belong on the rule, not on the message body.
     */
    private boolean levelAllowed(RuleMatch match, NotifyChannelBindBO bind) {
        NotifyChannelBindExt.Content bindContent = bindContent(bind);
        if (Objects.isNull(bindContent) || Objects.isNull(bindContent.getLevels()) || bindContent.getLevels().isEmpty()) {
            return true;
        }
        for (String level : bindContent.getLevels()) {
            if (StringUtils.equalsIgnoreCase(level, match.getSeverity())) {
                return true;
            }
        }
        return false;
    }

    private boolean rateLimitAllowed(NotifyBO notify, NotifyChannelBindBO bind, RuleStateBO state, LocalDateTime now) {
        if (Objects.isNull(state) || Objects.isNull(state.getLastNotifyTime())) {
            return true;
        }

        Long intervalMs = intervalMs(notify, bind);
        if (Objects.isNull(intervalMs) || intervalMs <= 0) {
            return true;
        }
        LocalDateTime nextAllowedTime = state.getLastNotifyTime().plusNanos(intervalMs * 1_000_000L);
        return !now.isBefore(nextAllowedTime);
    }

    private Long intervalMs(NotifyBO notify, NotifyChannelBindBO bind) {
        NotifyChannelBindExt.Content bindContent = bindContent(bind);
        if (Objects.nonNull(bindContent) && Objects.nonNull(bindContent.getRateLimitOverrideMs())) {
            return bindContent.getRateLimitOverrideMs();
        }
        NotifyExt.Content notifyContent = content(notify);
        if (Objects.nonNull(notifyContent) && Objects.nonNull(notifyContent.getRateLimit())
                && Objects.nonNull(notifyContent.getRateLimit().getIntervalMs())) {
            return notifyContent.getRateLimit().getIntervalMs();
        }
        return notify.getNotifyInterval();
    }

    private boolean isSilenced(NotifyBO notify, LocalDateTime now) {
        NotifyExt.Content notifyContent = content(notify);
        if (Objects.isNull(notifyContent) || Objects.isNull(notifyContent.getSilence())
                || !Boolean.TRUE.equals(notifyContent.getSilence().getEnabled())
                || Objects.isNull(notifyContent.getSilence().getWindows())) {
            return false;
        }
        for (NotifyExt.Window window : notifyContent.getSilence().getWindows()) {
            if (inWindow(window, now)) {
                return true;
            }
        }
        return false;
    }

    private boolean inWindow(NotifyExt.Window window, LocalDateTime now) {
        if (Objects.isNull(window) || StringUtils.isBlank(window.getStart()) || StringUtils.isBlank(window.getEnd())) {
            return false;
        }
        try {
            ZoneId zoneId = StringUtils.isNotBlank(window.getTimezone())
                    ? ZoneId.of(window.getTimezone())
                    : ZoneId.systemDefault();
            ZonedDateTime zonedNow = now.atZone(ZoneId.systemDefault()).withZoneSameInstant(zoneId);
            if (!dayAllowed(window.getDaysOfWeek(), zonedNow.getDayOfWeek())) {
                return false;
            }

            LocalTime start = LocalTime.parse(window.getStart());
            LocalTime end = LocalTime.parse(window.getEnd());
            LocalTime time = zonedNow.toLocalTime();
            if (start.equals(end)) {
                return true;
            }
            if (start.isBefore(end)) {
                return !time.isBefore(start) && time.isBefore(end);
            }
            return !time.isBefore(start) || time.isBefore(end);
        } catch (DateTimeException ignored) {
            return false;
        }
    }

    private boolean dayAllowed(List<String> daysOfWeek, DayOfWeek current) {
        if (Objects.isNull(daysOfWeek) || daysOfWeek.isEmpty()) {
            return true;
        }
        for (String day : daysOfWeek) {
            if (StringUtils.equalsIgnoreCase(day, current.name())
                    || StringUtils.equalsIgnoreCase(day, current.getDisplayName(java.time.format.TextStyle.SHORT,
                    Locale.ENGLISH))) {
                return true;
            }
        }
        return false;
    }

    private NotifyExt.Content content(NotifyBO notify) {
        if (Objects.isNull(notify) || Objects.isNull(notify.getNotifyExt())) {
            return null;
        }
        return notify.getNotifyExt().getContent();
    }

    private NotifyChannelBindExt.Content bindContent(NotifyChannelBindBO bind) {
        if (Objects.isNull(bind) || Objects.isNull(bind.getBindExt())) {
            return null;
        }
        return bind.getBindExt().getContent();
    }

}
