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
import io.github.pnoker.common.enums.PointCommandSourceEnum;
import io.github.pnoker.common.facade.api.PointCommandFacade;
import io.github.pnoker.common.facade.api.PointValueFacade;
import io.github.pnoker.common.facade.entity.bo.FacadePointValueBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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
    private PointCommandFacade pointCommandFacade;

    @Mock
    private ActionService actionService;

    private PointValueTool tool;
    private RequestHeader.PrincipalHeader header;

    @BeforeEach
    void setUp() {
        tool = new PointValueTool(pointValueFacade, pointCommandFacade, actionService);
        header = new RequestHeader.PrincipalHeader();
        header.setTenantId(1L);
        header.setPrincipalId(2L);
    }

    @Test
    void writePointValueAlwaysCreatesPendingAction() {
        when(actionService.createWritePointValueAction("conv-1", 10L, 20L, "42", header))
                .thenReturn(reactor.core.publisher.Mono.just("action-1"));

        StepVerifier.create(tool.writePointValueReactive(10L, 20L, "42", toolContext(Map.of(
                        AgenticConstant.ToolContextKey.CONVERSATION_ID, "conv-1",
                        AgenticConstant.ToolContextKey.USER_HEADER, header))))
                .assertNext(result -> {
                    assertThat(result.success()).isTrue();
                    assertThat(result.data()).isNotNull();
                    assertThat(result.data().sent()).isFalse();
                    assertThat(result.data().pendingConfirmation()).isTrue();
                    assertThat(result.data().actionId()).isEqualTo("action-1");
                })
                .verifyComplete();
        verify(actionService).createWritePointValueAction("conv-1", 10L, 20L, "42", header);
        verify(pointCommandFacade, never()).submitWrite(anyLong(), anyLong(), anyLong(), anyString());
    }

    @Test
    void getPointValueHistoryReturnsRawValuesAndChartData() {
        when(pointValueFacade.history(1L, 10L, 20L, null, 5)).thenReturn(reactor.core.publisher.Mono.just(io.github.pnoker.db.r2dbc.core.page.CursorPage.of(List.of(
                FacadePointValueBO.builder().value("24.0").build(),
                FacadePointValueBO.builder().value("23.8").build(),
                FacadePointValueBO.builder().value("offline").build(),
                FacadePointValueBO.builder().value("23.5").build()), null)));

        AtomicReference<PointValueTool.PointValueHistory> history = new AtomicReference<>();
        StepVerifier.create(tool.getPointValueHistoryReactive(10L, 20L, 5,
                        toolContext(Map.of(AgenticConstant.ToolContextKey.USER_HEADER, header))))
                .assertNext(value -> {
                    assertThat(value.success()).isTrue();
                    history.set(value.data());
                }).verifyComplete();
        PointValueTool.PointValueHistory result = history.get();

        assertThat(result.values()).containsExactly("24.0", "23.8", "offline", "23.5");
        assertThat(result.chart()).isNotNull();
        assertThat(result.chart().series()).hasSize(1);
        assertThat(result.chart().series().get(0).data())
                .containsExactly(List.of(0, 23.5D), List.of(1, 23.8D), List.of(2, 24.0D));
        assertThat(result.summary().numericCount()).isEqualTo(3);
        assertThat(result.summary().nonNumericCount()).isEqualTo(1);
        assertThat(result.summary().latest()).isEqualTo(24.0D);
        assertThat(result.summary().average()).isEqualTo((23.5D + 23.8D + 24.0D) / 3);
    }

    @Test
    void reactiveLatestValueUsesReactiveFacade() {
        FacadePointValueBO latest = FacadePointValueBO.builder().value("42").build();
        when(pointValueFacade.lastValue(1L, 10L, 20L)).thenReturn(reactor.core.publisher.Mono.just(latest));

        StepVerifier.create(tool.getLatestPointValueReactive(10L, 20L,
                        toolContext(Map.of(AgenticConstant.ToolContextKey.TENANT_ID, 1L))))
                .assertNext(result -> {
                    assertThat(result.success()).isTrue();
                    assertThat(result.data()).isSameAs(latest);
                })
                .verifyComplete();
        verify(pointValueFacade).lastValue(1L, 10L, 20L);
    }

    @Test
    void readPointValueSendsReadCommandThroughPointCommandFacade() {
        when(pointCommandFacade.submitRead(1L, 10L, 20L, PointCommandSourceEnum.AGENTIC))
                .thenReturn(reactor.core.publisher.Mono.just("cmd-1"));

        AgenticToolResult<PointValueTool.PointCommandResult> result = tool.readPointValueReactive(10L, 20L,
                toolContext(Map.of(AgenticConstant.ToolContextKey.USER_HEADER, header))).block();

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo(AgenticConstant.ToolResult.CODE_OK);
        assertThat(result.data().sent()).isTrue();
        assertThat(result.data().pendingConfirmation()).isFalse();
        verify(pointCommandFacade).submitRead(1L, 10L, 20L, PointCommandSourceEnum.AGENTIC);
    }

    private ToolContext toolContext(Map<String, Object> values) {
        return new ToolContext(new HashMap<>(values));
    }

}
