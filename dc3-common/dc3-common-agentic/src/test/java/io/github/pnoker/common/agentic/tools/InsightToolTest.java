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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.facade.api.InsightFacade;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class InsightToolTest {

    @Mock
    private InsightFacade insightFacade;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void alertStatsDelegatesWithoutArguments() {
        when(insightFacade.invokeReactive(eq(11L), eq(InsightFacade.Operations.ALERT_STATS), eq("")))
                .thenReturn(Mono.just("{\"total\":5,\"unconfirmed\":2}"));

        StepVerifier.create(tool().getAlertStats(toolContext()))
                .assertNext(result -> {
                    assertThat(result.success()).isTrue();
                    assertThat(result.data())
                            .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                            .containsEntry("total", 5)
                            .containsEntry("unconfirmed", 2);
                })
                .verifyComplete();
    }

    @Test
    void latestAlertsSerializeLimitIntoRequestJson() {
        when(insightFacade.invokeReactive(eq(11L), eq(InsightFacade.Operations.ALERT_LATEST), eq("{\"limit\":25}")))
                .thenReturn(Mono.just("[]"));

        StepVerifier.create(tool().getLatestAlerts(25, toolContext()))
                .assertNext(result -> assertThat(result.success()).isTrue())
                .verifyComplete();
        verify(insightFacade).invokeReactive(eq(11L), eq(InsightFacade.Operations.ALERT_LATEST), eq("{\"limit\":25}"));
    }

    @Test
    void pageAlertsRejectInvalidPagination() {
        StepVerifier.create(tool().pageAlerts(null, null, null, null, null, -1, 10, toolContext()))
                .assertNext(
                        result -> assertThat(result.code()).isEqualTo(AgenticConstant.ToolResult.CODE_INVALID_ARGUMENT))
                .verifyComplete();
        StepVerifier.create(tool().pageAlerts(null, null, null, null, null, 0, 500, toolContext()))
                .assertNext(
                        result -> assertThat(result.code()).isEqualTo(AgenticConstant.ToolResult.CODE_INVALID_ARGUMENT))
                .verifyComplete();
        verifyNoInteractions(insightFacade);
    }

    @Test
    void analyticsPassesRawPayloadThrough() {
        Map<String, Object> payload = Map.of("series", java.util.List.of(Map.of("deviceId", 1, "pointId", 2)));
        when(insightFacade.invokeReactive(eq(11L), eq(InsightFacade.Operations.QUERY_LATEST), anyJson()))
                .thenReturn(Mono.just("{\"conclusion\":\"ok\"}"));

        StepVerifier.create(tool().analyticsQueryLatest(payload, toolContext()))
                .assertNext(result -> {
                    assertThat(result.success()).isTrue();
                    assertThat(result.data())
                            .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                            .containsEntry("conclusion", "ok");
                })
                .verifyComplete();
    }

    @Test
    void pointCommandStatusRequiresCommandId() {
        StepVerifier.create(tool().getPointCommandStatus(" ", toolContext()))
                .assertNext(
                        result -> assertThat(result.code()).isEqualTo(AgenticConstant.ToolResult.CODE_INVALID_ARGUMENT))
                .verifyComplete();
        verifyNoInteractions(insightFacade);
    }

    @Test
    void pointCommandStatusClosesTheReadLoop() {
        when(insightFacade.invokeReactive(
                        eq(11L), eq(InsightFacade.Operations.POINT_COMMAND_STATUS), eq("{\"commandId\":\"cmd-9\"}")))
                .thenReturn(Mono.just("{\"status\":\"SUCCESS\"}"));

        StepVerifier.create(tool().getPointCommandStatus("cmd-9", toolContext()))
                .assertNext(result -> {
                    assertThat(result.success()).isTrue();
                    assertThat(result.data())
                            .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                            .containsEntry("status", "SUCCESS");
                })
                .verifyComplete();
    }

    @Test
    void facadeErrorsBecomeStructuredErrors() {
        when(insightFacade.invokeReactive(eq(11L), eq(InsightFacade.Operations.ALERT_STATS), eq("")))
                .thenReturn(Mono.error(new IllegalArgumentException("bad")));

        StepVerifier.create(tool().getAlertStats(toolContext()))
                .assertNext(result -> {
                    assertThat(result.success()).isFalse();
                    assertThat(result.code()).isEqualTo(AgenticConstant.ToolResult.CODE_ERROR);
                })
                .verifyComplete();
    }

    @Test
    void insightToolsDoNotFabricateWhenFacadeIsUnavailable() {
        InsightTool tool = new InsightTool(Optional.empty(), objectMapper);
        StepVerifier.create(tool.getAlertStats(toolContext()))
                .assertNext(result -> assertThat(result.code()).isEqualTo(AgenticConstant.ToolResult.CODE_UNAVAILABLE))
                .verifyComplete();
    }

    private InsightTool tool() {
        return new InsightTool(Optional.of(insightFacade), objectMapper);
    }

    private static String anyJson() {
        return org.mockito.ArgumentMatchers.any(String.class);
    }

    private ToolContext toolContext() {
        RequestHeader.PrincipalHeader header = new RequestHeader.PrincipalHeader();
        header.setTenantId(11L);
        header.setPrincipalId(22L);
        header.setPrincipalName("ops.engineer");
        header.setDisplayName("Ops Engineer");

        Map<String, Object> values = new HashMap<>();
        values.put(AgenticConstant.ToolContextKey.TENANT_ID, 11L);
        values.put(AgenticConstant.ToolContextKey.USER_ID, 22L);
        values.put(AgenticConstant.ToolContextKey.USER_HEADER, header);
        values.put(AgenticConstant.ToolContextKey.CONVERSATION_ID, "conv-1");
        return new ToolContext(values);
    }
}
