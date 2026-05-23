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
package io.github.pnoker.common.agentic.tools;

import io.github.pnoker.common.agentic.entity.model.AgenticToolResult;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.api.PointValueFacade;
import io.github.pnoker.common.facade.api.StatusHealthFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointValueBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceQuery;
import io.github.pnoker.common.facade.entity.query.FacadePointQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceToolTest {

    @Mock
    private DeviceFacade deviceFacade;

    @Mock
    private PointFacade pointFacade;

    @Mock
    private PointValueFacade pointValueFacade;

    @Mock
    private StatusHealthFacade statusHealthFacade;

    private DeviceTool tool;

    @BeforeEach
    void setUp() {
        tool = new DeviceTool(deviceFacade, pointFacade, pointValueFacade, Optional.of(statusHealthFacade));
    }

    @Test
    void searchDevicesUsesTenantScopedQueryAndReturnsStructuredPage() {
        FacadeDeviceBO device = device(201L, "Edge Gateway A1", 101L);
        FacadePage<FacadeDeviceBO> page = new FacadePage<>(1L, 20L, 1L, 1L, List.of(device));
        when(deviceFacade.listByPage(org.mockito.ArgumentMatchers.any(FacadeDeviceQuery.class))).thenReturn(page);

        AgenticToolResult<FacadePage<FacadeDeviceBO>> result = tool.searchDevices(
                "Edge Gateway", "edge-gateway-a1", 101L, 1, 20, toolContext());

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo(AgenticConstant.ToolResult.CODE_OK);
        assertThat(result.data().getRecords()).extracting(FacadeDeviceBO::getDeviceName)
                .containsExactly("Edge Gateway A1");

        ArgumentCaptor<FacadeDeviceQuery> captor = forClass(FacadeDeviceQuery.class);
        org.mockito.Mockito.verify(deviceFacade).listByPage(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(11L);
        assertThat(captor.getValue().getDeviceName()).isEqualTo("Edge Gateway");
        assertThat(captor.getValue().getDeviceCode()).isEqualTo("edge-gateway-a1");
        assertThat(captor.getValue().getDriverId()).isEqualTo(101L);
        assertThat(captor.getValue().getPage().getSize()).isEqualTo(20L);
    }

    @Test
    void getDeviceLatestPointValuesCombinesDevicePointMetadataAndLatestValues() {
        FacadeDeviceBO device = device(201L, "Edge Gateway A1", 101L);
        FacadePointBO temperature = point(301L, "Ambient Temperature");
        FacadePointValueBO latestValue = FacadePointValueBO.builder()
                .deviceId(201L)
                .pointId(301L)
                .value("23.7")
                .rawValue("23.68")
                .numValue(23.7D)
                .createTime(1_780_000_000L)
                .build();
        when(deviceFacade.getById(11L, 201L)).thenReturn(device);
        when(pointFacade.listByPage(org.mockito.ArgumentMatchers.any(FacadePointQuery.class)))
                .thenReturn(new FacadePage<>(1L, 5L, 1L, 1L, List.of(temperature)));
        when(pointValueFacade.lastValue(11L, 201L, 301L)).thenReturn(latestValue);

        AgenticToolResult<DeviceTool.DeviceLatestPointValues> result = tool.getDeviceLatestPointValues(
                201L, 5, toolContext());

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo(AgenticConstant.ToolResult.CODE_OK);
        assertThat(result.data().device().getDeviceName()).isEqualTo("Edge Gateway A1");
        assertThat(result.data().points()).hasSize(1);
        assertThat(result.data().points().get(0).point().getPointName()).isEqualTo("Ambient Temperature");
        assertThat(result.data().points().get(0).value().getValue()).isEqualTo("23.7");

        ArgumentCaptor<FacadePointQuery> captor = forClass(FacadePointQuery.class);
        org.mockito.Mockito.verify(pointFacade).listByPage(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(11L);
        assertThat(captor.getValue().getDeviceId()).isEqualTo(201L);
        assertThat(captor.getValue().getPage().getSize()).isEqualTo(5L);
    }

    @Test
    void getDeviceStatusesReturnsUnavailableWhenStatusFacadeIsAbsent() {
        DeviceTool unavailableTool = new DeviceTool(deviceFacade, pointFacade, pointValueFacade, Optional.empty());

        AgenticToolResult<Map<Long, String>> result = unavailableTool.getDeviceStatusesByIds(
                List.of(201L), toolContext());

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(AgenticConstant.ToolResult.CODE_UNAVAILABLE);
        assertThat(result.data()).isNull();
    }

    private FacadeDeviceBO device(Long id, String name, Long driverId) {
        FacadeDeviceBO device = new FacadeDeviceBO();
        device.setId(id);
        device.setDeviceName(name);
        device.setDeviceCode(name.toLowerCase().replace(' ', '-'));
        device.setDriverId(driverId);
        device.setEnableFlag(EnableFlagEnum.ENABLE);
        device.setTenantId(11L);
        device.setProfileId(401L);
        return device;
    }

    private FacadePointBO point(Long id, String name) {
        FacadePointBO point = new FacadePointBO();
        point.setId(id);
        point.setPointName(name);
        point.setPointCode(name.toLowerCase().replace(' ', '-'));
        point.setProfileId(401L);
        point.setTenantId(11L);
        point.setUnit("C");
        return point;
    }

    private ToolContext toolContext() {
        Map<String, Object> values = new HashMap<>();
        values.put(AgenticConstant.ToolContextKey.TENANT_ID, 11L);
        values.put(AgenticConstant.ToolContextKey.USER_ID, 22L);
        values.put(AgenticConstant.ToolContextKey.CONVERSATION_ID, "11:22:conv-1");
        return new ToolContext(values);
    }

}
