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

import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import io.github.pnoker.common.agentic.entity.model.AgenticRunEvent;
import io.github.pnoker.common.agentic.entity.model.AgenticVisualizationSpec;
import io.github.pnoker.common.agentic.service.MessageService;
import io.github.pnoker.common.entity.common.RequestHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AgenticMessageRecorderTest {

    @Mock
    private MessageService messageService;

    private AgenticMessageRecorder recorder;

    private RequestHeader.UserHeader userHeader;

    @BeforeEach
    void setUp() {
        recorder = new AgenticMessageRecorder(messageService);
        userHeader = new RequestHeader.UserHeader();
        userHeader.setTenantId(1L);
        userHeader.setUserId(2L);
        userHeader.setUserName("admin");
    }

    @Test
    void persistAssistantMessageSavesTraceOnlyMessage() {
        AgenticRunTrace runTrace = new AgenticRunTrace();
        runTrace.recordPendingEvent(new AgenticRunEvent("tool", "lookupDevice", "Device loaded", "OK",
                1000L, "result", "success", "OK"));
        AgenticPreparedChatRequest prepared = prepared(runTrace, false, List.of());

        recorder.persistAssistantMessage(prepared, "", userHeader);

        ArgumentCaptor<AgenticMessageContent> captor = ArgumentCaptor.forClass(AgenticMessageContent.class);
        verify(messageService).save(eq("tenant:user:conversation"), eq("assistant"), captor.capture(),
                eq("dc3-test-model"), eq(userHeader));
        AgenticMessageContent content = captor.getValue();
        assertThat(content.getText()).isEmpty();
        assertThat(content.getTools()).containsExactly("lookupDevice");
        assertThat(content.getTraces()).hasSize(1);
        assertThat(content.getTraces().get(0).getStatus()).isEqualTo("success");
    }

    @Test
    void persistAssistantMessageSavesReasoningOnlyMessage() {
        AgenticPreparedChatRequest prepared = prepared(new AgenticRunTrace(), true, List.of());

        recorder.persistAssistantMessage(prepared, "", "查询驱动列表前，先确认租户上下文。", userHeader);

        ArgumentCaptor<AgenticMessageContent> captor = ArgumentCaptor.forClass(AgenticMessageContent.class);
        verify(messageService).save(eq("tenant:user:conversation"), eq("assistant"), captor.capture(),
                eq("dc3-test-model"), eq(userHeader));
        AgenticMessageContent content = captor.getValue();
        assertThat(content.getText()).isEmpty();
        assertThat(content.getReasoning()).isTrue();
        assertThat(content.getReasoningContent()).isEqualTo("查询驱动列表前，先确认租户上下文。");
        assertThat(content.getTraces()).extracting(AgenticMessageContent.Trace::getType).containsExactly("reasoning");
    }

    @Test
    void persistAssistantMessageSavesVisualizationOnlyMessage() {
        AgenticRunTrace runTrace = new AgenticRunTrace();
        runTrace.recordPendingVisualization(visualization());
        AgenticPreparedChatRequest prepared = prepared(runTrace, false, List.of());

        recorder.persistAssistantMessage(prepared, "", userHeader);

        ArgumentCaptor<AgenticMessageContent> captor = ArgumentCaptor.forClass(AgenticMessageContent.class);
        verify(messageService).save(eq("tenant:user:conversation"), eq("assistant"), captor.capture(),
                eq("dc3-test-model"), eq(userHeader));
        AgenticMessageContent content = captor.getValue();
        assertThat(content.getText()).isEmpty();
        assertThat(content.getCharts()).hasSize(1);
        assertThat(content.getCharts().get(0).getType()).isEqualTo("line");
    }


    @Test
    void persistAssistantMessageSkipsCompletelyEmptyMessage() {
        AgenticPreparedChatRequest prepared = prepared(new AgenticRunTrace(), false, List.of());

        recorder.persistAssistantMessage(prepared, " ", userHeader);

        verifyNoInteractions(messageService);
    }

    private AgenticPreparedChatRequest prepared(AgenticRunTrace runTrace, boolean reasoning,
                                                List<AgenticMessageContent.Context> contexts) {
        return new AgenticPreparedChatRequest("hello", "tenant:user:conversation", null, "dc3-test-model",
                Map.of(), null, null, runTrace, true, reasoning, List.of(), contexts,
                AgenticMessageContent.Tokens.of(1, 0, 1, 0, 0, 0), List.of());
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
