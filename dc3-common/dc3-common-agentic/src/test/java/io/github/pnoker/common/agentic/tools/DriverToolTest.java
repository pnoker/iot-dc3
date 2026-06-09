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
import io.github.pnoker.common.enums.DriverTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.api.StatusHealthFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDriverQuery;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverToolTest {

    @Mock
    private DriverFacade driverFacade;

    @Mock
    private StatusHealthFacade statusHealthFacade;

    private RequestHeader.UserHeader header;

    @BeforeEach
    void setUp() {
        header = new RequestHeader.UserHeader();
        header.setTenantId(11L);
        header.setUserId(22L);
    }

    @Test
    void searchDriversUsesTenantScopedQueryAndReturnsStructuredPage() {
        DriverTool tool = new DriverTool(driverFacade, Optional.of(statusHealthFacade));
        FacadeDriverBO driver = driver(101L, "Virtual - Edge Acceptance Lab");
        FacadePage<FacadeDriverBO> page = new FacadePage<>(1L, 10L, 1L, 1L, List.of(driver));
        when(driverFacade.listByPage(org.mockito.ArgumentMatchers.any(FacadeDriverQuery.class))).thenReturn(page);

        AgenticToolResult<FacadePage<FacadeDriverBO>> result = tool.searchDrivers(
                "Virtual - Edge Acceptance Lab", 1, 10, toolContext());

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo(AgenticConstant.ToolResult.CODE_OK);
        assertThat(result.message()).isEqualTo("Driver page loaded");
        assertThat(result.data().getRecords()).extracting(FacadeDriverBO::getDriverName)
                .containsExactly("Virtual - Edge Acceptance Lab");

        ArgumentCaptor<FacadeDriverQuery> captor = forClass(FacadeDriverQuery.class);
        verify(driverFacade).listByPage(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(11L);
        assertThat(captor.getValue().getDriverName()).isEqualTo("Virtual - Edge Acceptance Lab");
        assertThat(captor.getValue().getPage().getCurrent()).isEqualTo(1L);
        assertThat(captor.getValue().getPage().getSize()).isEqualTo(10L);
    }

    @Test
    void getDriverStatusesReturnsUnavailableWhenStatusFacadeIsAbsent() {
        DriverTool tool = new DriverTool(driverFacade, Optional.empty());

        AgenticToolResult<Map<Long, String>> result = tool.getDriverStatusesByIds(List.of(101L), toolContext());

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(AgenticConstant.ToolResult.CODE_UNAVAILABLE);
        assertThat(result.data()).isNull();
    }

    @Test
    void lookupDriverByIdReturnsNotFoundWithoutFabricatingData() {
        DriverTool tool = new DriverTool(driverFacade, Optional.of(statusHealthFacade));
        when(driverFacade.getById(11L, 404L)).thenReturn(null);

        AgenticToolResult<FacadeDriverBO> result = tool.lookupDriverById(404L, toolContext());

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(AgenticConstant.ToolResult.CODE_NOT_FOUND);
        assertThat(result.data()).isNull();
    }

    private FacadeDriverBO driver(Long id, String name) {
        FacadeDriverBO driver = new FacadeDriverBO();
        driver.setId(id);
        driver.setDriverName(name);
        driver.setDriverCode("virtual-edge-acceptance-lab");
        driver.setServiceName("dc3-driver-virtual");
        driver.setServiceHost("dc3-driver-virtual.iot.svc");
        driver.setDriverTypeFlag(DriverTypeEnum.DRIVER_CLIENT);
        driver.setEnableFlag(EnableFlagEnum.ENABLE);
        driver.setTenantId(11L);
        return driver;
    }

    private ToolContext toolContext() {
        Map<String, Object> values = new HashMap<>();
        values.put(AgenticConstant.ToolContextKey.TENANT_ID, header.getTenantId());
        values.put(AgenticConstant.ToolContextKey.USER_ID, header.getUserId());
        values.put(AgenticConstant.ToolContextKey.USER_HEADER, header);
        values.put(AgenticConstant.ToolContextKey.CONVERSATION_ID, "11:22:conv-1");
        return new ToolContext(values);
    }

}
