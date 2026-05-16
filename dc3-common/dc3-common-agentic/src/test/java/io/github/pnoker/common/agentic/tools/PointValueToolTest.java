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
import io.github.pnoker.common.agentic.service.ActionService;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.facade.api.PointValueCommandFacade;
import io.github.pnoker.common.facade.api.PointValueFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointValueToolTest {

    @Mock
    private PointValueFacade pointValueFacade;

    @Mock
    private PointValueCommandFacade pointValueCommandFacade;

    @Mock
    private ActionService actionService;

    private PointValueTool tool;
    private RequestHeader.UserHeader header;

    @BeforeEach
    void setUp() {
        tool = new PointValueTool(pointValueFacade, pointValueCommandFacade, actionService);
        header = new RequestHeader.UserHeader();
        header.setTenantId(1L);
        header.setUserId(2L);
    }

    @Test
    void writePointValueAlwaysCreatesPendingAction() {
        when(actionService.createWritePointValueAction("conv-1", 10L, 20L, "42", header))
                .thenReturn("action-1");

        AgenticToolResult<PointValueTool.PointCommandResult> result = tool.writePointValue(10L, 20L, "42",
                toolContext(Map.of(
                        AgenticConstant.ToolContextKey.CONVERSATION_ID, "conv-1",
                        AgenticConstant.ToolContextKey.USER_HEADER, header)));

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isNotNull();
        assertThat(result.data().sent()).isFalse();
        assertThat(result.data().pendingConfirmation()).isTrue();
        assertThat(result.data().actionId()).isEqualTo("action-1");
        verify(actionService).createWritePointValueAction("conv-1", 10L, 20L, "42", header);
        verify(pointValueCommandFacade, never()).write(anyLong(), anyLong(), anyLong(), anyString());
    }

    private ToolContext toolContext(Map<String, Object> values) {
        return new ToolContext(new HashMap<>(values));
    }

}
