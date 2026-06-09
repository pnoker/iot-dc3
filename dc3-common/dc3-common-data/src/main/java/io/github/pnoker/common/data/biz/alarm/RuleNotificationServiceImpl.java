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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.constant.service.AlarmConstant;
import io.github.pnoker.common.data.dal.NotifyHistoryManager;
import io.github.pnoker.common.data.dal.RuleStateManager;
import io.github.pnoker.common.data.entity.bo.MessageBO;
import io.github.pnoker.common.data.entity.bo.NotifyBO;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBO;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBindBO;
import io.github.pnoker.common.data.entity.bo.NotifyHistoryBO;
import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.data.entity.bo.RuleStateBO;
import io.github.pnoker.common.data.entity.builder.NotifyHistoryBuilder;
import io.github.pnoker.common.data.entity.builder.RuleStateBuilder;
import io.github.pnoker.common.data.entity.model.NotifyHistoryDO;
import io.github.pnoker.common.data.entity.model.RuleStateDO;
import io.github.pnoker.common.entity.dto.NotifyTaskDTO;
import io.github.pnoker.common.entity.ext.NotifyExt;
import io.github.pnoker.common.entity.ext.NotifyHistoryRequestExt;
import io.github.pnoker.common.entity.ext.NotifyHistoryResponseExt;
import io.github.pnoker.common.entity.ext.RuleStateExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.NotifyHistoryStatusEnum;
import io.github.pnoker.common.enums.RuleStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
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

    private final NotifyConfigCache notifyConfigCache;

    private final RuleStateManager ruleStateManager;

    private final RuleStateBuilder ruleStateBuilder;

    private final NotifyHistoryManager notifyHistoryManager;

    private final NotifyHistoryBuilder notifyHistoryBuilder;

    private final NotifyPolicyEngine notifyPolicyEngine;

    private final MessageRenderService messageRenderService;

    private final NotifyTaskSender notifyTaskSender;

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
        if (Objects.isNull(state)) {
            return List.of();
        }
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
            NotifyHistoryBO history = persistPendingHistory(match, notify, message, bind, channel, payload, variables);
            histories.add(history);
            state.setLastNotifyTime(LocalDateTime.now());
            persistState(state);
            notifyTaskSender.publish(NotifyTaskDTO.builder()
                    .notifyHistoryId(history.getId())
                    .tenantId(match.getFact().getTenantId())
                    .channelId(channel.getId())
                    .channelTypeFlag(channel.getChannelTypeFlag().getIndex())
                    .payloadType(payload.getPayloadType())
                    .payload(payload.getPayload())
                    .missingVariables(payload.getMissingVariables())
                    .retryCount(0)
                    .createTime(LocalDateTime.now())
                    .build());
        }
        return histories;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<NotifyHistoryBO> notifyBatch(List<RuleMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            return List.of();
        }
        List<NotifyHistoryBO> allHistories = new ArrayList<>();
        for (RuleMatch match : matches) {
            if (Objects.isNull(match) || Objects.isNull(match.getRule()) || Objects.isNull(match.getFact())) {
                continue;
            }
            RuleBO rule = match.getRule();
            Map<String, Object> variables = RuleMatchVariables.of(match);
            NotifyBO notify = loadNotify(rule.getNotifyId());
            RuleStateBO state = persistRuleState(match, notify, variables);
            if (Objects.isNull(state)) {
                continue;
            }
            if (Objects.isNull(notify)) {
                log.warn("Skip alarm notification because notify policy does not exist, ruleId={}", rule.getId());
                continue;
            }
            MessageBO message = loadMessage(rule.getMessageId());
            List<NotifyChannelBindBO> binds = loadEnabledBinds(notify);
            for (NotifyChannelBindBO bind : binds) {
                NotifyChannelBO channel = loadChannel(bind.getChannelId(), bind.getTenantId());
                if (Objects.isNull(channel)) {
                    log.warn("Skip alarm notification because notify channel does not exist, channelId={}",
                            bind.getChannelId());
                    continue;
                }
                if (!EnableFlagEnum.ENABLE.equals(channel.getEnableFlag())) {
                    allHistories.add(historySkipped(match, notify, message, bind, channel, variables,
                            "Notify channel is disabled"));
                    continue;
                }
                if (Objects.isNull(message)) {
                    allHistories.add(historySkipped(match, notify, null, bind, channel, variables,
                            "Message template does not exist"));
                    continue;
                }
                NotifyDecision decision = notifyPolicyEngine.decide(match, notify, bind, state, LocalDateTime.now());
                if (!decision.isSend()) {
                    allHistories.add(historySkipped(match, notify, message, bind, channel, variables, decision.getReason()));
                    continue;
                }
                MessagePayload payload = messageRenderService.render(message, channel.getChannelTypeFlag(), variables);
                NotifyHistoryBO history = persistPendingHistory(match, notify, message, bind, channel, payload, variables);
                allHistories.add(history);
                state.setLastNotifyTime(LocalDateTime.now());
                persistState(state);
                notifyTaskSender.publish(NotifyTaskDTO.builder()
                        .notifyHistoryId(history.getId())
                        .tenantId(match.getFact().getTenantId())
                        .channelId(channel.getId())
                        .channelTypeFlag(channel.getChannelTypeFlag().getIndex())
                        .payloadType(payload.getPayloadType())
                        .payload(payload.getPayload())
                        .missingVariables(payload.getMissingVariables())
                        .retryCount(0)
                        .createTime(LocalDateTime.now())
                        .build());
            }
        }
        return allHistories;
    }

    private NotifyBO loadNotify(Long notifyId) {
        if (Objects.isNull(notifyId) || DefaultConstant.DEFAULT_ID == notifyId) {
            return null;
        }
        return notifyConfigCache.getNotify(notifyId);
    }

    private MessageBO loadMessage(Long messageId) {
        if (Objects.isNull(messageId) || DefaultConstant.DEFAULT_ID == messageId) {
            return null;
        }
        return notifyConfigCache.getMessage(messageId);
    }

    private List<NotifyChannelBindBO> loadEnabledBinds(NotifyBO notify) {
        return notifyConfigCache.findEnabledBinds(notify);
    }

    private NotifyChannelBO loadChannel(Long channelId, Long tenantId) {
        return notifyConfigCache.getChannel(channelId, tenantId);
    }

    private RuleStateBO persistRuleState(RuleMatch match, NotifyBO notify, Map<String, Object> variables) {
        RuleBO rule = match.getRule();
        RuleFact fact = match.getFact();
        String fingerprint = fingerprint(match, notify, variables);
        RuleStateBO state = loadState(rule, fact, fingerprint);
        boolean isRecovery = StringUtils.equalsIgnoreCase(match.getMatchType(), AlarmConstant.MATCH_TYPE_RECOVERY);
        if (isRecovery && (Objects.isNull(state) || !RuleStatusEnum.FIRING.equals(state.getEntityStateFlag()))) {
            log.debug("Skip recovery state transition because no FIRING fingerprint exists, ruleId={}, entityId={}",
                    rule.getId(), fact.getEntityId());
            return null;
        }

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
        state.setAlarmId(Objects.requireNonNullElse(fact.getAlarmId(), DefaultConstant.DEFAULT_ID));
        state.setEntityStateExt(ruleStateExt(match));

        if (isRecovery) {
            state.setEntityStateFlag(RuleStatusEnum.RECOVERED);
            state.setLastRecoverTime(now);
        } else {
            state.setEntityStateFlag(RuleStatusEnum.FIRING);
            state.setLastTriggerTime(now);
            if (Objects.isNull(state.getFirstTriggerTime())) {
                state.setFirstTriggerTime(now);
            }
        }
        return persistState(state, isRecovery);
    }

    private RuleStateBO loadState(RuleBO rule, RuleFact fact, String fingerprint) {
        return loadState(rule.getId(), rule.getAlarmTargetTypeFlag().getIndex(),
                fact.getEntityId(), fingerprint, fact.getTenantId());
    }

    private RuleStateBO loadState(long ruleId, byte alarmTargetTypeFlag, long entityId,
                                  String fingerprint, long tenantId) {
        LambdaQueryWrapper<RuleStateDO> wrapper = Wrappers.<RuleStateDO>query().lambda()
                .eq(RuleStateDO::getTenantId, tenantId)
                .eq(RuleStateDO::getRuleId, ruleId)
                .eq(RuleStateDO::getAlarmTargetTypeFlag, alarmTargetTypeFlag)
                .eq(RuleStateDO::getEntityId, entityId)
                .eq(RuleStateDO::getFingerprint, fingerprint)
                .last("limit 1");
        RuleStateDO entityDO = ruleStateManager.getOne(wrapper);
        return Objects.nonNull(entityDO) ? ruleStateBuilder.buildBOByDO(entityDO) : null;
    }

    /**
     * Persist a rule-state transition atomically. For the INSERT path (new
     * state row) it handles {@link DuplicateKeyException} by reloading and
     * retrying through the UPDATE path. For the UPDATE path it uses
     * {@code setSql("trigger_count = trigger_count + 1")} so concurrent
     * firings do not lose increments.
     */
    private RuleStateBO persistState(RuleStateBO state, boolean recovery) {
        RuleStateDO entityDO = ruleStateBuilder.buildDOByBO(state);
        if (Objects.isNull(state.getId())) {
            entityDO.setTriggerCount(recovery ? 0L : 1L);
            try {
                ruleStateManager.save(entityDO);
            } catch (DuplicateKeyException e) {
                RuleStateBO existing = loadState(state.getRuleId(), state.getAlarmTargetTypeFlag().getIndex(),
                        state.getEntityId(), state.getFingerprint(), state.getTenantId());
                if (Objects.isNull(existing)) {
                    log.warn("Duplicate key on rule_state insert but reload returned null, ruleId={}, entityId={}",
                            state.getRuleId(), state.getEntityId());
                    throw e;
                }
                state.setId(existing.getId());
                return persistState(state, recovery);
            }
        } else {
            LambdaUpdateWrapper<RuleStateDO> wrapper = Wrappers.<RuleStateDO>lambdaUpdate()
                    .eq(RuleStateDO::getId, state.getId())
                    .set(RuleStateDO::getEntityStateFlag, state.getEntityStateFlag().getIndex())
                    .set(RuleStateDO::getLastTriggerTime, state.getLastTriggerTime())
                    .set(RuleStateDO::getLastRecoverTime, state.getLastRecoverTime())
                    .set(RuleStateDO::getAlarmId, state.getAlarmId())
                    .set(RuleStateDO::getEntityStateExt, entityDO.getEntityStateExt());
            if (recovery) {
                wrapper.setSql("trigger_count = trigger_count");
            } else {
                wrapper.setSql("trigger_count = trigger_count + 1");
            }
            ruleStateManager.update(wrapper);
        }
        return ruleStateBuilder.buildBOByDO(entityDO);
    }

    /**
     * Lightweight persistence for last-notify-time stamp updates. Does not
     * transition rule state or modify trigger_count — use
     * {@link #persistState(RuleStateBO, boolean)} for state transitions.
     */
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
        return fact.getTenantId() + SymbolConstant.COLON + rule.getId() + SymbolConstant.COLON
                + rule.getAlarmTargetTypeFlag().getCode() + SymbolConstant.COLON + fact.getEntityId();
    }

    private NotifyHistoryBO historySkipped(RuleMatch match, NotifyBO notify, MessageBO message, NotifyChannelBindBO bind,
                                           NotifyChannelBO channel, Map<String, Object> variables, String reason) {
        MessagePayload payload = new MessagePayload(
                Objects.nonNull(channel) ? channel.getChannelTypeFlag() : null,
                null,
                Map.of(),
                List.of());
        NotifySendResult result = NotifySendResult.skipped(
                Objects.nonNull(channel) ? channel.getCredentialRef()
                        : "notify-channel" + SymbolConstant.COLON + bind.getChannelId(),
                reason);
        NotifyHistoryBO history = buildHistory(match, notify, message, bind, channel, payload, variables);
        history.setStatusFlag(result.getStatusFlag());
        history.setTarget(Objects.toString(result.getTarget(), ""));
        history.setResponseExt(responseExt(result));
        history.setErrorMessage(Objects.toString(result.getErrorMessage(), ""));
        NotifyHistoryDO entityDO = notifyHistoryBuilder.buildDOByBO(history);
        notifyHistoryManager.save(entityDO);
        return notifyHistoryBuilder.buildBOByDO(entityDO);
    }

    private NotifyHistoryBO persistPendingHistory(RuleMatch match, NotifyBO notify, MessageBO message,
                                                  NotifyChannelBindBO bind, NotifyChannelBO channel,
                                                  MessagePayload payload, Map<String, Object> variables) {
        NotifyHistoryBO history = buildHistory(match, notify, message, bind, channel, payload, variables);
        history.setStatusFlag(NotifyHistoryStatusEnum.PENDING);
        NotifyHistoryDO entityDO = notifyHistoryBuilder.buildDOByBO(history);
        notifyHistoryManager.save(entityDO);
        return notifyHistoryBuilder.buildBOByDO(entityDO);
    }

    private NotifyHistoryBO buildHistory(RuleMatch match, NotifyBO notify, MessageBO message,
                                         NotifyChannelBindBO bind, NotifyChannelBO channel,
                                         MessagePayload payload, Map<String, Object> variables) {
        NotifyHistoryBO history = new NotifyHistoryBO();
        history.setRuleId(match.getRule().getId());
        history.setNotifyId(Objects.nonNull(notify) ? notify.getId() : DefaultConstant.DEFAULT_ID);
        history.setMessageId(Objects.nonNull(message) ? message.getId()
                : Objects.requireNonNullElse(match.getRule().getMessageId(), DefaultConstant.DEFAULT_ID));
        history.setChannelId(Objects.nonNull(channel) ? channel.getId() : bind.getChannelId());
        history.setAlarmId(Objects.requireNonNullElse(match.getFact().getAlarmId(), DefaultConstant.DEFAULT_ID));
        history.setChannelTypeFlag(Objects.nonNull(channel) ? channel.getChannelTypeFlag() : payload.getChannelTypeFlag());
        history.setRequestExt(requestExt(payload, variables));
        history.setRetryCount(0);
        history.setTenantId(match.getFact().getTenantId());
        return history;
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
