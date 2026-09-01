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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.core.http.AsyncStreamResponse;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionFunctionTool;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionStreamOptions;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import io.github.pnoker.common.agentic.config.ChatClientFactory;
import io.github.pnoker.common.agentic.entity.bo.MessageBO;
import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import io.github.pnoker.common.agentic.service.chat.AgenticPreparedChatBO;
import io.github.pnoker.common.agentic.service.chat.AgenticPromptBuilder;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.enums.AgenticModelProviderTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.Disposable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Explicit OpenAI-compatible agent loop used when provider-specific reasoning
 * fields must be preserved across tool-call continuations.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
public class OpenAiCompatibleAgenticRuntime {

    private static final String REASONING_CONTENT_PROPERTY = "reasoning_content";

    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
    };

    private final ChatClientFactory chatClientFactory;

    private final AgenticPromptBuilder promptBuilder;

    private final ToolCallbackProvider toolCallbackProvider;

    private final ReactiveAgenticToolRegistry reactiveToolRegistry;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiCompatibleAgenticRuntime(ChatClientFactory chatClientFactory, AgenticPromptBuilder promptBuilder,
                                          @Qualifier("agenticToolCallbackProvider")
                                          ToolCallbackProvider toolCallbackProvider,
                                          ReactiveAgenticToolRegistry reactiveToolRegistry) {
        this.chatClientFactory = chatClientFactory;
        this.promptBuilder = promptBuilder;
        this.toolCallbackProvider = toolCallbackProvider;
        this.reactiveToolRegistry = reactiveToolRegistry;
    }

    /**
     * Return whether this runtime handles the prepared chat: it requires tool calling to
     * be enabled and the resolved model's provider to be OpenAI-compatible.
     *
     * @param prepared the prepared chat request
     * @return {@code true} if this runtime should handle the request
     */
    public boolean supports(AgenticPreparedChatBO prepared) {
        if (Objects.isNull(prepared) || !prepared.toolCallingEnabled()) {
            return false;
        }
        ModelProviderBO provider = chatClientFactory.resolveProviderForModel(prepared.model(), tenantId(prepared));
        return Objects.nonNull(provider)
                && AgenticModelProviderTypeEnum.OPENAI_COMPATIBLE.equals(provider.getProviderType());
    }

    /**
     * Run the agent loop as a stream, emitting one frame per delta (reasoning, content,
     * or tool round boundary) and terminating when the model stops calling tools.
     *
     * @param prepared the prepared chat request
     * @return a flux of stream frames
     */
    public Flux<AgenticRuntimeStreamFrame> stream(AgenticPreparedChatBO prepared) {
        return Flux.create(sink -> {
            ModelProviderBO provider = requireProvider(prepared);
            OpenAIClient client = createClient(provider);
            OpenAIClientAsync asyncClient = client.async();
            Disposable execution = streamRound(asyncClient, prepared, initialMessages(prepared),
                            toolCallbacksByName(), sink, 0)
                    .doFinally(signal -> client.close())
                    .subscribe(ignored -> {
                    }, sink::error, sink::complete);
            sink.onCancel(execution::dispose);
        });
    }

    /**
     * Run the agent loop to completion, executing tool calls and re-prompting until the
     * model produces a final answer or the round limit is exceeded. Preserves the
     * provider's reasoning content across tool-call continuations.
     *
     * @param prepared the prepared chat request
     * @return the final assistant content and finish reason
     * @throws IllegalStateException if the agent loop exceeds the max rounds
     */
    public Mono<AgenticRuntimeResult> call(AgenticPreparedChatBO prepared) {
        return Mono.defer(() -> {
            ModelProviderBO provider = requireProvider(prepared);
            OpenAIClient client = createClient(provider);
            OpenAIClientAsync asyncClient = client.async();
            List<ChatCompletionMessageParam> messages = initialMessages(prepared);
            return callRound(asyncClient, prepared, messages, toolCallbacksByName(), 0)
                    .doFinally(signal -> client.close());
        });
    }

    private Mono<AgenticRuntimeResult> callRound(OpenAIClientAsync client,
                                                  AgenticPreparedChatBO prepared,
                                                  List<ChatCompletionMessageParam> messages,
                                                  Map<String, ToolCallback> callbacks,
                                                  int round) {
        if (round >= AgenticConstant.ToolLimit.MAX_AGENT_LOOP_ROUNDS) {
            return Mono.error(new IllegalStateException("Agent loop exceeded max rounds: "
                    + AgenticConstant.ToolLimit.MAX_AGENT_LOOP_ROUNDS));
        }
        return Mono.fromFuture(client.chat().completions().create(buildRequest(prepared, messages, true, false)))
                .flatMap(completion -> {
                    if (completion.choices().isEmpty()) {
                        return Mono.just(new AgenticRuntimeResult("", AgenticConstant.Chat.FINISH_REASON_STOP));
                    }
                    ChatCompletion.Choice choice = completion.choices().get(0);
                    ChatCompletionMessage message = choice.message();
                    String finishReason = choice.finishReason().asString();
                    List<ToolCall> toolCalls = toolCallsFromMessage(message);
                    if (toolCalls.isEmpty()) {
                        return Mono.just(new AgenticRuntimeResult(message.content().orElse(""), finishReason));
                    }
                    messages.add(assistantToolCallMessage(message.content().orElse(null),
                            extractReasoningContent(message._additionalProperties()), toolCalls));
                    return executeToolCallsReactive(callbacks, prepared, toolCalls)
                            .collectList()
                            .flatMap(toolMessages -> {
                                messages.addAll(toolMessages);
                                return callRound(client, prepared, messages, callbacks, round + 1);
                            });
                });
    }

    private Mono<Void> streamRound(OpenAIClientAsync client,
                                   AgenticPreparedChatBO prepared,
                                   List<ChatCompletionMessageParam> messages,
                                   Map<String, ToolCallback> callbacks,
                                   FluxSink<AgenticRuntimeStreamFrame> sink,
                                   int round) {
        if (sink.isCancelled()) {
            return Mono.empty();
        }
        if (round >= AgenticConstant.ToolLimit.MAX_AGENT_LOOP_ROUNDS) {
            return Mono.error(new IllegalStateException("Agent loop exceeded max rounds: "
                    + AgenticConstant.ToolLimit.MAX_AGENT_LOOP_ROUNDS));
        }
        return streamOneRound(client, prepared, messages, sink)
                .flatMap(result -> {
                    if (result.toolCalls().isEmpty()) {
                        if (StringUtils.isNotBlank(result.finishReason()) && !sink.isCancelled()) {
                            sink.next(new AgenticRuntimeStreamFrame(AgenticStreamDelta.empty(), result.finishReason()));
                        }
                        return Mono.empty();
                    }
                    messages.add(assistantToolCallMessage(result.content(), result.reasoningContent(),
                            result.toolCalls()));
                    return executeToolCallsReactive(callbacks, prepared, result.toolCalls())
                            .collectList()
                            .flatMap(toolMessages -> {
                                messages.addAll(toolMessages);
                                if (!sink.isCancelled()) {
                                    sink.next(new AgenticRuntimeStreamFrame(AgenticStreamDelta.empty(), null));
                                }
                                return streamRound(client, prepared, messages, callbacks, sink, round + 1);
                            });
                });
    }

    private Mono<StreamRoundResult> streamOneRound(OpenAIClientAsync client,
                                                    AgenticPreparedChatBO prepared,
                                                    List<ChatCompletionMessageParam> messages,
                                                    FluxSink<AgenticRuntimeStreamFrame> sink) {
        StringBuilder content = new StringBuilder();
        StringBuilder reasoningContent = new StringBuilder();
        Map<Long, StreamingToolCallBuilder> toolCallBuilders = new LinkedHashMap<>();
        AtomicReference<String> finishReason = new AtomicReference<>();

        ChatCompletionCreateParams request = buildRequest(prepared, messages, true, true);
        return Mono.create(resultSink -> {
            AsyncStreamResponse<ChatCompletionChunk> response = client.chat().completions().createStreaming(request);
            AtomicBoolean terminated = new AtomicBoolean();
            AtomicBoolean cancelled = new AtomicBoolean();
            resultSink.onCancel(() -> {
                cancelled.set(true);
                response.close();
            });
            response.subscribe(new AsyncStreamResponse.Handler<>() {
                @Override
                public void onNext(ChatCompletionChunk chunk) {
                    if (sink.isCancelled() || cancelled.get()) {
                        response.close();
                        return;
                    }
                    if (sink.requestedFromDownstream() == 0) {
                        response.close();
                        resultSink.error(new IllegalStateException("Agentic model stream consumer is too slow"));
                        return;
                    }
                    if (chunk.choices().isEmpty()) {
                        return;
                    }
                    for (ChatCompletionChunk.Choice choice : chunk.choices()) {
                        ChatCompletionChunk.Choice.Delta delta = choice.delta();
                        String reasoning = extractReasoningContent(delta._additionalProperties());
                        if (StringUtils.isNotEmpty(reasoning)) {
                            reasoningContent.append(reasoning);
                            sink.next(new AgenticRuntimeStreamFrame(new AgenticStreamDelta(null, reasoning), null));
                        }
                        String text = delta.content().orElse(null);
                        if (StringUtils.isNotEmpty(text)) {
                            content.append(text);
                            sink.next(new AgenticRuntimeStreamFrame(new AgenticStreamDelta(text, null), null));
                        }
                        mergeToolCalls(toolCallBuilders, delta);
                        if (choice.finishReason().isPresent()) {
                            finishReason.set(choice.finishReason().get().asString());
                        }
                    }
                }

                @Override
                public void onComplete(Optional<Throwable> error) {
                    if (!terminated.compareAndSet(false, true)) {
                        return;
                    }
                    response.close();
                    if (error.isPresent()) {
                        resultSink.error(error.get());
                        return;
                    }
                    resultSink.success(new StreamRoundResult(content.toString(), reasoningContent.toString(),
                            toolCallBuilders.values().stream()
                                    .sorted(Comparator.comparingLong(StreamingToolCallBuilder::index))
                                    .map(StreamingToolCallBuilder::build)
                                    .filter(Objects::nonNull)
                                    .toList(),
                            finishReason.get()));
                }
            });
        });
    }

    private ChatCompletionCreateParams buildRequest(AgenticPreparedChatBO prepared,
                                                    List<ChatCompletionMessageParam> messages,
                                                    boolean includeTools, boolean stream) {
        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
                .model(prepared.model())
                .messages(messages);
        if (Objects.nonNull(prepared.temperature())) {
            builder.temperature(prepared.temperature());
        }
        if (Objects.nonNull(prepared.maxTokens())) {
            builder.maxTokens(prepared.maxTokens().longValue());
        }
        if (includeTools && prepared.toolCallingEnabled()) {
            builder.tools(chatCompletionTools());
        }
        if (stream) {
            builder.streamOptions(ChatCompletionStreamOptions.builder().includeUsage(true).build());
        }
        return builder.build();
    }

    private List<ChatCompletionMessageParam> initialMessages(AgenticPreparedChatBO prepared) {
        List<ChatCompletionMessageParam> messages = new ArrayList<>();
        String systemPrompt = promptBuilder.buildSystemPrompt(prepared);
        if (StringUtils.isNotBlank(systemPrompt)) {
            messages.add(systemMessage(systemPrompt));
        }
        appendMemoryMessages(messages, prepared.memoryHistory());
        messages.add(userMessage(prepared.userMessage()));
        return messages;
    }

    private void appendMemoryMessages(List<ChatCompletionMessageParam> messages, List<MessageBO> memoryHistory) {
        List<MessageBO> rows = new ArrayList<>(Objects.requireNonNullElse(memoryHistory, List.of()));
        stripTrailingUserMessages(rows);
        for (MessageBO row : rows) {
            if (Objects.isNull(row) || StringUtils.isBlank(row.getRole()) || Objects.isNull(row.getContent())) {
                continue;
            }
            AgenticMessageContent content = row.getContent();
            String text = StringUtils.trimToNull(content.getText());
            if (StringUtils.isBlank(text)) {
                continue;
            }
            switch (row.getRole().toLowerCase()) {
                case AgenticConstant.Chat.ROLE_SYSTEM -> messages.add(systemMessage(text));
                case AgenticConstant.Chat.ROLE_USER -> messages.add(userMessage(text));
                case AgenticConstant.Chat.ROLE_ASSISTANT -> messages.add(assistantTextMessage(text));
                default -> {
                    // Ignore unknown persisted roles.
                }
            }
        }
    }

    private void stripTrailingUserMessages(List<MessageBO> messages) {
        while (!messages.isEmpty()) {
            MessageBO tail = messages.get(messages.size() - 1);
            if (Objects.nonNull(tail) && AgenticConstant.Chat.ROLE_USER.equalsIgnoreCase(tail.getRole())) {
                messages.remove(messages.size() - 1);
                continue;
            }
            break;
        }
    }

    private ChatCompletionMessageParam systemMessage(String text) {
        return ChatCompletionMessageParam.ofSystem(ChatCompletionSystemMessageParam.builder()
                .content(text)
                .role(JsonValue.from(AgenticConstant.Chat.ROLE_SYSTEM))
                .build());
    }

    private ChatCompletionMessageParam userMessage(String text) {
        return ChatCompletionMessageParam.ofUser(ChatCompletionUserMessageParam.builder()
                .content(text)
                .role(JsonValue.from(AgenticConstant.Chat.ROLE_USER))
                .build());
    }

    private ChatCompletionMessageParam assistantTextMessage(String text) {
        return ChatCompletionMessageParam.ofAssistant(ChatCompletionAssistantMessageParam.builder()
                .role(JsonValue.from(AgenticConstant.Chat.ROLE_ASSISTANT))
                .content(text)
                .build());
    }

    ChatCompletionMessageParam assistantToolCallMessage(String content, String reasoningContent,
                                                        List<ToolCall> toolCalls) {
        ChatCompletionAssistantMessageParam.Builder builder = ChatCompletionAssistantMessageParam.builder()
                .role(JsonValue.from(AgenticConstant.Chat.ROLE_ASSISTANT))
                .toolCalls(toolCalls.stream()
                        .map(this::toMessageToolCall)
                        .toList());
        if (StringUtils.isNotEmpty(content)) {
            builder.content(content);
        }
        if (StringUtils.isNotEmpty(reasoningContent)) {
            builder.putAdditionalProperty(REASONING_CONTENT_PROPERTY, JsonValue.from(reasoningContent));
        }
        return ChatCompletionMessageParam.ofAssistant(builder.build());
    }

    private ChatCompletionMessageParam toolMessage(String toolCallId, String result) {
        return ChatCompletionMessageParam.ofTool(ChatCompletionToolMessageParam.builder()
                .role(JsonValue.from(AgenticConstant.Chat.ROLE_TOOL))
                .toolCallId(toolCallId)
                .content(StringUtils.defaultString(result))
                .build());
    }

    private ChatCompletionMessageToolCall toMessageToolCall(ToolCall toolCall) {
        return ChatCompletionMessageToolCall.ofFunction(ChatCompletionMessageFunctionToolCall.builder()
                .id(toolCall.id())
                .function(ChatCompletionMessageFunctionToolCall.Function.builder()
                        .name(toolCall.name())
                        .arguments(StringUtils.defaultIfBlank(toolCall.arguments(), "{}"))
                        .build())
                .build());
    }

    private Flux<ChatCompletionMessageParam> executeToolCallsReactive(Map<String, ToolCallback> callbacks,
                                                                       AgenticPreparedChatBO prepared,
                                                                       List<ToolCall> toolCalls) {
        ToolContext toolContext = new ToolContext(prepared.toolContext());
        Map<String, ReactiveAgenticTool> reactiveTools = reactiveTools();
        return Flux.fromIterable(toolCalls)
                .concatMap(toolCall -> Mono.defer(() -> {
                    ReactiveAgenticTool reactiveTool = reactiveTools.get(toolCall.name());
                    if (reactiveTool != null) {
                        return reactiveTool.call(StringUtils.defaultIfBlank(toolCall.arguments(), "{}"), toolContext)
                                .map(this::serializeToolResult)
                                .map(result -> toolMessage(toolCall.id(), result));
                    }
                    ToolCallback callback = callbacks.get(toolCall.name());
                    if (callback == null) {
                        return Mono.error(new IllegalStateException("No tool callback found for tool: "
                                + toolCall.name()));
                    }
                    return Mono.fromSupplier(() -> callback.call(
                                    StringUtils.defaultIfBlank(toolCall.arguments(), "{}"), toolContext))
                            .map(result -> toolMessage(toolCall.id(), result));
                }));
    }

    private Map<String, ToolCallback> toolCallbacksByName() {
        Map<String, ToolCallback> callbacks = new HashMap<>();
        for (ToolCallback callback : toolCallbackProvider.getToolCallbacks()) {
            if (Objects.nonNull(callback) && Objects.nonNull(callback.getToolDefinition())
                    && StringUtils.isNotBlank(callback.getToolDefinition().name())) {
                callbacks.put(callback.getToolDefinition().name(), callback);
            }
        }
        return callbacks;
    }

    private List<ChatCompletionTool> chatCompletionTools() {
        List<ChatCompletionTool> tools = new ArrayList<>();
        Map<String, ReactiveAgenticTool> reactive = reactiveTools();
        Set<String> reactiveNames = reactive.keySet();
        for (ToolCallback callback : toolCallbackProvider.getToolCallbacks()) {
            if (Objects.isNull(callback) || Objects.isNull(callback.getToolDefinition())) {
                continue;
            }
            ToolDefinition definition = callback.getToolDefinition();
            if (reactiveNames.contains(definition.name())) {
                continue;
            }
            FunctionDefinition functionDefinition = FunctionDefinition.builder()
                    .name(definition.name())
                    .description(StringUtils.defaultString(definition.description()))
                    .parameters(functionParameters(definition))
                    .build();
            tools.add(ChatCompletionTool.ofFunction(ChatCompletionFunctionTool.builder()
                    .function(functionDefinition)
                    .build()));
        }
        for (ReactiveAgenticTool tool : reactive.values()) {
            ToolDefinition definition = tool.definition();
            FunctionDefinition functionDefinition = FunctionDefinition.builder()
                    .name(definition.name())
                    .description(StringUtils.defaultString(definition.description()))
                    .parameters(functionParameters(definition))
                    .build();
            tools.add(ChatCompletionTool.ofFunction(ChatCompletionFunctionTool.builder()
                    .function(functionDefinition)
                    .build()));
        }
        return tools;
    }

    private String serializeToolResult(Object result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Failed to serialize agentic tool result", error);
        }
    }

    private Map<String, ReactiveAgenticTool> reactiveTools() {
        return reactiveToolRegistry == null ? Map.of() : reactiveToolRegistry.tools();
    }

    private FunctionParameters functionParameters(ToolDefinition definition) {
        FunctionParameters.Builder builder = FunctionParameters.builder();
        String schema = definition.inputSchema();
        if (StringUtils.isBlank(schema)) {
            return builder.build();
        }
        try {
            Map<String, Object> schemaMap = objectMapper.readValue(schema, MAP_TYPE_REFERENCE);
            schemaMap.forEach((key, value) -> builder.putAdditionalProperty(key, JsonValue.from(value)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse tool schema for tool: " + definition.name(), e);
        }
        return builder.build();
    }

    private List<ToolCall> toolCallsFromMessage(ChatCompletionMessage message) {
        return message.toolCalls()
                .orElse(List.of())
                .stream()
                .filter(ChatCompletionMessageToolCall::isFunction)
                .map(ChatCompletionMessageToolCall::asFunction)
                .map(toolCall -> new ToolCall(toolCall.id(), toolCall.function().name(),
                        toolCall.function().arguments()))
                .toList();
    }

    private void mergeToolCalls(Map<Long, StreamingToolCallBuilder> builders,
                                ChatCompletionChunk.Choice.Delta delta) {
        delta.toolCalls().orElse(List.of()).forEach(toolCall -> {
            StreamingToolCallBuilder builder = builders.computeIfAbsent(toolCall.index(),
                    ignored -> new StreamingToolCallBuilder(toolCall.index()));
            builder.merge(toolCall);
        });
    }

    private String extractReasoningContent(Map<String, JsonValue> additionalProperties) {
        if (Objects.isNull(additionalProperties) || !additionalProperties.containsKey(REASONING_CONTENT_PROPERTY)) {
            return null;
        }
        JsonValue value = additionalProperties.get(REASONING_CONTENT_PROPERTY);
        if (Objects.isNull(value)) {
            return null;
        }
        try {
            return value.convert(String.class);
        } catch (RuntimeException e) {
            log.debug("Reasoning content extraction failed", e);
            return null;
        }
    }

    private ModelProviderBO requireProvider(AgenticPreparedChatBO prepared) {
        ModelProviderBO provider = chatClientFactory.resolveProviderForModel(prepared.model(), tenantId(prepared));
        if (Objects.isNull(provider)
                || !AgenticModelProviderTypeEnum.OPENAI_COMPATIBLE.equals(provider.getProviderType())) {
            throw new IllegalStateException("OpenAI-compatible provider is required for model: " + prepared.model());
        }
        return provider;
    }

    private Long tenantId(AgenticPreparedChatBO prepared) {
        Object value = prepared.toolContext().get(AgenticConstant.ToolContextKey.TENANT_ID);
        return value instanceof Number number ? number.longValue() : null;
    }

    private OpenAIClient createClient(ModelProviderBO provider) {
        return OpenAIOkHttpClient.builder()
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .build();
    }

    record ToolCall(String id, String name, String arguments) {
        ToolCall {
            if (StringUtils.isBlank(id)) {
                throw new IllegalStateException("Tool call id is required");
            }
            if (StringUtils.isBlank(name)) {
                throw new IllegalStateException("Tool call name is required");
            }
            arguments = StringUtils.defaultIfBlank(arguments, "{}");
        }
    }

    private record StreamRoundResult(String content, String reasoningContent, List<ToolCall> toolCalls,
                                     String finishReason) {
    }

    private static class StreamingToolCallBuilder {

        private final long index;
        private final StringBuilder arguments = new StringBuilder();
        private String id;
        private String name;

        StreamingToolCallBuilder(long index) {
            this.index = index;
        }

        void merge(ChatCompletionChunk.Choice.Delta.ToolCall toolCall) {
            toolCall.id().filter(StringUtils::isNotBlank).ifPresent(value -> id = value);
            toolCall.function().ifPresent(function -> {
                function.name().filter(StringUtils::isNotBlank).ifPresent(value -> name = value);
                function.arguments().ifPresent(arguments::append);
            });
        }

        ToolCall build() {
            return new ToolCall(id, name, arguments.toString());
        }

        long index() {
            return index;
        }

    }

}
