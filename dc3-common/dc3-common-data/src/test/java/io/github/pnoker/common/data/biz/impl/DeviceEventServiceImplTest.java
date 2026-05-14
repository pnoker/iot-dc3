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
import io.github.pnoker.common.data.dal.DeviceEventManager;
import io.github.pnoker.common.data.entity.model.DeviceEventDO;
import io.github.pnoker.common.entity.dto.DeviceEventDTO;
import io.github.pnoker.common.enums.DeviceEventTypeEnum;
import io.github.pnoker.common.enums.DeviceStatusEnum;
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
class DeviceEventServiceImplTest {

    @Mock
    private LocalCacheService localCacheService;

    @Mock
    private DeviceEventManager deviceEventManager;

    @InjectMocks
    private DeviceEventServiceImpl service;

    private static DeviceEventDTO heartbeatPayload(DeviceStatusEnum status) {
        DeviceEventDTO.DeviceStatus payload = new DeviceEventDTO.DeviceStatus(10L, status);
        payload.setTenantId(1L);
        DeviceEventDTO dto = new DeviceEventDTO();
        dto.setContent(JsonUtil.toJsonString(payload));
        return dto;
    }

    @Test
    void heartbeatIgnoresPayloadWithoutDeviceId() {
        DeviceEventDTO.DeviceStatus payload = new DeviceEventDTO.DeviceStatus(null, DeviceStatusEnum.ONLINE);
        DeviceEventDTO dto = new DeviceEventDTO();
        dto.setContent(JsonUtil.toJsonString(payload));
        service.heartbeatEvent(dto);
        verifyNoInteractions(localCacheService, deviceEventManager);
    }

    @Test
    void heartbeatRefreshesCacheWithoutPersistingOnFirstSeen() {
        when(localCacheService.getKey(PrefixConstant.DEVICE_STATUS_KEY_PREFIX + 10L)).thenReturn(null);
        service.heartbeatEvent(heartbeatPayload(DeviceStatusEnum.ONLINE));

        verify(localCacheService).setKey(
                org.mockito.ArgumentMatchers.eq(PrefixConstant.DEVICE_STATUS_KEY_PREFIX + 10L),
                org.mockito.ArgumentMatchers.eq(DeviceStatusEnum.ONLINE.getCode()),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.<java.util.concurrent.TimeUnit>any());
        verify(deviceEventManager, never()).save(any(DeviceEventDO.class));
    }

    @Test
    void heartbeatPersistsAlarmOnOnlineToOfflineFlip() {
        when(localCacheService.getKey(PrefixConstant.DEVICE_STATUS_KEY_PREFIX + 10L))
                .thenReturn(DeviceStatusEnum.ONLINE.getCode());
        service.heartbeatEvent(heartbeatPayload(DeviceStatusEnum.OFFLINE));

        ArgumentCaptor<DeviceEventDO> captor = ArgumentCaptor.forClass(DeviceEventDO.class);
        verify(deviceEventManager).save(captor.capture());
        assertThat(captor.getValue().getDeviceId()).isEqualTo(10L);
        assertThat(captor.getValue().getEventTypeFlag()).isEqualTo(DeviceEventTypeEnum.ALARM.getIndex());
        assertThat(captor.getValue().getTenantId()).isEqualTo(1L);
    }

    @Test
    void heartbeatDoesNotPersistOnIntraFamilyFlip() {
        when(localCacheService.getKey(PrefixConstant.DEVICE_STATUS_KEY_PREFIX + 10L))
                .thenReturn(DeviceStatusEnum.ONLINE.getCode());
        // ONLINE -> MAINTAIN is within the "available" family — no derived ALARM
        service.heartbeatEvent(heartbeatPayload(DeviceStatusEnum.MAINTAIN));
        verify(deviceEventManager, never()).save(any(DeviceEventDO.class));
    }

    @Test
    void heartbeatPersistsAlarmOnUnavailableFlip() {
        when(localCacheService.getKey(PrefixConstant.DEVICE_STATUS_KEY_PREFIX + 10L))
                .thenReturn(DeviceStatusEnum.OFFLINE.getCode());
        service.heartbeatEvent(heartbeatPayload(DeviceStatusEnum.ONLINE));
        verify(deviceEventManager).save(any(DeviceEventDO.class));
    }

    @Test
    void alarmEventPersistsRowEvenWhenMessageIsBlank() {
        DeviceEventDTO.DeviceStatus payload = new DeviceEventDTO.DeviceStatus(10L, DeviceStatusEnum.OFFLINE);
        payload.setTenantId(1L);
        payload.setMessage(null);
        DeviceEventDTO dto = new DeviceEventDTO();
        dto.setContent(JsonUtil.toJsonString(payload));

        service.alarmEvent(dto);

        ArgumentCaptor<DeviceEventDO> captor = ArgumentCaptor.forClass(DeviceEventDO.class);
        verify(deviceEventManager).save(captor.capture());
        assertThat(captor.getValue().getEventTypeFlag()).isEqualTo(DeviceEventTypeEnum.ALARM.getIndex());
        assertThat(captor.getValue().getTenantId()).isEqualTo(1L);
    }

    @Test
    void alarmEventDropsPayloadWithoutDeviceId() {
        DeviceEventDTO.DeviceStatus payload = new DeviceEventDTO.DeviceStatus(null, DeviceStatusEnum.OFFLINE);
        DeviceEventDTO dto = new DeviceEventDTO();
        dto.setContent(JsonUtil.toJsonString(payload));
        service.alarmEvent(dto);
        verifyNoInteractions(deviceEventManager);
    }
}
