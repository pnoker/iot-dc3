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

package io.github.pnoker.common.agentic.config;

import io.github.pnoker.common.agentic.service.ActionService;
import io.github.pnoker.common.agentic.tools.DeviceTool;
import io.github.pnoker.common.agentic.tools.DriverTool;
import io.github.pnoker.common.agentic.tools.PointTool;
import io.github.pnoker.common.agentic.tools.PointValueTool;
import io.github.pnoker.common.agentic.tools.ProfileTool;
import io.github.pnoker.common.agentic.tools.SystemTool;
import io.github.pnoker.common.agentic.tools.TenantTool;
import io.github.pnoker.common.agentic.tools.UserTool;
import io.github.pnoker.common.agentic.service.runtime.ReactiveAgenticToolRegistry;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.api.PointCommandFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.api.PointValueFacade;
import io.github.pnoker.common.facade.api.ProfileFacade;
import io.github.pnoker.common.facade.api.StatusHealthFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks the Spring AI tool registration surface used by the agentic chat flow.
 */
@ExtendWith(MockitoExtension.class)
class ChatClientConfigTest {

    @Mock
    private DeviceFacade deviceFacade;

    @Mock
    private DriverFacade driverFacade;

    @Mock
    private PointFacade pointFacade;

    @Mock
    private PointValueFacade pointValueFacade;

    @Mock
    private PointCommandFacade pointCommandFacade;

    @Mock
    private ActionService actionService;

    @Mock
    private ProfileFacade profileFacade;

    @Mock
    private StatusHealthFacade statusHealthFacade;

    private ToolCallbackProvider provider;

    private ReactiveAgenticToolRegistry reactiveToolRegistry;

    @BeforeEach
    void setUp() {
        ChatClientConfig config = new ChatClientConfig();
        TenantTool tenantTool = new TenantTool();
        UserTool userTool = new UserTool();
        DeviceTool deviceTool = new DeviceTool(deviceFacade, pointFacade, pointValueFacade);
        DriverTool driverTool = new DriverTool(driverFacade);
        ProfileTool profileTool = new ProfileTool(Optional.of(profileFacade));
        PointTool pointTool = new PointTool(pointFacade);
        PointValueTool pointValueTool = new PointValueTool(pointValueFacade, pointCommandFacade, actionService);
        reactiveToolRegistry = new ReactiveAgenticToolRegistry(pointValueTool, new ObjectMapper());
        SystemTool systemTool = new SystemTool(Optional.of(statusHealthFacade));
        provider = config.agenticToolCallbackProvider(tenantTool, userTool, deviceTool, driverTool, profileTool,
                pointTool, pointValueTool, systemTool, new ObjectMapper());
    }

    @Test
    void agenticToolCallbackProviderRegistersExpectedTools() {
        assertThat(toolNames()).contains(
                "getCurrentTenantInfo",
                "getCurrentUserProfile",
                "getSystemHealth");
    }

    @Test
    void reactiveToolRegistryRegistersWritePointValueOnlyOnce() {
        assertThat(reactiveToolRegistry.tools()).containsOnlyKeys("writePointValue", "readPointValue",
                "getLatestPointValue", "getPointValueHistory");
        var tool = reactiveToolRegistry.tools().get("writePointValue");
        assertThat(tool.definition().name()).isEqualTo("writePointValue");
        assertThat(tool.definition().inputSchema()).contains("deviceId", "pointId", "value");
    }

    @Test
    void reactiveToolRegistryRegistersProfileOffsetTools() {
        ProfileTool profileTool = new ProfileTool(Optional.of(profileFacade));
        ReactiveAgenticToolRegistry registry = new ReactiveAgenticToolRegistry(null, null, profileTool,
                new ObjectMapper());
        assertThat(registry.tools()).containsOnlyKeys("lookupProfileById", "lookupProfilesByIds",
                "searchProfiles", "listProfilesByDeviceId");
        assertThat(registry.tools().get("searchProfiles").definition().inputSchema())
                .contains("offset", "limit");
    }

    @Test
    void agenticToolCallAdvisorRunsAfterMemoryAdvisor() {
        ChatClientConfig config = new ChatClientConfig();
        assertThat(config.agenticToolCallAdvisor(ToolCallingManager.builder().build()))
                .isInstanceOf(ToolCallingAdvisor.class)
                .extracting(ToolCallingAdvisor.class::cast)
                .satisfies(advisor -> {
                    assertThat(advisor.getName()).isEqualTo("Tool Calling Advisor");
                    assertThat(advisor.getOrder()).isEqualTo(Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER + 100);
                });
    }

    private Set<String> toolNames() {
        ToolCallback[] callbacks = provider.getToolCallbacks();
        assertThat(callbacks).isNotEmpty();
        Set<String> names = new LinkedHashSet<>(Arrays.stream(callbacks)
                .map(callback -> callback.getToolDefinition().name())
                .toList());
        assertThat(names).hasSameSizeAs(callbacks);
        return names;
    }

}
