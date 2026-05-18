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
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.facade.api.StatusHealthFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeSystemHealthBO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemAndContextToolTest {

    @Mock
    private StatusHealthFacade statusHealthFacade;

    @Test
    void tenantToolReturnsOnlyBackendTenantContext() {
        AgenticToolResult<TenantTool.CurrentTenantContext> result = new TenantTool().getCurrentTenantInfo(toolContext());

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo(AgenticConstant.ToolResult.CODE_OK);
        assertThat(result.data().tenantId()).isEqualTo(11L);
    }

    @Test
    void userToolReturnsCurrentBackendUserProfile() {
        AgenticToolResult<UserTool.CurrentUserProfile> result = new UserTool().getCurrentUserProfile(toolContext());

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo(AgenticConstant.ToolResult.CODE_OK);
        assertThat(result.data().userId()).isEqualTo(22L);
        assertThat(result.data().username()).isEqualTo("ops.engineer");
        assertThat(result.data().nickname()).isEqualTo("Ops Engineer");
    }

    @Test
    void contextToolsRejectMissingBackendContext() {
        ToolContext emptyContext = new ToolContext(Map.of());

        assertThatThrownBy(() -> new TenantTool().getCurrentTenantInfo(emptyContext))
                .isInstanceOf(UnAuthorizedException.class);
        assertThatThrownBy(() -> new UserTool().getCurrentUserProfile(emptyContext))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void systemHealthReturnsStructuredSnapshot() {
        FacadeSystemHealthBO health = new FacadeSystemHealthBO(
                Map.of("dc3-center-manager", "UP"),
                Map.of("postgres", "UP"),
                new FacadeSystemHealthBO.FleetSummary(4, 3),
                new FacadeSystemHealthBO.FleetSummary(120, 117));
        when(statusHealthFacade.systemHealth(11L)).thenReturn(health);

        AgenticToolResult<FacadeSystemHealthBO> result = new SystemTool(Optional.of(statusHealthFacade))
                .getSystemHealth(toolContext());

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo(AgenticConstant.ToolResult.CODE_OK);
        assertThat(result.data().getDrivers().getTotal()).isEqualTo(4);
        assertThat(result.data().getDevices().getOnline()).isEqualTo(117);
    }

    @Test
    void systemHealthDoesNotFabricateWhenFacadeIsUnavailable() {
        AgenticToolResult<FacadeSystemHealthBO> result = new SystemTool(Optional.empty()).getSystemHealth(toolContext());

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(AgenticConstant.ToolResult.CODE_UNAVAILABLE);
        assertThat(result.data()).isNull();
    }

    private ToolContext toolContext() {
        RequestHeader.UserHeader header = new RequestHeader.UserHeader();
        header.setTenantId(11L);
        header.setUserId(22L);
        header.setUserName("ops.engineer");
        header.setNickName("Ops Engineer");

        Map<String, Object> values = new HashMap<>();
        values.put(AgenticConstant.ToolContextKey.TENANT_ID, 11L);
        values.put(AgenticConstant.ToolContextKey.USER_ID, 22L);
        values.put(AgenticConstant.ToolContextKey.USER_HEADER, header);
        values.put(AgenticConstant.ToolContextKey.CONVERSATION_ID, "11:22:conv-1");
        return new ToolContext(values);
    }

}
