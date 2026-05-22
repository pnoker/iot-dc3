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

package io.github.pnoker.common.data.rabbit;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.rabbitmq.client.Channel;
import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.dal.EntityAlarmManager;
import io.github.pnoker.common.data.dal.EntityStateManager;
import io.github.pnoker.common.data.entity.model.EntityAlarmDO;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.entity.dto.DriverTimeoutCheckDTO;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverTimeoutCheckReceiverTest {

    @Mock
    private EntityStateManager entityStateManager;

    @Mock
    private EntityAlarmManager entityAlarmManager;

    @Mock
    private AlarmRuleTriggerService alarmRuleTriggerService;

    @Mock
    private Channel channel;

    @Mock
    private LambdaQueryChainWrapper<EntityStateDO> queryWrapper;

    @Mock
    private LambdaUpdateChainWrapper<EntityStateDO> claimUpdateWrapper;

    @Mock
    private LambdaUpdateChainWrapper<EntityStateDO> alarmUpdateWrapper;

    private DriverTimeoutCheckReceiver receiver;

    @BeforeEach
    void setUp() {
        receiver = new DriverTimeoutCheckReceiver(entityStateManager, entityAlarmManager, alarmRuleTriggerService);
    }

    private Message mockMessage(long deliveryTag) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(deliveryTag);
        return new Message(new byte[0], props);
    }

    private DriverTimeoutCheckDTO timeoutCheck(Long driverId, Long tenantId, Long leaseVersion) {
        return DriverTimeoutCheckDTO.builder()
                .driverId(driverId)
                .tenantId(tenantId)
                .leaseVersion(leaseVersion)
                .build();
    }

    private EntityStateDO driverState(byte statusFlag, long leaseVersion, LocalDateTime expireTime) {
        EntityStateDO state = new EntityStateDO();
        state.setId(9L);
        state.setTenantId(100L);
        state.setEntityTypeFlag((byte) EntityTypeFlagEnum.DRIVER.getIndex());
        state.setEntityId(7L);
        state.setParentEntityId(0L);
        state.setStateFlag(statusFlag);
        state.setLeaseVersion(leaseVersion);
        state.setExpireTime(expireTime);
        return state;
    }

    private void stubQuery(EntityStateDO state) {
        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.one()).thenReturn(state);
    }

    private void stubUpdateChain() {
        when(entityStateManager.lambdaUpdate()).thenReturn(claimUpdateWrapper, alarmUpdateWrapper);
        when(claimUpdateWrapper.eq(any(), any())).thenReturn(claimUpdateWrapper);
        when(claimUpdateWrapper.set(any(), any())).thenReturn(claimUpdateWrapper);
        when(claimUpdateWrapper.update()).thenReturn(true);
        when(alarmUpdateWrapper.eq(any(), any())).thenReturn(alarmUpdateWrapper);
        when(alarmUpdateWrapper.set(any(), any())).thenReturn(alarmUpdateWrapper);
        when(alarmUpdateWrapper.update()).thenReturn(true);
    }

    @Test
    void rejectsInvalidPayload() throws Exception {
        receiver.driverTimeoutCheck(channel, mockMessage(7L), timeoutCheck(null, 100L, 1L));

        verifyNoInteractions(entityStateManager, entityAlarmManager, alarmRuleTriggerService);
        verify(channel).basicReject(eq(7L), eq(false));
    }

    @Test
    void leaseMismatchMeansNewerHeartbeatArrived() throws Exception {
        stubQuery(driverState((byte) EntityStatusEnum.ONLINE.getIndex(), 3L,
                LocalDateTime.now().minusSeconds(1)));

        receiver.driverTimeoutCheck(channel, mockMessage(8L), timeoutCheck(7L, 100L, 2L));

        verify(channel).basicAck(eq(8L), eq(false));
        verify(entityStateManager, never()).lambdaUpdate();
        verifyNoInteractions(entityAlarmManager, alarmRuleTriggerService);
    }

    @Test
    void expiredFaultDriverIsClaimedOfflineAndTriggersAlarm() throws Exception {
        stubQuery(driverState((byte) EntityStatusEnum.FAULT.getIndex(), 4L,
                LocalDateTime.now().minusSeconds(1)));
        stubUpdateChain();
        when(entityAlarmManager.save(any(EntityAlarmDO.class))).thenReturn(true);

        receiver.driverTimeoutCheck(channel, mockMessage(9L), timeoutCheck(7L, 100L, 4L));

        verify(claimUpdateWrapper).set(any(), eq(EntityStatusEnum.OFFLINE.getIndex()));
        verify(entityAlarmManager).save(any(EntityAlarmDO.class));
        verify(alarmRuleTriggerService).processDriverAlarm(any());
        verify(channel).basicAck(eq(9L), eq(false));
    }

}
