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

package io.github.pnoker.common.agentic.service.direct;

import io.github.pnoker.common.agentic.context.AgenticRequestContext;
import io.github.pnoker.common.agentic.entity.request.DirectQueryRequest;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.api.PointValueFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointValueBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceQuery;
import io.github.pnoker.common.facade.entity.query.FacadeDriverQuery;
import io.github.pnoker.common.facade.entity.query.FacadePointQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataMonitorDirectBackendProviderTest {

    @Mock
    private DeviceFacade deviceFacade;

    @Mock
    private DriverFacade driverFacade;

    @Mock
    private PointFacade pointFacade;

    @Mock
    private PointValueFacade pointValueFacade;

    private DataMonitorDirectBackendProvider provider;

    private RequestHeader.UserHeader header;

    @BeforeEach
    void setUp() {
        provider = new DataMonitorDirectBackendProvider(deviceFacade, driverFacade, pointFacade, pointValueFacade);
        header = new RequestHeader.UserHeader();
        header.setTenantId(1L);
        header.setUserId(2L);
    }

    @Test
    void freeFormChatDoesNotTriggerPointValueDirectLookup() {
        FacadeDeviceBO device = device(10L, "East Pump 01", "PUMP-E01");
        when(deviceFacade.selectByPage(any(FacadeDeviceQuery.class))).thenReturn(page(List.of(device)));
        when(driverFacade.selectByPage(any(FacadeDriverQuery.class))).thenReturn(page(List.of(new FacadeDriverBO())));
        when(pointFacade.selectByPage(any(FacadePointQuery.class))).thenReturn(page(List.of(point(20L, "temperature",
                "TEMP"))));

        Queue<AgenticRequestContext.ToolEvent> events = new ConcurrentLinkedQueue<>();
        DirectBackendResult result = provider.build(null, header, events);

        assertThat(result.answer()).isNull();
        assertThat(result.context()).contains("Monitoring snapshot").contains("East Pump 01");
        assertThat(events).extracting(AgenticRequestContext.ToolEvent::toolName)
                .containsExactly("getMonitoringSnapshot");
        verify(pointValueFacade, never()).lastValue(any(), any(), any());
        verify(pointValueFacade, never()).history(any(), any(), any(), anyInt());
    }

    @Test
    void structuredPointValueQueryUsesExplicitIds() {
        DirectQueryRequest query = new DirectQueryRequest();
        query.setType(DirectQueryRequest.TYPE_POINT_VALUE);
        query.setDeviceId(10L);
        query.setPointId(20L);
        query.setLimit(3);

        when(deviceFacade.selectById(1L, 10L)).thenReturn(device(10L, "East Pump 01", "PUMP-E01"));
        when(pointFacade.selectById(1L, 20L)).thenReturn(point(20L, "temperature", "TEMP"));
        when(pointValueFacade.lastValue(1L, 10L, 20L)).thenReturn(FacadePointValueBO.builder()
                .deviceId(10L)
                .pointId(20L)
                .value("36.8")
                .rawValue("36.82")
                .createTime(1_779_000_000L)
                .build());
        when(pointValueFacade.history(1L, 10L, 20L, 3)).thenReturn(List.of("36.8", "36.7", "36.5"));

        DirectBackendResult result = provider.build(query, header, new ConcurrentLinkedQueue<>());

        assertThat(result.answer().title()).isEqualTo("位号数据查询结果");
        assertThat(result.answer().fields())
                .extracting(DirectAnswer.Field::value)
                .anySatisfy(value -> assertThat(value).contains("East Pump 01"))
                .anySatisfy(value -> assertThat(value).contains("temperature"))
                .anySatisfy(value -> assertThat(value).contains("36.8"));
        assertThat(result.answer().tables()).hasSize(1);
        assertThat(result.answer().tables().getFirst().title()).contains("最新 3 条历史值");
        assertThat(result.answer().charts()).hasSize(1);
        verify(deviceFacade).selectById(1L, 10L);
        verify(pointFacade).selectById(1L, 20L);
    }

    @Test
    void structuredPointValueQueryFailsClosedWhenSelectorIsMissing() {
        DirectQueryRequest query = new DirectQueryRequest();
        query.setType(DirectQueryRequest.TYPE_POINT_VALUE);
        query.setDeviceId(10L);

        DirectBackendResult result = provider.build(query, header, new ConcurrentLinkedQueue<>());

        assertThat(result.answer().message()).contains("确定性位号查询需要明确的设备选择器和位号选择器");
        verifyNoInteractions(deviceFacade, driverFacade, pointFacade, pointValueFacade);
    }

    private FacadeDeviceBO device(Long id, String name, String code) {
        FacadeDeviceBO device = new FacadeDeviceBO();
        device.setId(id);
        device.setTenantId(1L);
        device.setDeviceName(name);
        device.setDeviceCode(code);
        return device;
    }

    private FacadePointBO point(Long id, String name, String code) {
        FacadePointBO point = new FacadePointBO();
        point.setId(id);
        point.setTenantId(1L);
        point.setPointName(name);
        point.setPointCode(code);
        point.setUnit("C");
        return point;
    }

    private <T> FacadePage<T> page(List<T> records) {
        return new FacadePage<>(1L, records.size(), records.size(), 1L, records);
    }

}
