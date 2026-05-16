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

import io.github.pnoker.common.agentic.context.AgenticRequestContext;
import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticChatResponseCodecTest {

    private final AgenticChatResponseCodec codec = new AgenticChatResponseCodec(new ObjectMapper());

    @Test
    void streamEventsFlushesToolEventsWithoutContentChunk() {
        Queue<AgenticRequestContext.ToolEvent> toolEvents = new ConcurrentLinkedQueue<>();
        toolEvents.offer(new AgenticRequestContext.ToolEvent("searchDevices", "device", "Search devices", 1000L));
        AgenticPreparedChatRequest prepared = prepared(toolEvents);

        List<ServerSentEvent<String>> events = codec.streamEvents(prepared, "chatcmpl-test", 1L,
                AgenticStreamDelta.empty());

        assertThat(events).hasSize(1);
        assertThat(events.get(0).data()).contains("\"object\":\"agentic.event\"");
        assertThat(events.get(0).data()).contains("\"name\":\"searchDevices\"");
        assertThat(events.get(0).data()).doesNotContain("\"object\":\"chat.completion.chunk\"");
        assertThat(prepared.toolTraceEvents()).hasSize(1);
    }

    @Test
    void streamEventsKeepsToolEventsBeforeContentChunk() {
        Queue<AgenticRequestContext.ToolEvent> toolEvents = new ConcurrentLinkedQueue<>();
        toolEvents.offer(new AgenticRequestContext.ToolEvent("lookupDeviceById", "device", "Query device by ID",
                1000L));
        AgenticPreparedChatRequest prepared = prepared(toolEvents);

        List<ServerSentEvent<String>> events = codec.streamEvents(prepared, "chatcmpl-test", 1L,
                new AgenticStreamDelta("Device loaded", null));

        assertThat(events).hasSize(2);
        assertThat(events.get(0).data()).contains("\"object\":\"agentic.event\"");
        assertThat(events.get(1).data()).contains("\"object\":\"chat.completion.chunk\"");
        assertThat(events.get(1).data()).contains("\"content\":\"Device loaded\"");
    }

    private AgenticPreparedChatRequest prepared(Queue<AgenticRequestContext.ToolEvent> toolEvents) {
        return new AgenticPreparedChatRequest("hello", "tenant:user:conversation", null, "dc3-test-model",
                Map.of(), null, null, toolEvents, true, false, List.of(), List.of(),
                AgenticMessageContent.Tokens.of(1, 0, 1, 0, 0, 0), new ArrayList<>());
    }

}
