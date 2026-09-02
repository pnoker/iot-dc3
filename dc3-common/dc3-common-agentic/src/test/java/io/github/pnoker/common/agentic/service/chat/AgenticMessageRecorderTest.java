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
package io.github.pnoker.common.agentic.service.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.pnoker.common.agentic.entity.bo.MessageBO;
import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import io.github.pnoker.common.agentic.entity.model.AgenticRunEvent;
import io.github.pnoker.common.agentic.entity.model.AgenticVisualizationSpec;
import io.github.pnoker.common.agentic.repository.ReactiveMessageStore;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.AgenticMessageStatusEnum;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AgenticMessageRecorderTest {

    @Mock
    private ReactiveMessageStore messageStore;

    private AgenticMessageRecorder recorder;

    private RequestHeader.PrincipalHeader userHeader;

    @BeforeEach
    void setUp() {
        recorder = new AgenticMessageRecorder(messageStore);
        org.mockito.Mockito.lenient()
                .when(messageStore.save(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(Mono.just(new MessageBO()));
        userHeader = new RequestHeader.PrincipalHeader();
        userHeader.setTenantId(1L);
        userHeader.setPrincipalId(2L);
        userHeader.setPrincipalName("admin");
    }

    @Test
    void persistAssistantMessageSavesTraceOnlyMessage() {
        AgenticRunTrace runTrace = new AgenticRunTrace();
        runTrace.recordPendingEvent(
                new AgenticRunEvent("tool", "lookupDevice", "Device loaded", "OK", 1000L, "result", "success", "OK"));
        AgenticPreparedChatBO prepared = prepared(runTrace, false, List.of());

        StepVerifier.create(recorder.persistAssistantMessage(prepared, "", userHeader))
                .verifyComplete();

        ArgumentCaptor<AgenticMessageContent> captor = ArgumentCaptor.forClass(AgenticMessageContent.class);
        verify(messageStore)
                .save(
                        eq("conversation"),
                        eq("assistant"),
                        captor.capture(),
                        eq("dc3-test-model"),
                        eq(AgenticMessageStatusEnum.COMPLETED),
                        eq(userHeader));
        AgenticMessageContent content = captor.getValue();
        assertThat(content.getText()).isEmpty();
        assertThat(content.getTools()).containsExactly("lookupDevice");
        assertThat(content.getTraces()).hasSize(1);
        assertThat(content.getTraces().get(0).getStatus()).isEqualTo("success");
    }

    @Test
    void persistAssistantMessageSavesReasoningOnlyMessage() {
        AgenticPreparedChatBO prepared = prepared(new AgenticRunTrace(), true, List.of());

        StepVerifier.create(recorder.persistAssistantMessage(prepared, "", "查询驱动列表前，先确认租户上下文。", userHeader))
                .verifyComplete();

        ArgumentCaptor<AgenticMessageContent> captor = ArgumentCaptor.forClass(AgenticMessageContent.class);
        verify(messageStore)
                .save(
                        eq("conversation"),
                        eq("assistant"),
                        captor.capture(),
                        eq("dc3-test-model"),
                        eq(AgenticMessageStatusEnum.COMPLETED),
                        eq(userHeader));
        AgenticMessageContent content = captor.getValue();
        assertThat(content.getText()).isEmpty();
        assertThat(content.getReasoning()).isTrue();
        assertThat(content.getReasoningContent()).isEqualTo("查询驱动列表前，先确认租户上下文。");
        assertThat(content.getTraces())
                .extracting(AgenticMessageContent.Trace::getType)
                .containsExactly("reasoning");
    }

    @Test
    void persistAssistantMessageSavesVisualizationOnlyMessage() {
        AgenticRunTrace runTrace = new AgenticRunTrace();
        runTrace.recordPendingVisualization(visualization());
        AgenticPreparedChatBO prepared = prepared(runTrace, false, List.of());

        StepVerifier.create(recorder.persistAssistantMessage(prepared, "", userHeader))
                .verifyComplete();

        ArgumentCaptor<AgenticMessageContent> captor = ArgumentCaptor.forClass(AgenticMessageContent.class);
        verify(messageStore)
                .save(
                        eq("conversation"),
                        eq("assistant"),
                        captor.capture(),
                        eq("dc3-test-model"),
                        eq(AgenticMessageStatusEnum.COMPLETED),
                        eq(userHeader));
        AgenticMessageContent content = captor.getValue();
        assertThat(content.getText()).isEmpty();
        assertThat(content.getCharts()).hasSize(1);
        assertThat(content.getCharts().get(0).getType()).isEqualTo("line");
    }

    @Test
    void persistAssistantMessageSkipsCompletelyEmptyMessage() {
        AgenticPreparedChatBO prepared = prepared(new AgenticRunTrace(), false, List.of());

        StepVerifier.create(recorder.persistAssistantMessage(prepared, " ", userHeader))
                .verifyComplete();

        verifyNoInteractions(messageStore);
    }

    private AgenticPreparedChatBO prepared(
            AgenticRunTrace runTrace, boolean reasoning, List<AgenticMessageContent.Context> contexts) {
        return new AgenticPreparedChatBO(
                "hello",
                "conversation",
                null,
                "dc3-test-model",
                Map.of(),
                null,
                null,
                runTrace,
                true,
                reasoning,
                List.of(),
                contexts,
                AgenticMessageContent.Tokens.of(1, 0, 1, 0, 0, 0),
                List.of());
    }

    private AgenticVisualizationSpec visualization() {
        AgenticVisualizationSpec visualization = new AgenticVisualizationSpec();
        visualization.setId("chart-1");
        visualization.setType("line");
        visualization.setTitle("Trend");
        visualization.setDataset(List.of(Map.of("index", 0, "value", 23.5)));
        visualization.setEncode(AgenticVisualizationSpec.Encode.xy("index", "value"));
        return visualization;
    }
}
