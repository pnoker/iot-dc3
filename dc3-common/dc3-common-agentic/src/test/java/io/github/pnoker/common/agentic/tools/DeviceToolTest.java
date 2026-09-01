package io.github.pnoker.common.agentic.tools;

import io.github.pnoker.common.agentic.entity.model.AgenticToolResult;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.api.PointValueFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointValueBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceToolTest {
    @Mock private DeviceFacade deviceFacade;
    @Mock private PointFacade pointFacade;
    @Mock private PointValueFacade pointValueFacade;
    private DeviceTool tool;

    @BeforeEach
    void setUp() {
        tool = new DeviceTool(deviceFacade, pointFacade, pointValueFacade);
    }

    @Test
    void reactiveSearchUsesOffsetQueryAndTenantScope() {
        FacadeDeviceBO device = device(201L, "Edge Gateway A1", 101L);
        when(deviceFacade.listReactive(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.just(OffsetPage.of(List.of(device), 20, 10, 21)));
        StepVerifier.create(tool.searchDevicesReactive("Edge", null, 101L, 20, 10, toolContext()))
                .assertNext(result -> {
                    assertThat(result.success()).isTrue();
                    assertThat(result.data().offset()).isEqualTo(20);
                    assertThat(result.data().items()).containsExactly(device);
                }).verifyComplete();
        org.mockito.Mockito.verify(deviceFacade).listReactive(org.mockito.ArgumentMatchers.argThat(query -> query.tenantId().equals(11L)));
    }

    @Test
    void reactiveLookupRejectsInvalidIdWithoutFacadeCall() {
        StepVerifier.create(tool.lookupDeviceByIdReactive(0L, toolContext()))
                .assertNext(result -> assertThat(result.code()).isEqualTo(AgenticConstant.ToolResult.CODE_INVALID_ARGUMENT))
                .verifyComplete();
        org.mockito.Mockito.verifyNoInteractions(deviceFacade);
    }

    @Test
    void reactiveBatchLookupNormalizesIds() {
        when(deviceFacade.listByIdsReactive(11L, List.of(1L, 2L))).thenReturn(Flux.just(device(1L, "A", 9L), device(2L, "B", 9L)));
        StepVerifier.create(tool.lookupDevicesByIdsReactive(Arrays.asList(null, 1L, 1L, -1L, 2L), toolContext()))
                .assertNext(result -> assertThat(result.data()).hasSize(2)).verifyComplete();
    }

    @Test
    void latestSnapshotCombinesPointAndValueStreams() {
        FacadeDeviceBO device = device(201L, "Gateway", 101L);
        FacadePointBO point = new FacadePointBO(); point.setId(301L); point.setPointName("Temperature");
        FacadePointValueBO value = FacadePointValueBO.builder().deviceId(201L).pointId(301L).value("23.7").build();
        when(deviceFacade.getByIdReactive(11L, 201L)).thenReturn(Mono.just(device));
        when(pointFacade.listReactive(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.just(OffsetPage.of(List.of(point), 0, 5, 1)));
        when(pointValueFacade.lastValue(11L, 201L, 301L)).thenReturn(Mono.just(value));
        StepVerifier.create(tool.getDeviceLatestPointValuesReactive(201L, 5, toolContext()))
                .assertNext(result -> assertThat(result.data().points()).singleElement().extracting(DeviceTool.PointLatestValue::value).isEqualTo(value))
                .verifyComplete();
    }

    private FacadeDeviceBO device(Long id, String name, Long driverId) {
        FacadeDeviceBO device = new FacadeDeviceBO(); device.setId(id); device.setDeviceName(name);
        device.setDriverId(driverId); device.setEnableFlag(EnableFlagEnum.ENABLE); device.setTenantId(11L); return device;
    }

    private ToolContext toolContext() {
        Map<String, Object> values = new HashMap<>(); values.put(AgenticConstant.ToolContextKey.TENANT_ID, 11L);
        values.put(AgenticConstant.ToolContextKey.USER_ID, 22L); values.put(AgenticConstant.ToolContextKey.CONVERSATION_ID, "conv-1");
        return new ToolContext(values);
    }
}
