package io.github.pnoker.common.data.biz.alarm;

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
import io.github.pnoker.common.data.repository.NotifyHistoryInsertResult;
import io.github.pnoker.common.data.repository.ReactiveRuleStateStore;
import io.github.pnoker.common.entity.dto.NotifyTaskDTO;
import io.github.pnoker.common.enums.AlarmTargetTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.NotifyChannelTypeEnum;
import io.github.pnoker.common.enums.NotifyHistoryStatusEnum;
import io.github.pnoker.common.enums.RuleStatusEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleNotificationServiceImplTest {

    @Mock private NotifyConfigCache notifyConfigCache;
    @Mock private ReactiveRuleStateStore ruleStateStore;
    @Mock private RuleStateBuilder ruleStateBuilder;
    @Mock private ReactiveNotifyHistoryStore notifyHistoryStore;
    @Mock private NotifyHistoryBuilder notifyHistoryBuilder;
    @Mock private NotifyPolicyEngine notifyPolicyEngine;
    @Mock private MessageRenderService messageRenderService;
    @Mock private NotifyTaskSender notifyTaskSender;
    @Mock private AlarmTemplateRenderer alarmTemplateRenderer;
    @InjectMocks private RuleNotificationServiceImpl service;

    @Test
    void firingPersistsStateAndPendingHistoryThenPublishesTask() {
        RuleMatch match = RuleMatch.firing(rule(), fact());
        NotifyBO notify = notifyPolicy();
        MessageBO message = new MessageBO();
        message.setId(20L);
        NotifyChannelBindBO bind = bind();
        NotifyChannelBO channel = channel();
        MessagePayload payload = new MessagePayload(NotifyChannelTypeEnum.WEBHOOK, "text", Map.of("text", "alarm"), List.of());
        RuleStateDO stateDO = stateDO(1L, RuleStatusEnum.FIRING);
        NotifyHistoryDO historyDO = historyDO(2L, NotifyHistoryStatusEnum.PENDING);

        when(notifyConfigCache.getNotify(10L, 7L)).thenReturn(Mono.just(notify));
        when(notifyConfigCache.getMessage(20L, 7L)).thenReturn(Mono.just(message));
        when(notifyConfigCache.findEnabledBinds(notify)).thenReturn(Mono.just(List.of(bind)));
        when(notifyConfigCache.getChannel(30L, 7L)).thenReturn(Mono.just(channel));
        when(notifyPolicyEngine.decide(any(), any(), any(), any(), any())).thenReturn(NotifyDecision.send());
        when(messageRenderService.render(any(), any(), any())).thenReturn(payload);
        when(ruleStateStore.find(anyLong(), anyLong(), any(byte.class), anyLong(), any())).thenReturn(Mono.empty());
        when(ruleStateBuilder.buildDOByBO(any())).thenReturn(stateDO);
        when(ruleStateStore.transition(any(), any(Boolean.class))).thenReturn(Mono.just(stateDO));
        when(ruleStateBuilder.buildBOByDO(stateDO)).thenReturn(stateBO(stateDO));
        when(notifyHistoryBuilder.buildDOByBO(any())).thenReturn(historyDO);
        when(notifyHistoryStore.insertIdempotent(any())).thenReturn(Mono.just(new NotifyHistoryInsertResult(historyDO, true)));
        when(notifyHistoryBuilder.buildBOByDO(historyDO)).thenReturn(historyBO(historyDO));
        when(ruleStateStore.updateLastNotifyTime(anyLong(), anyLong(), any())).thenReturn(Mono.just(true));

        List<NotifyHistoryBO> histories = service.notify(match).collectList().block();

        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getStatusFlag()).isEqualTo(NotifyHistoryStatusEnum.PENDING);
        verify(notifyHistoryStore).insertIdempotent(any(NotifyHistoryDO.class));
        verify(notifyTaskSender).publish(any(NotifyTaskDTO.class));
    }

    @Test
    void recoveryWithoutFiringStateDoesNotWriteAnything() {
        RuleMatch match = RuleMatch.recovery(rule(), fact());
        when(notifyConfigCache.getNotify(10L, 7L)).thenReturn(Mono.just(notifyPolicy()));
        when(ruleStateStore.find(anyLong(), anyLong(), any(byte.class), anyLong(), any())).thenReturn(Mono.empty());

        assertThat(service.notify(match).collectList().block()).isEmpty();
        verify(ruleStateStore, never()).transition(any(), any(Boolean.class));
        verify(notifyHistoryStore, never()).insert(any());
    }

    @Test
    void disabledChannelProducesSkippedHistoryWithoutPublish() {
        RuleMatch match = RuleMatch.firing(rule(), fact());
        NotifyBO notify = notifyPolicy();
        NotifyChannelBO channel = channel();
        channel.setEnableFlag(EnableFlagEnum.DISABLE);
        NotifyHistoryDO historyDO = historyDO(3L, NotifyHistoryStatusEnum.SKIPPED);
        when(notifyConfigCache.getNotify(10L, 7L)).thenReturn(Mono.just(notify));
        when(notifyConfigCache.getMessage(20L, 7L)).thenReturn(Mono.empty());
        when(notifyConfigCache.findEnabledBinds(notify)).thenReturn(Mono.just(List.of(bind())));
        when(notifyConfigCache.getChannel(30L, 7L)).thenReturn(Mono.just(channel));
        when(ruleStateStore.find(anyLong(), anyLong(), any(byte.class), anyLong(), any())).thenReturn(Mono.empty());
        RuleStateDO stateDO = stateDO(1L, RuleStatusEnum.FIRING);
        when(ruleStateBuilder.buildDOByBO(any())).thenReturn(stateDO);
        when(ruleStateStore.transition(any(), any(Boolean.class))).thenReturn(Mono.just(stateDO));
        when(ruleStateBuilder.buildBOByDO(stateDO)).thenReturn(stateBO(stateDO));
        when(notifyHistoryBuilder.buildDOByBO(any())).thenReturn(historyDO);
        when(notifyHistoryStore.insert(any())).thenReturn(Mono.just(historyDO));
        when(notifyHistoryBuilder.buildBOByDO(historyDO)).thenReturn(historyBO(historyDO));

        List<NotifyHistoryBO> histories = service.notify(match).collectList().block();

        assertThat(histories).singleElement().extracting(NotifyHistoryBO::getStatusFlag)
                .isEqualTo(NotifyHistoryStatusEnum.SKIPPED);
        verify(notifyTaskSender, never()).publish(any());
    }

    @Test
    void duplicatePendingHistoryDoesNotPublishAgain() {
        RuleMatch match = RuleMatch.firing(rule(), fact());
        NotifyBO notify = notifyPolicy();
        MessageBO message = new MessageBO();
        message.setId(20L);
        NotifyChannelBindBO bind = bind();
        NotifyChannelBO channel = channel();
        MessagePayload payload = new MessagePayload(NotifyChannelTypeEnum.WEBHOOK, "text", Map.of("text", "alarm"), List.of());
        RuleStateDO stateDO = stateDO(1L, RuleStatusEnum.FIRING);
        NotifyHistoryDO historyDO = historyDO(2L, NotifyHistoryStatusEnum.PENDING);

        when(notifyConfigCache.getNotify(10L, 7L)).thenReturn(Mono.just(notify));
        when(notifyConfigCache.getMessage(20L, 7L)).thenReturn(Mono.just(message));
        when(notifyConfigCache.findEnabledBinds(notify)).thenReturn(Mono.just(List.of(bind)));
        when(notifyConfigCache.getChannel(30L, 7L)).thenReturn(Mono.just(channel));
        when(notifyPolicyEngine.decide(any(), any(), any(), any(), any())).thenReturn(NotifyDecision.send());
        when(messageRenderService.render(any(), any(), any())).thenReturn(payload);
        when(ruleStateStore.find(anyLong(), anyLong(), any(byte.class), anyLong(), any())).thenReturn(Mono.empty());
        when(ruleStateBuilder.buildDOByBO(any())).thenReturn(stateDO);
        when(ruleStateStore.transition(any(), any(Boolean.class))).thenReturn(Mono.just(stateDO));
        when(ruleStateBuilder.buildBOByDO(stateDO)).thenReturn(stateBO(stateDO));
        when(notifyHistoryBuilder.buildDOByBO(any())).thenReturn(historyDO);
        when(notifyHistoryStore.insertIdempotent(any())).thenReturn(Mono.just(new NotifyHistoryInsertResult(historyDO, false)));
        when(notifyHistoryBuilder.buildBOByDO(historyDO)).thenReturn(historyBO(historyDO));

        List<NotifyHistoryBO> histories = service.notify(match).collectList().block();

        assertThat(histories).singleElement().extracting(NotifyHistoryBO::getId).isEqualTo(2L);
        verify(notifyTaskSender, never()).publish(any());
        verify(ruleStateStore, never()).updateLastNotifyTime(anyLong(), anyLong(), any());
    }

    private static RuleBO rule() {
        RuleBO value = new RuleBO(); value.setId(1L); value.setRuleCode("r1");
        value.setAlarmTargetTypeFlag(AlarmTargetTypeEnum.POINT); value.setNotifyId(10L); value.setMessageId(20L); return value;
    }
    private static RuleFact fact() { return new RuleFact(7L, AlarmTargetTypeEnum.POINT, 11L, null, LocalDateTime.now(), Map.of("value", 1)); }
    private static NotifyBO notifyPolicy() { NotifyBO value = new NotifyBO(); value.setId(10L); value.setTenantId(7L); return value; }
    private static NotifyChannelBindBO bind() { NotifyChannelBindBO value = new NotifyChannelBindBO(); value.setId(31L); value.setNotifyId(10L); value.setChannelId(30L); value.setTenantId(7L); value.setEnableFlag(EnableFlagEnum.ENABLE); return value; }
    private static NotifyChannelBO channel() { NotifyChannelBO value = new NotifyChannelBO(); value.setId(30L); value.setTenantId(7L); value.setEnableFlag(EnableFlagEnum.ENABLE); value.setChannelTypeFlag(NotifyChannelTypeEnum.WEBHOOK); value.setCredentialRef("ref"); return value; }
    private static RuleStateDO stateDO(long id, RuleStatusEnum status) { RuleStateDO value = new RuleStateDO(); value.setId(id); value.setRuleId(1L); value.setAlarmTargetTypeFlag(AlarmTargetTypeEnum.POINT.getIndex()); value.setEntityId(11L); value.setFingerprint("7:1:point:11"); value.setEntityStateFlag(status.getIndex()); value.setTenantId(7L); value.setTriggerCount(1L); return value; }
    private static RuleStateBO stateBO(RuleStateDO value) { RuleStateBO result = new RuleStateBO(); result.setId(value.getId()); result.setRuleId(value.getRuleId()); result.setAlarmTargetTypeFlag(AlarmTargetTypeEnum.POINT); result.setEntityId(value.getEntityId()); result.setFingerprint(value.getFingerprint()); result.setEntityStateFlag(RuleStatusEnum.ofIndex(value.getEntityStateFlag())); result.setTenantId(value.getTenantId()); return result; }
    private static NotifyHistoryDO historyDO(long id, NotifyHistoryStatusEnum status) { NotifyHistoryDO value = new NotifyHistoryDO(); value.setId(id); value.setStatusFlag(status.getIndex()); value.setTenantId(7L); value.setRuleId(1L); value.setNotifyId(10L); value.setMessageId(20L); value.setChannelId(30L); return value; }
    private static NotifyHistoryBO historyBO(NotifyHistoryDO value) { NotifyHistoryBO result = new NotifyHistoryBO(); result.setId(value.getId()); result.setStatusFlag(NotifyHistoryStatusEnum.ofIndex(value.getStatusFlag())); result.setTenantId(value.getTenantId()); return result; }
}
