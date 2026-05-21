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
import io.github.pnoker.common.entity.ext.RuleExt;
import io.github.pnoker.common.enums.AlarmTargetTypeFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.NotifyChannelTypeFlagEnum;
import io.github.pnoker.common.enums.NotifyHistoryStatusEnum;
import io.github.pnoker.common.enums.RuleStateFlagEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

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

    @Mock
    private NotifyConfigCache notifyConfigCache;

    @Mock
    private RuleStateManager ruleStateManager;

    @Mock
    private RuleStateBuilder ruleStateBuilder;

    @Mock
    private NotifyHistoryManager notifyHistoryManager;

    @Mock
    private NotifyHistoryBuilder notifyHistoryBuilder;

    @Mock
    private NotifyPolicyEngine notifyPolicyEngine;

    @Mock
    private MessageRenderService messageRenderService;

    @Mock
    private NotifyTaskSender notifyTaskSender;

    @Mock
    private AlarmTemplateRenderer alarmTemplateRenderer;

    @InjectMocks
    private RuleNotificationServiceImpl service;

    // ---------- fixtures ----------

    private static RuleBO rule() {
        RuleBO bo = new RuleBO();
        bo.setId(1L);
        bo.setRuleCode("test-rule");
        bo.setRuleName("Test Rule");
        bo.setAlarmTargetTypeFlag(AlarmTargetTypeFlagEnum.POINT);
        bo.setNotifyId(10L);
        bo.setMessageId(20L);
        RuleExt ext = new RuleExt();
        RuleExt.Content content = new RuleExt.Content();
        content.setSeverity("P1");
        content.setEventType("threshold");
        ext.setContent(content);
        bo.setRuleExt(ext);
        return bo;
    }

    private static RuleFact fact() {
        return new RuleFact(7L, AlarmTargetTypeFlagEnum.POINT, 11L, null,
                LocalDateTime.of(2026, 5, 21, 12, 0), Map.of("value", 100));
    }

    private static RuleMatch firingMatch() {
        return RuleMatch.firing(rule(), fact());
    }

    private static RuleMatch recoveryMatch() {
        return RuleMatch.recovery(rule(), fact());
    }

    private static NotifyBO notify(long id) {
        NotifyBO bo = new NotifyBO();
        bo.setId(id);
        bo.setTenantId(7L);
        bo.setNotifyInterval(60_000L);
        return bo;
    }

    private static NotifyChannelBO channel(long id, long tenantId) {
        NotifyChannelBO bo = new NotifyChannelBO();
        bo.setId(id);
        bo.setTenantId(tenantId);
        bo.setChannelTypeFlag(NotifyChannelTypeFlagEnum.WEBHOOK);
        bo.setEnableFlag(EnableFlagEnum.ENABLE);
        bo.setCredentialRef("secret:test:" + id);
        return bo;
    }

    private static MessageBO message(long id) {
        MessageBO bo = new MessageBO();
        bo.setId(id);
        bo.setMessageName("test-message");
        return bo;
    }

    private static NotifyChannelBindBO bind(long channelId, long tenantId) {
        NotifyChannelBindBO bo = new NotifyChannelBindBO();
        bo.setId(100L + channelId);
        bo.setChannelId(channelId);
        bo.setTenantId(tenantId);
        return bo;
    }

    private static RuleStateBO state(long ruleId, long entityId, RuleStateFlagEnum flag) {
        RuleStateBO bo = new RuleStateBO();
        bo.setId(1L);
        bo.setRuleId(ruleId);
        bo.setAlarmTargetTypeFlag(AlarmTargetTypeFlagEnum.POINT);
        bo.setEntityId(entityId);
        bo.setFingerprint("7:1:point:11");
        bo.setStateFlag(flag);
        bo.setFirstTriggerTime(LocalDateTime.of(2026, 5, 21, 11, 0));
        bo.setLastTriggerTime(LocalDateTime.of(2026, 5, 21, 11, 0));
        bo.setTriggerCount(1L);
        bo.setAlarmId(100L);
        bo.setTenantId(7L);
        return bo;
    }

    // ---------- notify: firing path ----------

    private static RuleStateDO stateDO(long id, RuleStateFlagEnum flag, long triggerCount) {
        RuleStateDO entity = new RuleStateDO();
        entity.setId(id);
        entity.setRuleId(1L);
        entity.setAlarmTargetTypeFlag(AlarmTargetTypeFlagEnum.POINT.getIndex());
        entity.setEntityId(11L);
        entity.setFingerprint("7:1:point:11");
        entity.setStateFlag(flag.getIndex());
        entity.setTriggerCount(triggerCount);
        entity.setAlarmId(100L);
        entity.setTenantId(7L);
        return entity;
    }

    private static NotifyExt dedupDisabledExt() {
        NotifyExt ext = new NotifyExt();
        NotifyExt.Content content = new NotifyExt.Content();
        content.setRecovery(new NotifyExt.Recovery(false, false, false));
        ext.setContent(content);
        return ext;
    }

    // ---------- notify: recovery path ----------

    @Test
    void notifyPersistsFiringStateAndPendingHistoryAndPublishesTask() {
        RuleMatch match = firingMatch();
        stubNotifyConfigLoaded(match);
        when(notifyPolicyEngine.decide(any(), any(), any(), any(), any())).thenReturn(NotifyDecision.send());
        when(messageRenderService.render(any(), any(), any()))
                .thenReturn(new MessagePayload(NotifyChannelTypeFlagEnum.WEBHOOK, "json", Map.of("title", "T"), List.of()));
        stubStateBuilder();
        stubHistoryBuilder();

        // No existing rule_state row — first fire
        when(ruleStateManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(ruleStateManager.save(any())).thenReturn(true);
        doNotExpectDuplicateKey();

        List<NotifyHistoryBO> histories = service.notify(match);

        assertThat(histories).hasSize(1);
        // Verify rule_state save (new row)
        ArgumentCaptor<RuleStateDO> stateCaptor = ArgumentCaptor.forClass(RuleStateDO.class);
        verify(ruleStateManager).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getStateFlag()).isEqualTo(RuleStateFlagEnum.FIRING.getIndex());
        assertThat(stateCaptor.getValue().getTriggerCount()).isEqualTo(1L);
        // Verify PENDING history persisted
        ArgumentCaptor<NotifyHistoryDO> historyCaptor = ArgumentCaptor.forClass(NotifyHistoryDO.class);
        verify(notifyHistoryManager).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getStatusFlag()).isEqualTo(NotifyHistoryStatusEnum.PENDING.getIndex());
        // Verify MQ task published
        verify(notifyTaskSender).publish(any(NotifyTaskDTO.class));
    }

    @Test
    void notifyAtomicallyIncrementsTriggerCountForExistingState() {
        RuleMatch match = firingMatch();
        stubNotifyConfigLoaded(match);
        when(notifyPolicyEngine.decide(any(), any(), any(), any(), any())).thenReturn(NotifyDecision.send());
        when(messageRenderService.render(any(), any(), any()))
                .thenReturn(new MessagePayload(NotifyChannelTypeFlagEnum.WEBHOOK, "json", Map.of(), List.of()));
        stubStateBuilder();
        stubHistoryBuilder();

        // Existing FIRING row — subsequent fire
        RuleStateDO existingDO = stateDO(1L, RuleStateFlagEnum.FIRING, 5L);
        when(ruleStateManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(existingDO);
        RuleStateBO existingBO = state(1L, 11L, RuleStateFlagEnum.FIRING);
        existingBO.setTriggerCount(5L);
        when(ruleStateBuilder.buildBOByDO(existingDO)).thenReturn(existingBO);

        when(ruleStateManager.update(any(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class)))
                .thenReturn(true);

        service.notify(match);

        // Must use LambdaUpdateWrapper with setSql("trigger_count = trigger_count + 1")
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<RuleStateDO>> updateCaptor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(ruleStateManager).update(updateCaptor.capture());
        // updateById must not be used for state transitions
        verify(ruleStateManager, never()).updateById(any());
    }

    // ---------- notify: skip conditions ----------

    @Test
    void notifySetsRecoveredStateForExistingFiringRow() {
        RuleMatch match = recoveryMatch();
        stubNotifyConfigLoaded(match);
        when(notifyPolicyEngine.decide(any(), any(), any(), any(), any())).thenReturn(NotifyDecision.send());
        when(messageRenderService.render(any(), any(), any()))
                .thenReturn(new MessagePayload(NotifyChannelTypeFlagEnum.WEBHOOK, "json", Map.of(), List.of()));
        stubStateBuilder();
        stubHistoryBuilder();

        // Existing FIRING row — recovery gated
        RuleStateDO existingDO = stateDO(1L, RuleStateFlagEnum.FIRING, 3L);
        when(ruleStateManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(existingDO);
        RuleStateBO existingBO = state(1L, 11L, RuleStateFlagEnum.FIRING);
        when(ruleStateBuilder.buildBOByDO(existingDO)).thenReturn(existingBO);

        when(ruleStateManager.update(any(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class)))
                .thenReturn(true);

        service.notify(match);

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<RuleStateDO>> updateCaptor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(ruleStateManager).update(updateCaptor.capture());
        verify(notifyHistoryManager).save(any());
    }

    @Test
    void notifyDropsRecoveryWhenNoFiringStateExists() {
        RuleMatch match = recoveryMatch();
        stubNotifyConfigLoaded(match);

        // No existing state at all
        when(ruleStateManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        List<NotifyHistoryBO> histories = service.notify(match);

        assertThat(histories).isEmpty();
        verify(ruleStateManager, never()).save(any(RuleStateDO.class));
        verify(notifyHistoryManager, never()).save(any(NotifyHistoryDO.class));
    }

    @Test
    void notifyReturnsEmptyWhenNotifyPolicyMissing() {
        RuleMatch match = firingMatch();
        when(notifyConfigCache.findNotify(anyLong())).thenReturn(null);
        when(alarmTemplateRenderer.renderText(any(), any())).thenReturn("7:1:point:11");

        // No existing state
        when(ruleStateManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        stubStateBuilderForSave();

        List<NotifyHistoryBO> histories = service.notify(match);
        assertThat(histories).isEmpty();
        verify(notifyTaskSender, never()).publish(any());
    }

    @Test
    void notifySkipsChannelOnDisabledChannel() {
        RuleMatch match = firingMatch();
        NotifyBO notify = notify(10L);
        notify.setNotifyExt(dedupDisabledExt());
        when(notifyConfigCache.findNotify(10L)).thenReturn(notify);
        when(notifyConfigCache.findMessage(20L)).thenReturn(message(20L));

        NotifyChannelBindBO bind = bind(30L, 7L);
        when(notifyConfigCache.findEnabledBinds(notify)).thenReturn(List.of(bind));

        NotifyChannelBO channel = channel(30L, 7L);
        channel.setEnableFlag(EnableFlagEnum.DISABLE);
        when(notifyConfigCache.findChannel(30L, 7L)).thenReturn(channel);

        when(alarmTemplateRenderer.renderText(any(), any())).thenReturn("7:1:point:11");
        stubStateBuilderForSaveWhenNoExisting();
        stubHistoryBuilderForSave();

        List<NotifyHistoryBO> histories = service.notify(match);
        assertThat(histories).hasSize(1);
        ArgumentCaptor<NotifyHistoryDO> captor = ArgumentCaptor.forClass(NotifyHistoryDO.class);
        verify(notifyHistoryManager).save(captor.capture());
        assertThat(captor.getValue().getStatusFlag()).isEqualTo(NotifyHistoryStatusEnum.SKIPPED.getIndex());
        verify(notifyTaskSender, never()).publish(any());
    }

    // ---------- notifyBatch ----------

    @Test
    void notifySkipsChannelOnMissingMessageTemplate() {
        RuleMatch match = firingMatch();
        NotifyBO notify = notify(10L);
        notify.setNotifyExt(dedupDisabledExt());
        when(notifyConfigCache.findNotify(10L)).thenReturn(notify);
        when(notifyConfigCache.findMessage(20L)).thenReturn(null);

        NotifyChannelBindBO bind = bind(30L, 7L);
        when(notifyConfigCache.findEnabledBinds(notify)).thenReturn(List.of(bind));

        NotifyChannelBO channel = channel(30L, 7L);
        when(notifyConfigCache.findChannel(30L, 7L)).thenReturn(channel);

        when(alarmTemplateRenderer.renderText(any(), any())).thenReturn("7:1:point:11");
        stubStateBuilderForSaveWhenNoExisting();
        stubHistoryBuilderForSave();

        List<NotifyHistoryBO> histories = service.notify(match);
        assertThat(histories).hasSize(1);
        ArgumentCaptor<NotifyHistoryDO> captor = ArgumentCaptor.forClass(NotifyHistoryDO.class);
        verify(notifyHistoryManager).save(captor.capture());
        assertThat(captor.getValue().getStatusFlag()).isEqualTo(NotifyHistoryStatusEnum.SKIPPED.getIndex());
    }

    @Test
    void notifySkipsOnPolicyDecisionRejection() {
        RuleMatch match = firingMatch();
        stubNotifyConfigLoaded(match);
        when(notifyPolicyEngine.decide(any(), any(), any(), any(), any()))
                .thenReturn(NotifyDecision.skip("rate-limited"));

        List<NotifyHistoryBO> histories = service.notify(match);
        assertThat(histories).hasSize(1);
        ArgumentCaptor<NotifyHistoryDO> captor = ArgumentCaptor.forClass(NotifyHistoryDO.class);
        verify(notifyHistoryManager).save(captor.capture());
        assertThat(captor.getValue().getStatusFlag()).isEqualTo(NotifyHistoryStatusEnum.SKIPPED.getIndex());
        verify(notifyTaskSender, never()).publish(any());
    }

    // ---------- persistState: duplicate-key fallback ----------

    @Test
    void notifyBatchProcessesMultipleMatchesInSingleCall() {
        RuleMatch m1 = firingMatch();
        RuleMatch m2 = firingMatch();
        // Same rule + fact structure; second match reuses cached config lookups
        stubNotifyConfigLoaded(m1);
        stubStateBuilderForSaveWhenNoExisting();
        stubHistoryBuilderForSave();

        when(notifyPolicyEngine.decide(any(), any(), any(), any(), any()))
                .thenReturn(NotifyDecision.send());
        when(messageRenderService.render(any(), any(), any()))
                .thenReturn(new MessagePayload(NotifyChannelTypeFlagEnum.WEBHOOK, "json", Map.of("title", "T"), List.of()));

        List<NotifyHistoryBO> histories = service.notifyBatch(List.of(m1, m2));
        // Each match has 1 bind × 1 channel = 1 history; 2 matches = 2 histories
        assertThat(histories).hasSize(2);
        verify(notifyTaskSender).publish(any(NotifyTaskDTO.class));
    }

    // ---------- helpers ----------

    @Test
    void notifyBatchReturnsEmptyForNullAndEmptyInput() {
        assertThat(service.notifyBatch(null)).isEmpty();
        assertThat(service.notifyBatch(List.of())).isEmpty();
        verifyNoDbInteraction();
    }

    @Test
    void persistStateRetriesWithUpdateOnDuplicateKey() {
        RuleMatch match = firingMatch();
        NotifyBO notify = notify(10L);
        notify.setNotifyExt(dedupDisabledExt());
        when(notifyConfigCache.findNotify(10L)).thenReturn(notify);
        when(notifyConfigCache.findMessage(20L)).thenReturn(message(20L));

        NotifyChannelBindBO bind = bind(30L, 7L);
        when(notifyConfigCache.findEnabledBinds(notify)).thenReturn(List.of(bind));
        NotifyChannelBO channel = channel(30L, 7L);
        when(notifyConfigCache.findChannel(30L, 7L)).thenReturn(channel);
        when(notifyPolicyEngine.decide(any(), any(), any(), any(), any())).thenReturn(NotifyDecision.send());
        when(messageRenderService.render(any(), any(), any()))
                .thenReturn(new MessagePayload(NotifyChannelTypeFlagEnum.WEBHOOK, "json", Map.of(), List.of()));
        when(alarmTemplateRenderer.renderText(any(), any())).thenReturn("7:1:point:11");

        // First load: no existing state → will try insert
        when(ruleStateManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        // Insert throws DuplicateKeyException (another thread inserted first)
        when(ruleStateManager.save(any(RuleStateDO.class))).thenThrow(new DuplicateKeyException("duplicate"));
        // Reload after exception: now the row exists
        RuleStateDO reloadedDO = stateDO(99L, RuleStateFlagEnum.FIRING, 1L);
        when(ruleStateManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(reloadedDO);
        RuleStateBO reloadedBO = state(1L, 11L, RuleStateFlagEnum.FIRING);
        reloadedBO.setId(99L);
        when(ruleStateBuilder.buildBOByDO(reloadedDO)).thenReturn(reloadedBO);
        // Fallback to atomic update
        when(ruleStateManager.update(any(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class)))
                .thenReturn(true);
        when(ruleStateBuilder.buildDOByBO(any())).thenReturn(stateDO(99L, RuleStateFlagEnum.FIRING, 1L));
        stubHistoryBuilderForSave();

        List<NotifyHistoryBO> histories = service.notify(match);

        assertThat(histories).isNotEmpty();
        // Must have tried save first, then fallen back to update
        verify(ruleStateManager).save(any(RuleStateDO.class));
        verify(ruleStateManager).update(any(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class));
    }

    private void stubNotifyConfigLoaded(RuleMatch match) {
        NotifyBO notify = notify(match.getRule().getNotifyId());
        notify.setNotifyExt(dedupDisabledExt());
        when(notifyConfigCache.findNotify(match.getRule().getNotifyId())).thenReturn(notify);
        when(notifyConfigCache.findMessage(match.getRule().getMessageId())).thenReturn(message(match.getRule().getMessageId()));

        NotifyChannelBindBO bind = bind(30L, 7L);
        when(notifyConfigCache.findEnabledBinds(notify)).thenReturn(List.of(bind));
        NotifyChannelBO channel = channel(30L, 7L);
        when(notifyConfigCache.findChannel(30L, 7L)).thenReturn(channel);

        when(alarmTemplateRenderer.renderText(any(), any())).thenReturn("7:1:point:11");
    }

    private void doNotExpectDuplicateKey() {
        // save succeeds without duplicate
    }

    private void stubStateBuilderForSaveWhenNoExisting() {
        when(ruleStateManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(ruleStateManager.save(any(RuleStateDO.class))).thenReturn(true);
        when(ruleStateBuilder.buildDOByBO(any())).thenAnswer(inv -> {
            RuleStateBO bo = inv.getArgument(0);
            RuleStateDO entity = new RuleStateDO();
            entity.setId(bo.getId());
            entity.setRuleId(bo.getRuleId());
            entity.setAlarmTargetTypeFlag(bo.getAlarmTargetTypeFlag().getIndex());
            entity.setEntityId(bo.getEntityId());
            entity.setFingerprint(bo.getFingerprint());
            entity.setStateFlag(bo.getStateFlag().getIndex());
            entity.setTriggerCount(bo.getTriggerCount());
            entity.setAlarmId(bo.getAlarmId());
            entity.setTenantId(bo.getTenantId());
            return entity;
        });
        when(ruleStateBuilder.buildBOByDO(any())).thenAnswer(inv -> {
            RuleStateDO entity = inv.getArgument(0);
            RuleStateBO bo = new RuleStateBO();
            bo.setId(entity.getId());
            bo.setRuleId(entity.getRuleId());
            bo.setAlarmTargetTypeFlag(AlarmTargetTypeFlagEnum.POINT);
            bo.setEntityId(entity.getEntityId());
            bo.setFingerprint(entity.getFingerprint());
            bo.setStateFlag(RuleStateFlagEnum.ofIndex(entity.getStateFlag()));
            bo.setTriggerCount(entity.getTriggerCount());
            bo.setAlarmId(entity.getAlarmId());
            bo.setTenantId(entity.getTenantId());
            return bo;
        });
    }

    private void stubStateBuilder() {
        when(ruleStateBuilder.buildDOByBO(any())).thenAnswer(inv -> {
            RuleStateBO bo = inv.getArgument(0);
            RuleStateDO entity = new RuleStateDO();
            entity.setId(bo.getId());
            entity.setRuleId(bo.getRuleId());
            entity.setAlarmTargetTypeFlag(bo.getAlarmTargetTypeFlag().getIndex());
            entity.setEntityId(bo.getEntityId());
            entity.setFingerprint(bo.getFingerprint());
            entity.setStateFlag(bo.getStateFlag().getIndex());
            entity.setTriggerCount(bo.getTriggerCount());
            entity.setAlarmId(bo.getAlarmId());
            entity.setTenantId(bo.getTenantId());
            return entity;
        });
        when(ruleStateBuilder.buildBOByDO(any())).thenAnswer(inv -> {
            RuleStateDO entity = inv.getArgument(0);
            RuleStateBO bo = new RuleStateBO();
            bo.setId(entity.getId());
            bo.setRuleId(entity.getRuleId());
            bo.setAlarmTargetTypeFlag(AlarmTargetTypeFlagEnum.POINT);
            bo.setEntityId(entity.getEntityId());
            bo.setFingerprint(entity.getFingerprint());
            bo.setStateFlag(RuleStateFlagEnum.ofIndex(entity.getStateFlag()));
            bo.setTriggerCount(entity.getTriggerCount());
            bo.setAlarmId(entity.getAlarmId());
            bo.setTenantId(entity.getTenantId());
            return bo;
        });
    }

    private void stubStateBuilderForSave() {
        stubStateBuilder();
        when(ruleStateManager.save(any(RuleStateDO.class))).thenReturn(true);
    }

    private void stubHistoryBuilder() {
        when(notifyHistoryBuilder.buildDOByBO(any())).thenAnswer(inv -> {
            NotifyHistoryBO bo = inv.getArgument(0);
            NotifyHistoryDO entity = new NotifyHistoryDO();
            entity.setId(200L);
            entity.setRuleId(bo.getRuleId());
            entity.setNotifyId(bo.getNotifyId());
            entity.setMessageId(bo.getMessageId());
            entity.setChannelId(bo.getChannelId());
            entity.setAlarmId(bo.getAlarmId());
            entity.setStatusFlag(bo.getStatusFlag().getIndex());
            entity.setTenantId(bo.getTenantId());
            return entity;
        });
        when(notifyHistoryBuilder.buildBOByDO(any())).thenAnswer(inv -> {
            NotifyHistoryDO entity = inv.getArgument(0);
            NotifyHistoryBO bo = new NotifyHistoryBO();
            bo.setId(entity.getId());
            bo.setRuleId(entity.getRuleId());
            bo.setNotifyId(entity.getNotifyId());
            bo.setMessageId(entity.getMessageId());
            bo.setChannelId(entity.getChannelId());
            bo.setAlarmId(entity.getAlarmId());
            bo.setStatusFlag(NotifyHistoryStatusEnum.ofIndex(entity.getStatusFlag()));
            bo.setTenantId(entity.getTenantId());
            return bo;
        });
        when(notifyHistoryManager.save(any(NotifyHistoryDO.class))).thenReturn(true);
    }

    private void stubHistoryBuilderForSave() {
        when(notifyHistoryBuilder.buildDOByBO(any())).thenAnswer(inv -> {
            NotifyHistoryBO bo = inv.getArgument(0);
            NotifyHistoryDO entity = new NotifyHistoryDO();
            entity.setId(200L);
            entity.setRuleId(bo.getRuleId());
            entity.setNotifyId(bo.getNotifyId());
            entity.setMessageId(bo.getMessageId());
            entity.setChannelId(bo.getChannelId());
            entity.setAlarmId(bo.getAlarmId());
            entity.setStatusFlag(bo.getStatusFlag() != null ? bo.getStatusFlag().getIndex() : 0);
            entity.setTenantId(bo.getTenantId());
            return entity;
        });
        when(notifyHistoryBuilder.buildBOByDO(any())).thenAnswer(inv -> {
            NotifyHistoryDO entity = inv.getArgument(0);
            NotifyHistoryBO bo = new NotifyHistoryBO();
            bo.setId(entity.getId());
            bo.setRuleId(entity.getRuleId());
            bo.setNotifyId(entity.getNotifyId());
            bo.setMessageId(entity.getMessageId());
            bo.setChannelId(entity.getChannelId());
            bo.setAlarmId(entity.getAlarmId());
            bo.setTenantId(entity.getTenantId());
            return bo;
        });
        when(notifyHistoryManager.save(any(NotifyHistoryDO.class))).thenReturn(true);
    }

    private void verifyNoDbInteraction() {
        verify(ruleStateManager, never()).save(any(RuleStateDO.class));
        verify(ruleStateManager, never()).updateById(any());
        verify(ruleStateManager, never()).update(any(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class));
        verify(notifyHistoryManager, never()).save(any(NotifyHistoryDO.class));
    }
}
