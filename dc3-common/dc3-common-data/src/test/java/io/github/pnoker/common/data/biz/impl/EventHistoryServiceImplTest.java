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

import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.dal.EventHistoryManager;
import io.github.pnoker.common.data.entity.builder.EventHistoryBuilder;
import io.github.pnoker.common.data.entity.model.EventHistoryDO;
import io.github.pnoker.common.data.entity.vo.EventReportVO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EventLevelEnum;
import io.github.pnoker.common.enums.EventTypeFlagEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.EventFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeEventBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeEventQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventHistoryServiceImplTest {

    @Mock
    private DeviceFacade deviceFacade;

    @Mock
    private EventFacade eventFacade;

    @Mock
    private AlarmRuleTriggerService alarmRuleTriggerService;

    @Mock
    private EventHistoryManager eventHistoryManager;

    @Mock
    private EventHistoryBuilder eventHistoryBuilder;

    private EventHistoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EventHistoryServiceImpl(deviceFacade, eventFacade, alarmRuleTriggerService, eventHistoryManager, eventHistoryBuilder);
    }

    @Test
    void reportResolvesEventByCodeWithinDeviceProfile() {
        Long tenantId = 100L;
        Long deviceId = 10L;
        Long eventId = 20L;

        FacadeDeviceBO device = new FacadeDeviceBO();
        device.setId(deviceId);
        device.setTenantId(tenantId);
        device.setProfileId(30L);
        device.setEnableFlag(EnableFlagEnum.ENABLE);

        FacadeEventBO event = new FacadeEventBO();
        event.setId(eventId);
        event.setTenantId(tenantId);
        event.setProfileId(30L);
        event.setEventCode("overheat");
        event.setEventTypeFlag(EventTypeFlagEnum.ALERT);
        event.setEventLevelFlag(EventLevelEnum.HIGH);
        event.setEnableFlag(EnableFlagEnum.ENABLE);

        EventReportVO report = new EventReportVO();
        report.setDeviceId(deviceId);
        report.setEventCode("overheat");
        report.setParamValues(Map.of("value", "90"));

        when(deviceFacade.getById(tenantId, deviceId)).thenReturn(device);
        when(eventFacade.listByPage(any(FacadeEventQuery.class)))
                .thenReturn(new FacadePage<>(1L, 1L, 1L, 1L, List.of(event)));
        when(eventHistoryManager.save(any(EventHistoryDO.class))).thenReturn(true);

        String recordId = service.report(tenantId, report);

        ArgumentCaptor<FacadeEventQuery> queryCaptor = ArgumentCaptor.forClass(FacadeEventQuery.class);
        verify(eventFacade).listByPage(queryCaptor.capture());
        assertThat(queryCaptor.getValue().getTenantId()).isEqualTo(tenantId);
        assertThat(queryCaptor.getValue().getProfileId()).isEqualTo(30L);
        assertThat(queryCaptor.getValue().getEventCode()).isEqualTo("overheat");

        ArgumentCaptor<EventHistoryDO> recordCaptor = ArgumentCaptor.forClass(EventHistoryDO.class);
        verify(eventHistoryManager).save(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getRecordId()).isEqualTo(recordId);
        assertThat(recordCaptor.getValue().getEventId()).isEqualTo(eventId);
        assertThat(recordCaptor.getValue().getEventCode()).isEqualTo("overheat");
    }

}
