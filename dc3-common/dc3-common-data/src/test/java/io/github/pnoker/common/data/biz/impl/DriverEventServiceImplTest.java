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

import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.data.cache.LocalCacheService;
import io.github.pnoker.common.data.dal.DriverEventManager;
import io.github.pnoker.common.data.entity.model.DriverEventDO;
import io.github.pnoker.common.entity.dto.DriverEventDTO;
import io.github.pnoker.common.enums.DriverEventTypeEnum;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.utils.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverEventServiceImplTest {

    @Mock
    private LocalCacheService localCacheService;

    @Mock
    private DriverEventManager driverEventManager;

    @InjectMocks
    private DriverEventServiceImpl service;

    private static DriverEventDTO heartbeatPayload(DriverStatusEnum status) {
        DriverEventDTO.DriverStatus payload = new DriverEventDTO.DriverStatus(7L, status);
        payload.setTenantId(1L);
        DriverEventDTO dto = new DriverEventDTO();
        dto.setContent(JsonUtil.toJsonString(payload));
        return dto;
    }

    @Test
    void heartbeatIgnoresPayloadWithoutDriverId() {
        DriverEventDTO.DriverStatus payload = new DriverEventDTO.DriverStatus(null, DriverStatusEnum.ONLINE);
        DriverEventDTO dto = new DriverEventDTO();
        dto.setContent(JsonUtil.toJsonString(payload));
        service.heartbeatEvent(dto);
        verifyNoInteractions(localCacheService, driverEventManager);
    }

    @Test
    void heartbeatRefreshesCacheWithoutPersistingOnFirstSeen() {
        when(localCacheService.getKey(PrefixConstant.DRIVER_STATUS_KEY_PREFIX + 7L)).thenReturn(null);
        service.heartbeatEvent(heartbeatPayload(DriverStatusEnum.ONLINE));
        verify(localCacheService).setKey(
                org.mockito.ArgumentMatchers.eq(PrefixConstant.DRIVER_STATUS_KEY_PREFIX + 7L),
                org.mockito.ArgumentMatchers.eq(DriverStatusEnum.ONLINE.getCode()),
                org.mockito.ArgumentMatchers.eq(45L),
                org.mockito.ArgumentMatchers.<java.util.concurrent.TimeUnit>any());
        verify(driverEventManager, never()).save(any(DriverEventDO.class));
    }

    @Test
    void heartbeatPersistsAlarmOnOnlineToOfflineFlip() {
        when(localCacheService.getKey(PrefixConstant.DRIVER_STATUS_KEY_PREFIX + 7L))
                .thenReturn(DriverStatusEnum.ONLINE.getCode());
        service.heartbeatEvent(heartbeatPayload(DriverStatusEnum.OFFLINE));

        ArgumentCaptor<DriverEventDO> captor = ArgumentCaptor.forClass(DriverEventDO.class);
        verify(driverEventManager).save(captor.capture());
        assertThat(captor.getValue().getEventTypeFlag()).isEqualTo(DriverEventTypeEnum.ALARM.getIndex());
        assertThat(captor.getValue().getDriverId()).isEqualTo(7L);
    }

    @Test
    void heartbeatDoesNotPersistOnIntraFamilyFlip() {
        when(localCacheService.getKey(PrefixConstant.DRIVER_STATUS_KEY_PREFIX + 7L))
                .thenReturn(DriverStatusEnum.OFFLINE.getCode());
        // OFFLINE -> FAULT is within the unavailable family — no derived ALARM
        service.heartbeatEvent(heartbeatPayload(DriverStatusEnum.FAULT));
        verify(driverEventManager, never()).save(any(DriverEventDO.class));
    }

    @Test
    void alarmEventDropsPayloadWithoutDriverId() {
        DriverEventDTO.DriverStatus payload = new DriverEventDTO.DriverStatus(null, DriverStatusEnum.OFFLINE);
        DriverEventDTO dto = new DriverEventDTO();
        dto.setContent(JsonUtil.toJsonString(payload));
        service.alarmEvent(dto);
        verifyNoInteractions(driverEventManager);
    }

    @Test
    void alarmEventPersistsRowWithSuppliedTenant() {
        DriverEventDTO.DriverStatus payload = new DriverEventDTO.DriverStatus(7L, DriverStatusEnum.OFFLINE);
        payload.setTenantId(1L);
        payload.setMessage("network down");
        DriverEventDTO dto = new DriverEventDTO();
        dto.setContent(JsonUtil.toJsonString(payload));

        service.alarmEvent(dto);

        ArgumentCaptor<DriverEventDO> captor = ArgumentCaptor.forClass(DriverEventDO.class);
        verify(driverEventManager).save(captor.capture());
        assertThat(captor.getValue().getEventTypeFlag()).isEqualTo(DriverEventTypeEnum.ALARM.getIndex());
        assertThat(captor.getValue().getTenantId()).isEqualTo(1L);
    }
}
