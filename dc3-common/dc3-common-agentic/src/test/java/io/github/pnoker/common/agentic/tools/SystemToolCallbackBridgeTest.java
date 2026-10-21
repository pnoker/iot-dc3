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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.agentic.service.runtime.ReactiveAgenticTool;
import io.github.pnoker.common.agentic.service.runtime.ReactiveAgenticToolCallback;
import io.github.pnoker.common.agentic.service.runtime.ReactiveAgenticToolRegistry;
import io.github.pnoker.common.facade.api.StatusHealthFacade;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/**
 * Reproduces the Spring AI bridge behavior for the SystemTool status methods the same
 * way ChatClientConfig assembles it in production: the reactive registry wrapped in
 * {@link ReactiveAgenticToolCallback} bridges.
 */
@ExtendWith(MockitoExtension.class)
class SystemToolCallbackBridgeTest {

    @Mock
    private StatusHealthFacade statusHealthFacade;

    private ReactiveAgenticToolRegistry registryWith(SystemTool systemTool) {
        return new ReactiveAgenticToolRegistry(
                mock(PointValueTool.class),
                mock(PointTool.class),
                mock(ProfileTool.class),
                mock(CommandTool.class),
                mock(EventTool.class),
                mock(DeviceTool.class),
                mock(DriverTool.class),
                mock(InsightTool.class),
                mock(HistoryTool.class),
                mock(TenantTool.class),
                mock(UserTool.class),
                systemTool,
                new ObjectMapper(), null);
    }

    @Test
    void deviceStatusesCallbackReturnsTheStatusMapThroughSpringAi() {
        when(statusHealthFacade.listDeviceStatusesByIdsReactive(
                        org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(Mono.just(Map.of(8319267248317138751L, "online")));
        SystemTool systemTool = new SystemTool(Optional.of(statusHealthFacade));
        ReactiveAgenticToolRegistry registry = registryWith(systemTool);

        ReactiveAgenticTool deviceStatusesTool = registry.tools().get("getDeviceStatuses");
        ReactiveAgenticTool systemHealthTool = registry.tools().get("getSystemHealth");
        assertThat(deviceStatusesTool).as("getDeviceStatuses must be registered").isNotNull();
        assertThat(systemHealthTool).as("getSystemHealth must be registered").isNotNull();

        ToolCallback deviceStatuses = new ReactiveAgenticToolCallback(deviceStatusesTool);
        ToolContext context = new ToolContext(Map.of("dc3.agentic.tenantId", 1L));
        String result = deviceStatuses.call("{\"deviceIds\":[8319267248317138751]}", context);
        assertThat(result).as("status map must be keyed by device id").contains("8319267248317138751");
    }

    @Test
    void oversizedIdListsAreBoundedAndAnnotated() {
        when(statusHealthFacade.listDeviceStatusesByIdsReactive(
                        org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.anyCollection()))
                .thenAnswer(invocation -> Mono.just(Map.of(1L, "online")));
        SystemTool systemTool = new SystemTool(Optional.of(statusHealthFacade));
        ReactiveAgenticToolRegistry registry = registryWith(systemTool);

        StringBuilder ids = new StringBuilder("[");
        for (long id = 1; id <= 60; id++) {
            if (id > 1) {
                ids.append(',');
            }
            ids.append(id);
        }
        ids.append(']');
        String result = registry
                .tools()
                .get("getDeviceStatuses")
                .call("{\"deviceIds\":" + ids + "}", new ToolContext(Map.of("dc3.agentic.tenantId", 1L)))
                .block(java.time.Duration.ofSeconds(5))
                .toString();
        assertThat(result)
                .as("the result must tell the model the id list was truncated")
                .contains("first 50 of 60");
    }
}
