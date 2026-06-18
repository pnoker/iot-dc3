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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.github.pnoker.common.agentic.config.ChatClientFactory;
import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import io.github.pnoker.common.agentic.service.chat.AgenticPreparedChatBO;
import io.github.pnoker.common.agentic.service.chat.AgenticPromptBuilder;
import io.github.pnoker.common.agentic.service.chat.AgenticRunTrace;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.enums.AgenticModelProviderTypeEnum;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAiCompatibleAgenticRuntimeConversationTest {

    private static final String MODEL = "deepseek-reasoner";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ChatClientFactory chatClientFactory;

    @Mock
    private AgenticPromptBuilder promptBuilder;

    private static Stream<ConversationScenario> platformDataScenarios() {
        return Stream.of(
                new ConversationScenario(
                        "用户询问设备列表时必须调用设备查询工具",
                        "目前有哪些设备",
                        "searchDevices",
                        Map.of("deviceName", "", "deviceCode", "", "driverId", "", "page", 1, "size", 20),
                        "deviceName",
                        "{\"success\":true,\"code\":\"OK\",\"message\":\"Device page loaded\",\"data\":{\"records\":[{\"id\":201,\"deviceName\":\"Edge Gateway A1\"}]}}",
                        "OK",
                        "当前租户下有 1 台设备：Edge Gateway A1。"),
                new ConversationScenario(
                        "用户询问驱动列表时必须调用驱动查询工具",
                        "我需要的是驱动列表",
                        "searchDrivers",
                        Map.of("driverName", "", "page", 1, "size", 20),
                        "driverName",
                        "{\"success\":true,\"code\":\"OK\",\"message\":\"Driver page loaded\",\"data\":{\"records\":[{\"id\":101,\"driverName\":\"Virtual - Edge Acceptance Lab\"}]}}",
                        "OK",
                        "当前租户下有 1 个驱动：Virtual - Edge Acceptance Lab。"),
                new ConversationScenario(
                        "用户按驱动名称查详情时必须带名称调用驱动查询工具",
                        "查一下这个驱动Virtual - Edge Acceptance Lab的详情",
                        "searchDrivers",
                        Map.of("driverName", "Virtual - Edge Acceptance Lab", "page", 1, "size", 10),
                        "Virtual - Edge Acceptance Lab",
                        "{\"success\":true,\"code\":\"OK\",\"message\":\"Driver page loaded\",\"data\":{\"records\":[{\"id\":101,\"driverName\":\"Virtual - Edge Acceptance Lab\",\"serviceName\":\"dc3-driver-virtual\"}]}}",
                        "OK",
                        "Virtual - Edge Acceptance Lab 使用服务 dc3-driver-virtual。"),
                new ConversationScenario(
                        "用户询问点位最新值时必须调用点值工具",
                        "读取设备201的温度点位301当前值",
                        "getLatestPointValue",
                        Map.of("deviceId", 201, "pointId", 301),
                        "pointId",
                        "{\"success\":true,\"code\":\"OK\",\"message\":\"Latest point value loaded\",\"data\":{\"deviceId\":201,\"pointId\":301,\"value\":\"23.7\",\"unit\":\"C\"}}",
                        "OK",
                        "设备 201 的点位 301 当前值是 23.7。"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("platformDataScenarios")
    void platformDataQuestionsRunRealToolLoop(ConversationScenario scenario) throws Exception {
        try (FakeOpenAiServer server = FakeOpenAiServer.start()) {
            server.enqueue(toolCallResponse(scenario.toolName(), scenario.arguments(),
                    "需要调用平台工具读取真实数据。"));
            server.enqueue(finalResponse(scenario.finalAnswer()));
            RecordingToolCallback callback = new RecordingToolCallback(scenario.toolName(), scenario.toolResult());
            OpenAiCompatibleAgenticRuntime runtime = runtime(server, callback);

            AgenticRuntimeResult result = runtime.call(prepared(scenario.userMessage()));

            assertThat(result.content()).isEqualTo(scenario.finalAnswer());
            assertThat(result.finishReason()).isEqualTo(AgenticConstant.Chat.FINISH_REASON_STOP);
            assertThat(callback.calls()).hasValue(1);
            assertThat(callback.lastInput()).contains(scenario.expectedArgumentFragment());
            assertThat(callback.lastToolContext().getContext())
                    .containsEntry(AgenticConstant.ToolContextKey.TENANT_ID, 11L)
                    .containsEntry(AgenticConstant.ToolContextKey.USER_ID, 22L);

            assertThat(server.requestBodies()).hasSize(2);
            JsonNode secondRequest = objectMapper.readTree(server.requestBodies().get(1));
            assertThat(secondRequest.at("/messages").toString())
                    .contains("\"role\":\"assistant\"")
                    .contains("\"reasoning_content\":\"需要调用平台工具读取真实数据。\"")
                    .contains("\"role\":\"tool\"")
                    .contains(scenario.toolResultCode());
        }
    }

    @Test
    void generalChatCanFinishWithoutToolCall() throws Exception {
        try (FakeOpenAiServer server = FakeOpenAiServer.start()) {
            server.enqueue(finalResponse("你好，我是 IoT DC3 平台助手。"));
            RecordingToolCallback callback = new RecordingToolCallback("searchDevices",
                    "{\"success\":true,\"code\":\"OK\",\"message\":\"Device page loaded\",\"data\":{}}");
            OpenAiCompatibleAgenticRuntime runtime = runtime(server, callback);

            AgenticRuntimeResult result = runtime.call(prepared("你好"));

            assertThat(result.content()).isEqualTo("你好，我是 IoT DC3 平台助手。");
            assertThat(callback.calls()).hasValue(0);
            assertThat(server.requestBodies()).hasSize(1);
            assertThat(server.requestBodies().get(0)).contains("你好");
        }
    }

    private OpenAiCompatibleAgenticRuntime runtime(FakeOpenAiServer server, ToolCallback callback) {
        ModelProviderBO provider = new ModelProviderBO();
        provider.setProviderType(AgenticModelProviderTypeEnum.OPENAI_COMPATIBLE);
        provider.setBaseUrl(server.baseUrl());
        provider.setApiKey("test-api-key");
        when(chatClientFactory.resolveProviderForModel(MODEL)).thenReturn(provider);
        when(promptBuilder.buildSystemPrompt(any(AgenticPreparedChatBO.class)))
                .thenReturn("You are the IoT DC3 platform assistant.");
        return new OpenAiCompatibleAgenticRuntime(chatClientFactory, promptBuilder,
                ToolCallbackProvider.from(callback));
    }

    private AgenticPreparedChatBO prepared(String userMessage) {
        return new AgenticPreparedChatBO(userMessage, "11:22:conv-1", null, MODEL,
                Map.of(
                        AgenticConstant.ToolContextKey.TENANT_ID, 11L,
                        AgenticConstant.ToolContextKey.USER_ID, 22L,
                        AgenticConstant.ToolContextKey.CONVERSATION_ID, "11:22:conv-1"),
                null, null, new AgenticRunTrace(), true, true, List.of(), List.of(),
                AgenticMessageContent.Tokens.of(10, 0, 10, 0, 0, 0), List.of());
    }

    private String toolCallResponse(String toolName, Map<String, Object> arguments, String reasoning)
            throws JsonProcessingException {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", AgenticConstant.Chat.ROLE_ASSISTANT);
        message.put("content", null);
        message.put("reasoning_content", reasoning);
        message.put("tool_calls", List.of(Map.of(
                "id", "call_1",
                "type", "function",
                "function", Map.of(
                        "name", toolName,
                        "arguments", objectMapper.writeValueAsString(arguments)))));
        return completion(message, "tool_calls");
    }

    private String finalResponse(String content) throws JsonProcessingException {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", AgenticConstant.Chat.ROLE_ASSISTANT);
        message.put("content", content);
        return completion(message, AgenticConstant.Chat.FINISH_REASON_STOP);
    }

    private String completion(Map<String, Object> message, String finishReason) throws JsonProcessingException {
        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("index", 0);
        choice.put("message", message);
        choice.put("finish_reason", finishReason);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", "chatcmpl-test");
        response.put("object", "chat.completion");
        response.put("created", 1_780_000_000L);
        response.put("model", MODEL);
        response.put("choices", List.of(choice));
        return objectMapper.writeValueAsString(response);
    }

    private record ConversationScenario(String name, String userMessage, String toolName,
                                        Map<String, Object> arguments, String expectedArgumentFragment,
                                        String toolResult, String toolResultCode, String finalAnswer) {
        @Override
        public String toString() {
            return name;
        }
    }

    private static class RecordingToolCallback implements ToolCallback {

        private final String name;

        private final String result;

        private final AtomicInteger calls = new AtomicInteger();

        private final AtomicReference<String> lastInput = new AtomicReference<>();

        private final AtomicReference<ToolContext> lastToolContext = new AtomicReference<>();

        private RecordingToolCallback(String name, String result) {
            this.name = name;
            this.result = result;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return DefaultToolDefinition.builder()
                    .name(name)
                    .description("Fixture tool for " + name)
                    .inputSchema("""
                            {"type":"object","properties":{}}
                            """)
                    .build();
        }

        @Override
        public String call(String toolInput) {
            calls.incrementAndGet();
            lastInput.set(StringUtils.defaultString(toolInput));
            return result;
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            calls.incrementAndGet();
            lastInput.set(StringUtils.defaultString(toolInput));
            lastToolContext.set(toolContext);
            return result;
        }

        AtomicInteger calls() {
            return calls;
        }

        String lastInput() {
            return lastInput.get();
        }

        ToolContext lastToolContext() {
            return lastToolContext.get();
        }

    }

    private static class FakeOpenAiServer implements AutoCloseable {

        private final HttpServer server;

        private final Queue<String> responses = new ArrayDeque<>();

        private final List<String> requestBodies = new ArrayList<>();

        private FakeOpenAiServer(HttpServer server) {
            this.server = server;
        }

        static FakeOpenAiServer start() throws IOException {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            FakeOpenAiServer fakeServer = new FakeOpenAiServer(httpServer);
            httpServer.createContext("/v1/chat/completions", exchange -> {
                fakeServer.requestBodies.add(new String(exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8));
                String response = fakeServer.responses.poll();
                if (Objects.isNull(response)) {
                    response = "{\"error\":{\"message\":\"No fake OpenAI response queued\"}}";
                    byte[] body = response.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(500, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                    return;
                }
                byte[] body = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            httpServer.start();
            return fakeServer;
        }

        void enqueue(String response) {
            responses.offer(response);
        }

        String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
        }

        List<String> requestBodies() {
            return requestBodies;
        }

        @Override
        public void close() {
            server.stop(0);
        }

    }

}
