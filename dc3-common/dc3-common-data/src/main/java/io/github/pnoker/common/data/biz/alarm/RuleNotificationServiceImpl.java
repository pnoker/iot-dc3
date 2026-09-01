/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.pnoker.common.data.biz.alarm;

import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.constant.service.AlarmConstant;
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
import io.github.pnoker.common.data.repository.ReactiveNotifyHistoryStore;
import io.github.pnoker.common.data.repository.ReactiveRuleStateStore;
import io.github.pnoker.common.data.repository.NotifyHistoryInsertResult;
import io.github.pnoker.common.entity.dto.NotifyTaskDTO;
import io.github.pnoker.common.entity.ext.NotifyExt;
import io.github.pnoker.common.entity.ext.NotifyHistoryRequestExt;
import io.github.pnoker.common.entity.ext.NotifyHistoryResponseExt;
import io.github.pnoker.common.entity.ext.RuleStateExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.NotifyHistoryStatusEnum;
import io.github.pnoker.common.enums.RuleStatusEnum;
import io.github.pnoker.common.utils.DecodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Reactive rule notification orchestration. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleNotificationServiceImpl implements RuleNotificationService {

    private final NotifyConfigCache notifyConfigCache;
    private final ReactiveRuleStateStore ruleStateStore;
    private final RuleStateBuilder ruleStateBuilder;
    private final ReactiveNotifyHistoryStore notifyHistoryStore;
    private final NotifyHistoryBuilder notifyHistoryBuilder;
    private final NotifyPolicyEngine notifyPolicyEngine;
    private final MessageRenderService messageRenderService;
    private final NotifyTaskSender notifyTaskSender;
    private final AlarmTemplateRenderer alarmTemplateRenderer;

    @Override
    public Flux<NotifyHistoryBO> notify(RuleMatch match) {
        if (match == null || match.getRule() == null || match.getFact() == null) {
            return Flux.empty();
        }
        RuleBO rule = match.getRule();
        Map<String, Object> variables = RuleMatchVariables.of(match);
        return loadNotify(rule.getNotifyId(), match.getFact().getTenantId()).map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMapMany(notifyValue -> {
                    NotifyBO notify = notifyValue.orElse(null);
                    return persistRuleState(match, notify, variables)
                        .flatMapMany(state -> {
                    if (notify == null) {
                        log.warn("Skip alarm notification because notify policy does not exist, ruleId={}", rule.getId());
                        return Flux.empty();
                    }
                    return loadMessage(rule.getMessageId(), match.getFact().getTenantId()).map(Optional::of)
                            .defaultIfEmpty(Optional.empty())
                            .flatMapMany(messageValue -> notifyConfigCache.findEnabledBinds(notify)
                                    .flatMapMany(Flux::fromIterable)
                                    .concatMap(bind -> notifyConfigCache.getChannel(bind.getChannelId(), bind.getTenantId())
                                            .flatMap(channel -> processBind(match, notify, messageValue.orElse(null), bind, state, variables, channel))
                                            .switchIfEmpty(Mono.defer(() -> {
                                                log.warn("Skip alarm notification because notify channel does not exist, channelId={}", bind.getChannelId());
                                                return Mono.empty();
                                            }))));
                });
                });
    }

    @Override
    public Flux<NotifyHistoryBO> notifyBatch(List<RuleMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            return Flux.empty();
        }
        return Flux.fromIterable(matches).filter(Objects::nonNull).concatMap(this::notify);
    }

    private Mono<NotifyHistoryBO> processBind(RuleMatch match, NotifyBO notify, MessageBO message,
                                               NotifyChannelBindBO bind, RuleStateBO state,
                                               Map<String, Object> variables, NotifyChannelBO channel) {
        if (!EnableFlagEnum.ENABLE.equals(channel.getEnableFlag())) {
            return historySkipped(match, notify, message, bind, channel, variables, "Notify channel is disabled");
        }
        if (message == null) {
            return historySkipped(match, notify, null, bind, channel, variables, "Message template does not exist");
        }
        NotifyDecision decision = notifyPolicyEngine.decide(match, notify, bind, state, LocalDateTime.now(ZoneOffset.UTC));
        if (!decision.isSend()) {
            return historySkipped(match, notify, message, bind, channel, variables, decision.getReason());
        }
        MessagePayload payload = messageRenderService.render(message, channel.getChannelTypeFlag(), variables);
        return persistPendingHistory(match, notify, message, bind, channel, payload, variables)
                .flatMap(insertResult -> {
                    NotifyHistoryBO history = notifyHistoryBuilder.buildBOByDO(insertResult.history());
                    if (!insertResult.inserted()) {
                        return Mono.just(history);
                    }
                    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
                    state.setLastNotifyTime(now);
                    return ruleStateStore.updateLastNotifyTime(match.getFact().getTenantId(), state.getId(), now)
                            .flatMap(updated -> updated ? Mono.defer(() -> {
                                        Mono<Void> publication = notifyTaskSender.publish(task(history, match, channel, payload));
                                        return publication == null ? Mono.empty() : publication;
                                    }).thenReturn(history)
                                    : Mono.error(new IllegalStateException("rule state disappeared while updating notify time")));
                });
    }

    private NotifyTaskDTO task(NotifyHistoryBO history, RuleMatch match, NotifyChannelBO channel, MessagePayload payload) {
        return NotifyTaskDTO.builder().notifyHistoryId(history.getId()).tenantId(match.getFact().getTenantId())
                .channelId(channel.getId()).channelTypeFlag(channel.getChannelTypeFlag().getIndex())
                .payloadType(payload.getPayloadType()).payload(payload.getPayload())
                .missingVariables(payload.getMissingVariables()).retryCount(0).createTime(LocalDateTime.now(ZoneOffset.UTC)).build();
    }

    private Mono<NotifyBO> loadNotify(Long notifyId, Long tenantId) {
        return notifyId == null || DefaultConstant.DEFAULT_ID == notifyId
                ? Mono.empty() : notifyConfigCache.getNotify(notifyId, tenantId);
    }

    private Mono<MessageBO> loadMessage(Long messageId, Long tenantId) {
        return messageId == null || DefaultConstant.DEFAULT_ID == messageId
                ? Mono.empty() : notifyConfigCache.getMessage(messageId, tenantId);
    }

    private Mono<RuleStateBO> persistRuleState(RuleMatch match, NotifyBO notify, Map<String, Object> variables) {
        RuleBO rule = match.getRule();
        RuleFact fact = match.getFact();
        String fingerprint = fingerprint(match, notify, variables);
        boolean recovery = Strings.CI.equals(match.getMatchType(), AlarmConstant.MATCH_TYPE_RECOVERY);
        return ruleStateStore.find(fact.getTenantId(), rule.getId(), rule.getAlarmTargetTypeFlag().getIndex(),
                        fact.getEntityId(), fingerprint)
                .flatMap(existing -> {
                    if (recovery && !RuleStatusEnum.FIRING.getIndex().equals(existing.getEntityStateFlag())) {
                        return Mono.empty();
                    }
                    return transition(match, ruleStateBuilder.buildBOByDO(existing), recovery);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    if (recovery) {
                        return Mono.<RuleStateBO>empty();
                    }
                    RuleStateBO state = new RuleStateBO();
                    state.setRuleId(rule.getId());
                    state.setAlarmTargetTypeFlag(rule.getAlarmTargetTypeFlag());
                    state.setEntityId(fact.getEntityId());
                    state.setFingerprint(fingerprint);
                    state.setFirstTriggerTime(LocalDateTime.now(ZoneOffset.UTC));
                    state.setTriggerCount(0L);
                    state.setTenantId(fact.getTenantId());
                    return transition(match, state, false);
                }));
    }

    private Mono<RuleStateBO> transition(RuleMatch match, RuleStateBO state, boolean recovery) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        state.setAlarmId(Objects.requireNonNullElse(match.getFact().getAlarmId(), DefaultConstant.DEFAULT_ID));
        state.setEntityStateExt(ruleStateExt(match));
        if (recovery) {
            state.setEntityStateFlag(RuleStatusEnum.RECOVERED);
            state.setLastRecoverTime(now);
        } else {
            state.setEntityStateFlag(RuleStatusEnum.FIRING);
            state.setLastTriggerTime(now);
            if (state.getFirstTriggerTime() == null) state.setFirstTriggerTime(now);
        }
        RuleStateDO entity = ruleStateBuilder.buildDOByBO(state);
        return ruleStateStore.transition(entity, recovery).map(ruleStateBuilder::buildBOByDO);
    }

    private Mono<NotifyHistoryBO> historySkipped(RuleMatch match, NotifyBO notify, MessageBO message,
                                                  NotifyChannelBindBO bind, NotifyChannelBO channel,
                                                  Map<String, Object> variables, String reason) {
        MessagePayload payload = new MessagePayload(channel == null ? null : channel.getChannelTypeFlag(), null, Map.of(), List.of());
        NotifySendResult result = NotifySendResult.skipped(channel == null ? "notify-channel" + SymbolConstant.COLON + bind.getChannelId() : channel.getCredentialRef(), reason);
        NotifyHistoryBO history = buildHistory(match, notify, message, bind, channel, payload, variables);
        history.setStatusFlag(result.getStatusFlag());
        history.setTarget(Objects.toString(result.getTarget(), ""));
        history.setResponseExt(responseExt(result));
        history.setErrorMessage(Objects.toString(result.getErrorMessage(), ""));
        return saveHistory(history);
    }

    private Mono<NotifyHistoryInsertResult> persistPendingHistory(RuleMatch match, NotifyBO notify, MessageBO message,
                                                                   NotifyChannelBindBO bind, NotifyChannelBO channel,
                                                                   MessagePayload payload, Map<String, Object> variables) {
        NotifyHistoryBO history = buildHistory(match, notify, message, bind, channel, payload, variables);
        history.setStatusFlag(NotifyHistoryStatusEnum.PENDING);
        NotifyHistoryDO entity = notifyHistoryBuilder.buildDOByBO(history);
        return notifyHistoryStore.insertIdempotent(entity);
    }

    private Mono<NotifyHistoryBO> saveHistory(NotifyHistoryBO history) {
        NotifyHistoryDO entity = notifyHistoryBuilder.buildDOByBO(history);
        return notifyHistoryStore.insert(entity).map(notifyHistoryBuilder::buildBOByDO);
    }

    private NotifyHistoryBO buildHistory(RuleMatch match, NotifyBO notify, MessageBO message,
                                         NotifyChannelBindBO bind, NotifyChannelBO channel,
                                         MessagePayload payload, Map<String, Object> variables) {
        NotifyHistoryBO history = new NotifyHistoryBO();
        history.setRuleId(match.getRule().getId());
        history.setNotifyId(notify == null ? DefaultConstant.DEFAULT_ID : notify.getId());
        history.setMessageId(message == null ? Objects.requireNonNullElse(match.getRule().getMessageId(), DefaultConstant.DEFAULT_ID) : message.getId());
        history.setChannelId(channel == null ? bind.getChannelId() : channel.getId());
        history.setAlarmId(Objects.requireNonNullElse(match.getFact().getAlarmId(), DefaultConstant.DEFAULT_ID));
        history.setTenantId(match.getFact().getTenantId());
        String phase = Strings.CI.equals(match.getMatchType(), AlarmConstant.MATCH_TYPE_RECOVERY) ? "recovery" : "firing";
        String target = channel == null ? "channel:" + bind.getChannelId() : StringUtils.defaultString(channel.getCredentialRef(), "channel:" + channel.getId());
        history.setDedupeKey(DecodeUtil.sha256Hex(String.join(SymbolConstant.COLON,
                "v1", String.valueOf(history.getTenantId()), String.valueOf(history.getAlarmId()),
                String.valueOf(history.getRuleId()), String.valueOf(history.getNotifyId()),
                String.valueOf(history.getChannelId()), target, phase)));
        history.setChannelTypeFlag(channel == null ? payload.getChannelTypeFlag() : channel.getChannelTypeFlag());
        history.setRequestExt(requestExt(payload, variables));
        history.setRetryCount(0);
        return history;
    }

    private RuleStateExt ruleStateExt(RuleMatch match) {
        RuleStateExt ext = new RuleStateExt();
        ext.setType(AlarmConstant.EXT_RULE_STATE);
        ext.setVersion(1);
        ext.setContent(new RuleStateExt.Content(match.getRule().getRuleCode(), match.getSeverity(), match.getEventType(),
                match.getLabels(), Objects.requireNonNullElse(match.getFact().getValues(), Map.of()), match.getMatchType(), Map.of()));
        return ext;
    }

    private String fingerprint(RuleMatch match, NotifyBO notify, Map<String, Object> variables) {
        NotifyExt.Dedup dedup = notify != null && notify.getNotifyExt() != null && notify.getNotifyExt().getContent() != null
                ? notify.getNotifyExt().getContent().getDedup() : null;
        if (dedup != null && Boolean.TRUE.equals(dedup.getEnabled()) && StringUtils.isNotBlank(dedup.getKey())) {
            return alarmTemplateRenderer.renderText(dedup.getKey(), variables);
        }
        return match.getFact().getTenantId() + SymbolConstant.COLON + match.getRule().getId() + SymbolConstant.COLON
                + match.getRule().getAlarmTargetTypeFlag().getCode() + SymbolConstant.COLON + match.getFact().getEntityId();
    }

    private NotifyHistoryRequestExt requestExt(MessagePayload payload, Map<String, Object> variables) {
        NotifyHistoryRequestExt ext = new NotifyHistoryRequestExt();
        ext.setType(AlarmConstant.EXT_NOTIFY_HISTORY_REQUEST);
        ext.setVersion(1);
        Map<String, Object> rendered = Objects.requireNonNullElse(payload.getPayload(), Map.of());
        ext.setContent(new NotifyHistoryRequestExt.Content(Objects.toString(rendered.get("title"), ""),
                Objects.toString(rendered.getOrDefault("summary", rendered.getOrDefault("text", ""))), payload.getPayloadType(), variables, rendered));
        return ext;
    }

    private NotifyHistoryResponseExt responseExt(NotifySendResult result) {
        NotifyHistoryResponseExt ext = new NotifyHistoryResponseExt();
        ext.setType(AlarmConstant.EXT_NOTIFY_HISTORY_RESPONSE);
        ext.setVersion(1);
        ext.setContent(new NotifyHistoryResponseExt.Content(result.getProviderMessageId(), result.getStatusCode(), result.getStatusMessage(),
                Objects.requireNonNullElse(result.getResponsePayload(), Map.of())));
        return ext;
    }
}
