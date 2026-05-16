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
package io.github.pnoker.common.agentic.service.runtime;

import com.openai.models.chat.completions.ChatCompletionMessageParam;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiCompatibleAgenticRuntimeTest {

    @Test
    void assistantToolCallMessagePreservesReasoningContentForProviderContinuation() {
        OpenAiCompatibleAgenticRuntime runtime = new OpenAiCompatibleAgenticRuntime(null, null, null);

        ChatCompletionMessageParam message = runtime.assistantToolCallMessage(null, "real reasoning",
                List.of(new OpenAiCompatibleAgenticRuntime.ToolCall("call_1", "searchDrivers",
                        "{\"driverName\":\"Virtual - Edge Acceptance Lab\"}")));

        assertThat(message.isAssistant()).isTrue();
        assertThat(message.asAssistant()._additionalProperties().get("reasoning_content").convert(String.class))
                .isEqualTo("real reasoning");
        assertThat(message.asAssistant().toolCalls()).hasValueSatisfying(toolCalls -> {
            assertThat(toolCalls).hasSize(1);
            assertThat(toolCalls.get(0).asFunction().id()).isEqualTo("call_1");
            assertThat(toolCalls.get(0).asFunction().function().name()).isEqualTo("searchDrivers");
            assertThat(toolCalls.get(0).asFunction().function().arguments())
                    .contains("Virtual - Edge Acceptance Lab");
        });
    }

}
