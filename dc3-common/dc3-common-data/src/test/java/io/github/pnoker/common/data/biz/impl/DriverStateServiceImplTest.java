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

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.github.pnoker.common.data.biz.DriverAlarmService;
import io.github.pnoker.common.data.cache.LocalCacheService;
import io.github.pnoker.common.data.dal.EntityStateManager;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.entity.dto.DriverStateDTO;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverStateServiceImplTest {

    @Mock
    private LocalCacheService localCacheService;

    @Mock
    private DriverAlarmService driverAlarmService;

    @Mock
    private EntityStateManager entityStateManager;

    @Mock
    private LambdaQueryChainWrapper<EntityStateDO> queryWrapper;

    @InjectMocks
    private DriverStateServiceImpl service;

    private DriverStateDTO heartbeat(Long driverId, String status, Long tenantId) {
        DriverStateDTO dto = new DriverStateDTO();
        dto.setDriverId(driverId);
        dto.setStatus(status);
        dto.setTenantId(tenantId);
        return dto;
    }

    @Test
    void nullDtoDoesNothing() {
        service.heartbeat(null);
        verify(entityStateManager, never()).saveOrUpdate(any());
        verify(localCacheService, never()).setKey(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void nullDriverIdDoesNothing() {
        DriverStateDTO dto = new DriverStateDTO();
        dto.setStatus("online");
        service.heartbeat(dto);
        verify(entityStateManager, never()).saveOrUpdate(any());
    }

    @Test
    void newDriverCreatesDbRow() {
        when(localCacheService.getKey(anyString())).thenReturn(null);
        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.one()).thenReturn(null);
        when(entityStateManager.saveOrUpdate(any())).thenReturn(true);

        service.heartbeat(heartbeat(1L, "online", 100L));

        ArgumentCaptor<EntityStateDO> captor = ArgumentCaptor.forClass(EntityStateDO.class);
        verify(entityStateManager).saveOrUpdate(captor.capture());

        EntityStateDO saved = captor.getValue();
        assertThat(saved.getEntityTypeFlag()).isEqualTo((byte) EntityTypeFlagEnum.DRIVER.getIndex());
        assertThat(saved.getEntityId()).isEqualTo(1L);
        assertThat(saved.getDriverId()).isEqualTo(1L);
        assertThat(saved.getTenantId()).isEqualTo(100L);
        assertThat(saved.getLeaseVersion()).isEqualTo(1L);
        assertThat(saved.getStateFlag()).isEqualTo((byte) DriverStatusEnum.ONLINE.getIndex());
        assertThat(saved.getTtlSeconds()).isEqualTo(45);
        assertThat(saved.getExpireTime()).isAfter(LocalDateTime.now().plusSeconds(40));

        verify(localCacheService).setKey(anyString(), eq("online"), eq(45L), eq(TimeUnit.SECONDS));
    }

    @Test
    void existingDriverIncrementsLeaseVersion() {
        EntityStateDO existing = new EntityStateDO();
        existing.setEntityTypeFlag((byte) EntityTypeFlagEnum.DRIVER.getIndex());
        existing.setEntityId(1L);
        existing.setLeaseVersion(5L);

        when(localCacheService.getKey(anyString())).thenReturn("online");
        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.one()).thenReturn(existing);
        when(entityStateManager.saveOrUpdate(any())).thenReturn(true);

        service.heartbeat(heartbeat(1L, "online", 100L));

        ArgumentCaptor<EntityStateDO> captor = ArgumentCaptor.forClass(EntityStateDO.class);
        verify(entityStateManager).saveOrUpdate(captor.capture());
        assertThat(captor.getValue().getLeaseVersion()).isEqualTo(6L);
    }

    @Test
    void statusFlipFromOnlineToOfflineTriggersAlarm() {
        when(localCacheService.getKey(anyString())).thenReturn("online");
        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.one()).thenReturn(null);
        when(entityStateManager.saveOrUpdate(any())).thenReturn(true);

        service.heartbeat(heartbeat(1L, "offline", 100L));

        verify(driverAlarmService).alarm(any());
    }

    @Test
    void sameStatusNoFlipDoesNotTriggerAlarm() {
        when(localCacheService.getKey(anyString())).thenReturn("online");
        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.one()).thenReturn(null);
        when(entityStateManager.saveOrUpdate(any())).thenReturn(true);

        service.heartbeat(heartbeat(1L, "online", 100L));

        verify(driverAlarmService, never()).alarm(any());
    }
}
