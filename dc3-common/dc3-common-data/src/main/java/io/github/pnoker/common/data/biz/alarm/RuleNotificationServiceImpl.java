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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.service.AlarmConstant;
import io.github.pnoker.common.data.dal.MessageManager;
import io.github.pnoker.common.data.dal.NotifyChannelBindManager;
import io.github.pnoker.common.data.dal.NotifyChannelManager;
import io.github.pnoker.common.data.dal.NotifyManager;
import io.github.pnoker.common.data.dal.NotifyHistoryManager;
import io.github.pnoker.common.data.dal.RuleStateManager;
import io.github.pnoker.common.data.entity.bo.MessageBO;
import io.github.pnoker.common.data.entity.bo.NotifyBO;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBO;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBindBO;
import io.github.pnoker.common.data.entity.bo.NotifyHistoryBO;
import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.data.entity.bo.RuleStateBO;
import io.github.pnoker.common.data.entity.builder.MessageBuilder;
import io.github.pnoker.common.data.entity.builder.NotifyBuilder;
import io.github.pnoker.common.data.entity.builder.NotifyChannelBindBuilder;
import io.github.pnoker.common.data.entity.builder.NotifyChannelBuilder;
import io.github.pnoker.common.data.entity.builder.NotifyHistoryBuilder;
import io.github.pnoker.common.data.entity.builder.RuleStateBuilder;
import io.github.pnoker.common.data.entity.model.MessageDO;
import io.github.pnoker.common.data.entity.model.NotifyChannelBindDO;
import io.github.pnoker.common.data.entity.model.NotifyChannelDO;
import io.github.pnoker.common.data.entity.model.NotifyDO;
import io.github.pnoker.common.data.entity.model.NotifyHistoryDO;
import io.github.pnoker.common.data.entity.model.RuleStateDO;
import io.github.pnoker.common.entity.ext.NotifyExt;
import io.github.pnoker.common.entity.ext.NotifyHistoryRequestExt;
import io.github.pnoker.common.entity.ext.NotifyHistoryResponseExt;
import io.github.pnoker.common.entity.ext.RuleStateExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.NotifyHistoryStatusEnum;
import io.github.pnoker.common.enums.RuleStateFlagEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Rule notification service implementation.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleNotificationServiceImpl implements RuleNotificationService {

    private final NotifyManager notifyManager;

    private final NotifyBuilder notifyBuilder;

    private final MessageManager messageManager;

    private final MessageBuilder messageBuilder;

    private final NotifyChannelBindManager notifyChannelBindManager;

    private final NotifyChannelBindBuilder notifyChannelBindBuilder;

    private final NotifyChannelManager notifyChannelManager;

    private final NotifyChannelBuilder notifyChannelBuilder;

    private final RuleStateManager ruleStateManager;

    private final RuleStateBuilder ruleStateBuilder;

    private final NotifyHistoryManager notifyHistoryManager;

    private final NotifyHistoryBuilder notifyHistoryBuilder;

    private final NotifyPolicyEngine notifyPolicyEngine;

    private final MessageRenderService messageRenderService;

    private final NotifyChannelAdapterRegistry notifyChannelAdapterRegistry;

    private final AlarmTemplateRenderer alarmTemplateRenderer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<NotifyHistoryBO> notify(RuleMatch match) {
        if (Objects.isNull(match) || Objects.isNull(match.getRule()) || Objects.isNull(match.getFact())) {
            return List.of();
        }

        RuleBO rule = match.getRule();
        Map<String, Object> variables = RuleMatchVariables.of(match);
        NotifyBO notify = loadNotify(rule.getNotifyId());
        RuleStateBO state = persistRuleState(match, notify, variables);
        if (Objects.isNull(notify)) {
            log.warn("Skip alarm notification because notify policy does not exist, ruleId={}", rule.getId());
            return List.of();
        }

        MessageBO message = loadMessage(rule.getMessageId());
        List<NotifyChannelBindBO> binds = loadEnabledBinds(notify);
        List<NotifyHistoryBO> histories = new ArrayList<>();
        for (NotifyChannelBindBO bind : binds) {
            NotifyChannelBO channel = loadChannel(bind.getChannelId(), bind.getTenantId());
            if (Objects.isNull(channel)) {
                log.warn("Skip alarm notification because notify channel does not exist, channelId={}",
                        bind.getChannelId());
                continue;
            }
            if (!EnableFlagEnum.ENABLE.equals(channel.getEnableFlag())) {
                histories.add(historySkipped(match, notify, message, bind, channel, variables,
                        "Notify channel is disabled"));
                continue;
            }
            if (Objects.isNull(message)) {
                histories.add(historySkipped(match, notify, null, bind, channel, variables,
                        "Message template does not exist"));
                continue;
            }

            NotifyDecision decision = notifyPolicyEngine.decide(match, notify, bind, state, LocalDateTime.now());
            if (!decision.isSend()) {
                histories.add(historySkipped(match, notify, message, bind, channel, variables, decision.getReason()));
                continue;
            }

            MessagePayload payload = messageRenderService.render(message, channel.getChannelTypeFlag(), variables);
            NotifySendResult result = notifyChannelAdapterRegistry.find(channel.getChannelTypeFlag())
                    .map(adapter -> adapter.send(channel, payload))
                    .orElseGet(() -> NotifySendResult.failed(channel.getCredentialRef(), "Notify channel adapter is missing"));
            NotifyHistoryBO history = persistHistory(match, notify, message, bind, channel, payload, variables, result);
            histories.add(history);
            if (NotifyHistoryStatusEnum.SUCCESS.equals(result.getStatusFlag())) {
                state.setLastNotifyTime(LocalDateTime.now());
                persistState(state);
            }
        }
        return histories;
    }

    private NotifyBO loadNotify(Long notifyId) {
        if (Objects.isNull(notifyId) || DefaultConstant.DEFAULT_ID == notifyId) {
            return null;
        }
        NotifyDO entityDO = notifyManager.getById(notifyId);
        return Objects.nonNull(entityDO) ? notifyBuilder.buildBOByDO(entityDO) : null;
    }

    private MessageBO loadMessage(Long messageId) {
        if (Objects.isNull(messageId) || DefaultConstant.DEFAULT_ID == messageId) {
            return null;
        }
        MessageDO entityDO = messageManager.getById(messageId);
        return Objects.nonNull(entityDO) ? messageBuilder.buildBOByDO(entityDO) : null;
    }

    private List<NotifyChannelBindBO> loadEnabledBinds(NotifyBO notify) {
        List<NotifyChannelBindDO> list = notifyChannelBindManager.lambdaQuery()
                .eq(NotifyChannelBindDO::getNotifyId, notify.getId())
                .eq(NotifyChannelBindDO::getTenantId, notify.getTenantId())
                .eq(NotifyChannelBindDO::getEnableFlag, EnableFlagEnum.ENABLE.getIndex())
                .list();
        return notifyChannelBindBuilder.buildBOListByDOList(list);
    }

    private NotifyChannelBO loadChannel(Long channelId, Long tenantId) {
        NotifyChannelDO entityDO = notifyChannelManager.getById(channelId);
        if (Objects.isNull(entityDO) || !Objects.equals(entityDO.getTenantId(), tenantId)
                || Objects.isNull(entityDO.getChannelTypeFlag())) {
            return null;
        }
        return notifyChannelBuilder.buildBOByDO(entityDO);
    }

    private RuleStateBO persistRuleState(RuleMatch match, NotifyBO notify, Map<String, Object> variables) {
        RuleBO rule = match.getRule();
        RuleFact fact = match.getFact();
        String fingerprint = fingerprint(match, notify, variables);
        RuleStateBO state = loadState(rule, fact, fingerprint);
        LocalDateTime now = LocalDateTime.now();
        if (Objects.isNull(state)) {
            state = new RuleStateBO();
            state.setRuleId(rule.getId());
            state.setAlarmTargetTypeFlag(rule.getAlarmTargetTypeFlag());
            state.setEntityId(fact.getEntityId());
            state.setFingerprint(fingerprint);
            state.setFirstTriggerTime(now);
            state.setTriggerCount(0L);
            state.setTenantId(fact.getTenantId());
        }
        state.setEventId(Objects.requireNonNullElse(fact.getEventId(), DefaultConstant.DEFAULT_ID));
        state.setStateExt(ruleStateExt(match));

        if (StringUtils.equalsIgnoreCase(match.getMatchType(), AlarmConstant.MATCH_TYPE_RECOVERY)) {
            state.setStateFlag(RuleStateFlagEnum.RECOVERED);
            state.setLastRecoverTime(now);
        } else {
            state.setStateFlag(RuleStateFlagEnum.FIRING);
            state.setLastTriggerTime(now);
            state.setTriggerCount(Objects.requireNonNullElse(state.getTriggerCount(), 0L) + 1);
            if (Objects.isNull(state.getFirstTriggerTime())) {
                state.setFirstTriggerTime(now);
            }
        }
        return persistState(state);
    }

    private RuleStateBO loadState(RuleBO rule, RuleFact fact, String fingerprint) {
        LambdaQueryWrapper<RuleStateDO> wrapper = Wrappers.<RuleStateDO>query().lambda()
                .eq(RuleStateDO::getTenantId, fact.getTenantId())
                .eq(RuleStateDO::getRuleId, rule.getId())
                .eq(RuleStateDO::getAlarmTargetTypeFlag, rule.getAlarmTargetTypeFlag().getIndex())
                .eq(RuleStateDO::getEntityId, fact.getEntityId())
                .eq(RuleStateDO::getFingerprint, fingerprint)
                .last("limit 1");
        RuleStateDO entityDO = ruleStateManager.getOne(wrapper);
        return Objects.nonNull(entityDO) ? ruleStateBuilder.buildBOByDO(entityDO) : null;
    }

    private RuleStateBO persistState(RuleStateBO state) {
        RuleStateDO entityDO = ruleStateBuilder.buildDOByBO(state);
        if (Objects.isNull(state.getId())) {
            ruleStateManager.save(entityDO);
        } else {
            entityDO.setOperateTime(null);
            ruleStateManager.updateById(entityDO);
        }
        return ruleStateBuilder.buildBOByDO(entityDO);
    }

    private RuleStateExt ruleStateExt(RuleMatch match) {
        RuleStateExt ext = new RuleStateExt();
        ext.setType(AlarmConstant.EXT_RULE_STATE);
        ext.setVersion(1);
        ext.setContent(new RuleStateExt.Content(
                match.getRule().getRuleCode(),
                match.getSeverity(),
                match.getEventType(),
                match.getLabels(),
                Objects.requireNonNullElse(match.getFact().getValues(), Map.of()),
                match.getMatchType(),
                Map.of()));
        return ext;
    }

    private String fingerprint(RuleMatch match, NotifyBO notify, Map<String, Object> variables) {
        NotifyExt.Dedup dedup = null;
        if (Objects.nonNull(notify) && Objects.nonNull(notify.getNotifyExt())
                && Objects.nonNull(notify.getNotifyExt().getContent())) {
            dedup = notify.getNotifyExt().getContent().getDedup();
        }
        if (Objects.nonNull(dedup) && Boolean.TRUE.equals(dedup.getEnabled()) && StringUtils.isNotBlank(dedup.getKey())) {
            return alarmTemplateRenderer.renderText(dedup.getKey(), variables);
        }
        RuleBO rule = match.getRule();
        RuleFact fact = match.getFact();
        return fact.getTenantId() + ":" + rule.getId() + ":" + rule.getAlarmTargetTypeFlag().getCode() + ":"
                + fact.getEntityId();
    }

    private NotifyHistoryBO historySkipped(RuleMatch match, NotifyBO notify, MessageBO message, NotifyChannelBindBO bind,
                                         NotifyChannelBO channel, Map<String, Object> variables, String reason) {
        MessagePayload payload = new MessagePayload(
                Objects.nonNull(channel) ? channel.getChannelTypeFlag() : null,
                null,
                Map.of(),
                List.of());
        NotifySendResult result = NotifySendResult.skipped(
                Objects.nonNull(channel) ? channel.getCredentialRef() : "notify-channel:" + bind.getChannelId(),
                reason);
        return persistHistory(match, notify, message, bind, channel, payload, variables, result);
    }

    private NotifyHistoryBO persistHistory(RuleMatch match, NotifyBO notify, MessageBO message, NotifyChannelBindBO bind,
                                         NotifyChannelBO channel, MessagePayload payload, Map<String, Object> variables,
                                         NotifySendResult result) {
        NotifyHistoryBO history = new NotifyHistoryBO();
        history.setRuleId(match.getRule().getId());
        history.setNotifyId(Objects.nonNull(notify) ? notify.getId() : DefaultConstant.DEFAULT_ID);
        history.setMessageId(Objects.nonNull(message) ? message.getId()
                : Objects.requireNonNullElse(match.getRule().getMessageId(), DefaultConstant.DEFAULT_ID));
        history.setChannelId(Objects.nonNull(channel) ? channel.getId() : bind.getChannelId());
        history.setEventId(Objects.requireNonNullElse(match.getFact().getEventId(), DefaultConstant.DEFAULT_ID));
        history.setChannelTypeFlag(Objects.nonNull(channel) ? channel.getChannelTypeFlag() : payload.getChannelTypeFlag());
        history.setTarget(Objects.toString(result.getTarget(), ""));
        history.setStatusFlag(result.getStatusFlag());
        history.setRequestExt(requestExt(payload, variables));
        history.setResponseExt(responseExt(result));
        history.setErrorMessage(Objects.toString(result.getErrorMessage(), ""));
        history.setRetryCount(0);
        history.setTenantId(match.getFact().getTenantId());
        NotifyHistoryDO entityDO = notifyHistoryBuilder.buildDOByBO(history);
        notifyHistoryManager.save(entityDO);
        return notifyHistoryBuilder.buildBOByDO(entityDO);
    }

    private NotifyHistoryRequestExt requestExt(MessagePayload payload, Map<String, Object> variables) {
        NotifyHistoryRequestExt ext = new NotifyHistoryRequestExt();
        ext.setType(AlarmConstant.EXT_NOTIFY_HISTORY_REQUEST);
        ext.setVersion(1);
        Map<String, Object> renderedPayload = Objects.requireNonNullElse(payload.getPayload(), Map.of());
        ext.setContent(new NotifyHistoryRequestExt.Content(
                Objects.toString(renderedPayload.get("title"), ""),
                Objects.toString(renderedPayload.getOrDefault("summary", renderedPayload.getOrDefault("text", ""))),
                payload.getPayloadType(),
                variables,
                renderedPayload));
        return ext;
    }

    private NotifyHistoryResponseExt responseExt(NotifySendResult result) {
        NotifyHistoryResponseExt ext = new NotifyHistoryResponseExt();
        ext.setType(AlarmConstant.EXT_NOTIFY_HISTORY_RESPONSE);
        ext.setVersion(1);
        ext.setContent(new NotifyHistoryResponseExt.Content(
                result.getProviderMessageId(),
                result.getStatusCode(),
                result.getStatusMessage(),
                Objects.requireNonNullElse(result.getResponsePayload(), Map.of())));
        return ext;
    }

}
