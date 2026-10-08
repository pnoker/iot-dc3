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
import static org.mockito.Mockito.when;

import io.github.pnoker.common.facade.api.StatusHealthFacade;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import reactor.core.publisher.Mono;

/**
 * Reproduces the Spring AI @Tool bridge behavior for the SystemTool status methods the
 * same way ChatClientConfig assembles it in production.
 */
@ExtendWith(MockitoExtension.class)
class SystemToolCallbackBridgeTest {

    @Mock
    private StatusHealthFacade statusHealthFacade;

    @Test
    void deviceStatusesCallbackReturnsTheStatusMapThroughSpringAi() {
        when(statusHealthFacade.listDeviceStatusesByIdsReactive(
                        org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(Mono.just(Map.of(8319267248317138751L, "online")));
        SystemTool systemTool = new SystemTool(Optional.of(statusHealthFacade));
        ToolCallbackProvider provider =
                MethodToolCallbackProvider.builder().toolObjects(systemTool).build();

        ToolCallback deviceStatuses = null;
        ToolCallback systemHealth = null;
        for (ToolCallback callback : provider.getToolCallbacks()) {
            String name = callback.getToolDefinition().name();
            System.out.println("[bridge] callback: " + name);
            if ("getDeviceStatuses".equals(name)) deviceStatuses = callback;
            if ("getSystemHealth".equals(name)) systemHealth = callback;
        }
        assertThat(deviceStatuses).as("getDeviceStatuses callback must exist").isNotNull();
        assertThat(systemHealth).as("getSystemHealth callback must exist").isNotNull();

        ToolContext context = new ToolContext(Map.of("dc3.agentic.tenantId", 1L));
        String result = deviceStatuses.call("{\"deviceIds\":[8319267248317138751]}", context);
        System.out.println("[bridge] getDeviceStatuses raw result: " + result);
        assertThat(result).as("status map must be keyed by device id").contains("8319267248317138751");
    }
}
