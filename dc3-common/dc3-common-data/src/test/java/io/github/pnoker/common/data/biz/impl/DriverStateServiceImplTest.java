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

package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.data.biz.DriverAlarmService;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.data.mapper.EntityStateMapper;
import io.github.pnoker.common.entity.dto.DriverStateDTO;
import io.github.pnoker.common.entity.dto.DriverTimeoutCheckDTO;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import io.github.pnoker.common.enums.TimeoutSourceFlagEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverStateServiceImplTest {

    @Mock
    private DriverAlarmService driverAlarmService;

    @Mock
    private EntityStateMapper entityStateMapper;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private DriverStateServiceImpl service;

    private DriverStateDTO heartbeat(Long driverId, String status, Long tenantId) {
        DriverStateDTO dto = new DriverStateDTO();
        dto.setDriverId(driverId);
        dto.setStatus(status);
        dto.setTenantId(tenantId);
        return dto;
    }

    private EntityStateDO persisted(byte stateFlag, byte lastStateFlag, long leaseVersion) {
        EntityStateDO state = new EntityStateDO();
        state.setEntityTypeFlag((byte) EntityTypeFlagEnum.DRIVER.getIndex());
        state.setEntityId(1L);
        state.setParentEntityId(0L);
        state.setTenantId(100L);
        state.setStateFlag(stateFlag);
        state.setLastStateFlag(lastStateFlag);
        state.setLeaseVersion(leaseVersion);
        state.setTimeoutSeconds(45);
        state.setExpireTime(LocalDateTime.now().plusSeconds(45));
        state.setLastHeartbeatTime(LocalDateTime.now());
        state.setLastAlarmId(0L);
        return state;
    }

    private void stubUpsert(EntityStateDO state) {
        when(entityStateMapper.upsertEntityState(anyLong(), anyLong(), anyByte(), anyLong(), anyLong(), anyByte(),
                anyByte(), any(), anyInt(), anyByte(), anyString(), any())).thenReturn(state);
    }

    @Test
    void nullDtoDoesNothing() {
        service.heartbeat(null);

        verifyNoInteractions(entityStateMapper, rabbitTemplate);
    }

    @Test
    void nullDriverIdDoesNothing() {
        DriverStateDTO dto = new DriverStateDTO();
        dto.setStatus(EntityStatusEnum.ONLINE.getCode());

        service.heartbeat(dto);

        verifyNoInteractions(entityStateMapper, rabbitTemplate);
    }

    @Test
    void driverHeartbeatUpsertsDbRowAndPublishesTimeoutCheck() {
        stubUpsert(persisted((byte) EntityStatusEnum.ONLINE.getIndex(),
                (byte) EntityStatusEnum.OFFLINE.getIndex(), 6L));

        service.heartbeat(heartbeat(1L, EntityStatusEnum.ONLINE.getCode(), 100L));

        verify(entityStateMapper).upsertEntityState(anyLong(),
                eq(100L),
                eq((byte) EntityTypeFlagEnum.DRIVER.getIndex()),
                eq(1L),
                eq(0L),
                eq((byte) EntityStatusEnum.ONLINE.getIndex()),
                eq((byte) EntityStatusEnum.OFFLINE.getIndex()),
                any(LocalDateTime.class),
                eq(45),
                eq((byte) TimeoutSourceFlagEnum.SYSTEM.getIndex()),
                eq("driver-heartbeat"),
                any());

        ArgumentCaptor<DriverTimeoutCheckDTO> captor = ArgumentCaptor.forClass(DriverTimeoutCheckDTO.class);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), captor.capture());
        assertThat(captor.getValue().getDriverId()).isEqualTo(1L);
        assertThat(captor.getValue().getTenantId()).isEqualTo(100L);
        assertThat(captor.getValue().getLeaseVersion()).isEqualTo(6L);
    }

    @Test
    void statusFlipFromOnlineToOfflineTriggersAlarm() {
        stubUpsert(persisted((byte) EntityStatusEnum.OFFLINE.getIndex(),
                (byte) EntityStatusEnum.ONLINE.getIndex(), 4L));

        service.heartbeat(heartbeat(1L, EntityStatusEnum.OFFLINE.getCode(), 100L));

        verify(driverAlarmService).alarm(any());
    }

    @Test
    void sameStatusNoFlipDoesNotTriggerAlarm() {
        stubUpsert(persisted((byte) EntityStatusEnum.ONLINE.getIndex(),
                (byte) EntityStatusEnum.ONLINE.getIndex(), 4L));

        service.heartbeat(heartbeat(1L, EntityStatusEnum.ONLINE.getCode(), 100L));

        verify(driverAlarmService, never()).alarm(any());
    }
}
