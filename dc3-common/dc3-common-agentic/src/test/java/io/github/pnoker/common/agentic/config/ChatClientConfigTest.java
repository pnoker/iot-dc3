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
import io.github.pnoker.common.agentic.tool.AuthToolSet;
import io.github.pnoker.common.agentic.tool.DataToolSet;
import io.github.pnoker.common.agentic.tool.ManagerToolSet;
import io.github.pnoker.common.agentic.tool.PlatformToolSet;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.api.PointValueCommandFacade;
import io.github.pnoker.common.facade.api.PointValueFacade;
import io.github.pnoker.common.facade.api.ProfileFacade;
import io.github.pnoker.common.facade.api.StatusHealthFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
    private PointValueCommandFacade pointValueCommandFacade;

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
        AuthToolSet authToolSet = new AuthToolSet();
        ManagerToolSet managerToolSet = new ManagerToolSet(deviceFacade, driverFacade, pointFacade);
        DataToolSet dataToolSet = new DataToolSet(pointValueFacade, pointValueCommandFacade, deviceFacade, pointFacade,
                actionService);
        PlatformToolSet platformToolSet = new PlatformToolSet(Optional.of(profileFacade),
                Optional.of(statusHealthFacade));
        provider = config.agenticToolCallbackProvider(authToolSet, managerToolSet, dataToolSet, platformToolSet);
    }

    @Test
    void agenticToolCallbackProviderRegistersExpectedToolSets() {
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
    void skillToolNamesAreBackedByRegisteredCallbacks() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:skills/*.yml");
        Set<String> registeredToolNames = toolNames();
        Set<String> declaredToolNames = new LinkedHashSet<>();
        Yaml yaml = new Yaml();

        for (Resource resource : resources) {
            try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(),
                    StandardCharsets.UTF_8)) {
                Map<String, Object> map = yaml.load(reader);
                @SuppressWarnings("unchecked")
                List<String> tools = (List<String>) map.get("tools");
                if (tools != null) {
                    declaredToolNames.addAll(tools);
                }
            }
        }

        assertThat(declaredToolNames).isNotEmpty();
        assertThat(registeredToolNames).containsAll(declaredToolNames);
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
