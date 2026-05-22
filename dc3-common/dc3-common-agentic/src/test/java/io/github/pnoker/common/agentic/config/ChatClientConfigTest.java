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
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.api.PointCommandFacade;
import io.github.pnoker.common.facade.api.PointValueFacade;
import io.github.pnoker.common.facade.api.ProfileFacade;
import io.github.pnoker.common.facade.api.StatusHealthFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
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

    @BeforeEach
    void setUp() {
        ChatClientConfig config = new ChatClientConfig();
        TenantTool tenantTool = new TenantTool();
        UserTool userTool = new UserTool();
        DeviceTool deviceTool = new DeviceTool(deviceFacade, pointFacade, pointValueFacade,
                Optional.of(statusHealthFacade));
        DriverTool driverTool = new DriverTool(driverFacade, Optional.of(statusHealthFacade));
        ProfileTool profileTool = new ProfileTool(Optional.of(profileFacade));
        PointTool pointTool = new PointTool(pointFacade);
        PointValueTool pointValueTool = new PointValueTool(pointValueFacade, pointCommandFacade, actionService);
        SystemTool systemTool = new SystemTool(Optional.of(statusHealthFacade));
        provider = config.agenticToolCallbackProvider(tenantTool, userTool, deviceTool, driverTool, profileTool,
                pointTool, pointValueTool, systemTool, new ObjectMapper());
    }

    @Test
    void agenticToolCallbackProviderRegistersExpectedTools() {
        assertThat(toolNames()).contains(
                "getCurrentTenantInfo",
                "getCurrentUserProfile",
                "lookupDeviceById",
                "lookupDevicesByIds",
                "searchDevices",
                "listDevicesByDriverId",
                "listDevicesByProfileId",
                "lookupDriverById",
                "lookupDriversByIds",
                "lookupDriverByDeviceId",
                "searchDrivers",
                "lookupPointById",
                "lookupPointsByIds",
                "searchPoints",
                "listPointsByDeviceId",
                "listPointsByProfileId",
                "getLatestPointValue",
                "getPointValueHistory",
                "getDeviceLatestPointValues",
                "readPointValue",
                "writePointValue",
                "lookupProfileById",
                "lookupProfilesByIds",
                "searchProfiles",
                "listProfilesByDeviceId",
                "getDeviceStatusesByIds",
                "getDeviceStatusesByProfileId",
                "getDriverStatusesByIds",
                "getDriverDeviceStatusSummary",
                "getSystemHealth");
    }

    @Test
    void agenticToolCallAdvisorRunsAfterMemoryAdvisor() {
        ChatClientConfig config = new ChatClientConfig();
        assertThat(config.agenticToolCallAdvisor(ToolCallingManager.builder().build()))
                .isInstanceOf(ToolCallAdvisor.class)
                .extracting(ToolCallAdvisor.class::cast)
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
