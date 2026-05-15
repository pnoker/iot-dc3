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
package io.github.pnoker.common.agentic.service.impl;

import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletionChunk;
import io.github.pnoker.common.agentic.config.AgenticProperties;
import io.github.pnoker.common.agentic.config.ChatClientConfig;
import io.github.pnoker.common.agentic.config.ChatClientFactory;
import io.github.pnoker.common.agentic.context.AgenticRequestContext;
import io.github.pnoker.common.agentic.entity.bo.MessageBO;
import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import io.github.pnoker.common.agentic.entity.model.SessionExt;
import io.github.pnoker.common.agentic.entity.request.ChatCompletionRequest;
import io.github.pnoker.common.agentic.entity.request.ChatMessageDTO;
import io.github.pnoker.common.agentic.entity.response.ChatCompletionChunkResponse;
import io.github.pnoker.common.agentic.entity.response.ChatCompletionResponse;
import io.github.pnoker.common.agentic.service.AgenticChatService;
import io.github.pnoker.common.agentic.service.AttachmentService;
import io.github.pnoker.common.agentic.service.MessageService;
import io.github.pnoker.common.agentic.service.SessionService;
import io.github.pnoker.common.agentic.skill.SkillDefinition;
import io.github.pnoker.common.agentic.skill.SkillRegistry;
import io.github.pnoker.common.agentic.util.AgenticConversationIds;
import io.github.pnoker.common.agentic.util.AgenticTokenEstimator;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceQuery;
import io.github.pnoker.common.facade.entity.query.FacadeDriverQuery;
import io.github.pnoker.common.facade.entity.query.FacadePointQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Default agentic chat orchestration service.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class AgenticChatServiceImpl implements AgenticChatService {

    private final ChatClientFactory chatClientFactory;

    private final SkillRegistry skillRegistry;

    private final SessionService sessionService;

    private final MessageService messageService;

    private final AttachmentService attachmentService;

    private final DeviceFacade deviceFacade;

    private final DriverFacade driverFacade;

    private final PointFacade pointFacade;

    private final AgenticProperties properties;

    private final ToolCallbackProvider toolCallbackProvider;

    private final ObjectMapper objectMapper;

    public AgenticChatServiceImpl(ChatClientFactory chatClientFactory, SkillRegistry skillRegistry, SessionService sessionService,
                                  MessageService messageService, AttachmentService attachmentService,
                                  DeviceFacade deviceFacade, DriverFacade driverFacade, PointFacade pointFacade,
                                  AgenticProperties properties,
                                  @Qualifier("agenticToolCallbackProvider") ToolCallbackProvider toolCallbackProvider,
                                  ObjectMapper objectMapper) {
        this.chatClientFactory = chatClientFactory;
        this.skillRegistry = skillRegistry;
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.attachmentService = attachmentService;
        this.deviceFacade = deviceFacade;
        this.driverFacade = driverFacade;
        this.pointFacade = pointFacade;
        this.properties = properties;
        this.toolCallbackProvider = toolCallbackProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public Flux<ServerSentEvent<String>> streamChatCompletion(ChatCompletionRequest request,
                                                              RequestHeader.UserHeader userHeader) {
        return Flux.defer(() -> {
            PreparedChatRequest prepared = prepare(request, userHeader, "stream");
            ChatClient.ChatClientRequestSpec promptSpec = buildPrompt(prepared);
            persistUserMessage(prepared, userHeader);

            String chatId = newChatId();
            long created = Instant.now().getEpochSecond();
            StringBuilder assistantContent = new StringBuilder();

            Flux<StreamDelta> contentFlux = promptSpec.stream().chatResponse()
                    .doOnSubscribe(subscription -> AgenticRequestContext.set(userHeader))
                    .map(this::extractStreamDelta)
                    .filter(StreamDelta::hasContent)
                    .doOnNext(delta -> assistantContent.append(delta.content()))
                    .doOnComplete(() -> persistAssistantMessage(prepared, assistantContent.toString(), userHeader))
                    .doFinally(signalType -> AgenticRequestContext.clear());

            Flux<ServerSentEvent<String>> initialEvents = Flux.fromIterable(initialEvents(prepared));
            Flux<ServerSentEvent<String>> responseEvents = contentFlux
                    .flatMap(chunk -> Flux.fromIterable(chunkEvents(prepared, chatId, created, chunk)))
                    .onErrorResume(error -> {
                        log.warn("Agentic stream chat failed, conversationId={}, model={}",
                                prepared.scopedConversationId(), prepared.model(), error);
                        return Flux.just(ServerSentEvent.<String>builder()
                                .data(formatEvent("error", "Request failed", error.getMessage(), "agentic"))
                                .build());
                    });

            return initialEvents
                    .concatWith(responseEvents)
                    .concatWith(Mono.just(ServerSentEvent.<String>builder()
                            .data(formatFinalChunk(chatId, created, prepared.model()))
                            .build()))
                    .concatWith(Mono.just(ServerSentEvent.<String>builder().data("[DONE]").build()));
        }).doFinally(signalType -> AgenticRequestContext.clear()).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<ChatCompletionResponse> chatCompletion(ChatCompletionRequest request, RequestHeader.UserHeader userHeader) {
        return Mono.fromCallable(() -> {
            try {
                PreparedChatRequest prepared = prepare(request, userHeader, "blocking");
                ChatClient.ChatClientRequestSpec promptSpec = buildPrompt(prepared);
                persistUserMessage(prepared, userHeader);

                AgenticRequestContext.set(userHeader);
                String content;
                content = promptSpec.call().content();
                persistAssistantMessage(prepared, content, userHeader);

                return ChatCompletionResponse.builder()
                        .id(newChatId())
                        .object("chat.completion")
                        .created(Instant.now().getEpochSecond())
                        .model(prepared.model())
                        .choices(List.of(ChatCompletionResponse.Choice.builder()
                                .index(0)
                                .message(new ChatCompletionResponse.Message("assistant", content))
                                .finishReason("stop")
                                .build()))
                        .usage(new ChatCompletionResponse.Usage(0, 0, 0))
                        .build();
            } finally {
                AgenticRequestContext.clear();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private PreparedChatRequest prepare(ChatCompletionRequest request, RequestHeader.UserHeader userHeader, String mode) {
        validateRequest(request);

        String rawUserMessage = extractLastUserMessage(request);
        List<Long> attachments = normalizeAttachments(request);
        String attachmentContext = attachmentService.summarize(attachments, userHeader);
        String conversationId = resolveConversationId(request);
        String scopedConversationId = AgenticConversationIds.scope(userHeader.getTenantId(), userHeader.getUserId(),
                conversationId);
        SkillDefinition skill = resolveSkill(request.getSkill(), rawUserMessage);
        String effectiveSkillName = Objects.isNull(skill) ? null : skill.getName();
        List<String> toolNames = Objects.isNull(skill) ? List.of() : skillRegistry.getEnabledToolNames(skill.getName());
        String skillSystemPrompt = Objects.isNull(skill) ? null : buildSkillSystemPrompt(skill);
        String model = chatClientFactory.resolveModel(request.getModel());
        boolean toolCallingEnabled = properties.isToolCallingEnabled() && chatClientFactory.supportsToolCall(model);
        Queue<AgenticRequestContext.ToolEvent> toolEvents = new ConcurrentLinkedQueue<>();
        Map<String, Object> toolContext = new HashMap<>();
        toolContext.put(AgenticConstant.ToolContextKey.TENANT_ID, userHeader.getTenantId());
        toolContext.put(AgenticConstant.ToolContextKey.USER_ID, userHeader.getUserId());
        toolContext.put(AgenticConstant.ToolContextKey.CONVERSATION_ID, scopedConversationId);
        toolContext.put(AgenticConstant.ToolContextKey.CONFIRM_ACTIONS,
                !Boolean.FALSE.equals(request.getConfirmActions()));
        toolContext.put(AgenticConstant.ToolContextKey.TOOL_EVENTS, toolEvents);
        String directContext = buildDirectContext(effectiveSkillName, userHeader, toolEvents);
        List<AgenticMessageContent.Context> contexts = buildContexts(attachmentContext, directContext);
        String requestSystemContext = buildRequestSystemContext(contexts);
        List<MessageBO> memoryHistory = loadMemoryHistory(scopedConversationId);
        AgenticRequestContext.setMemoryHistory(scopedConversationId, memoryHistory);
        AgenticMessageContent.Tokens inputTokens = buildInputTokens(rawUserMessage, skillSystemPrompt,
                requestSystemContext, contexts, memoryHistory);

        log.debug(
                "Agentic chat request received, mode={}, model={}, messageCount={}, conversationIdPresent={}, skill={}, tenantId={}, userId={}",
                mode, model, request.getMessages().size(), StringUtils.isNotBlank(request.getConversationId()),
                Objects.isNull(skill) ? null : skill.getName(), userHeader.getTenantId(), userHeader.getUserId());

        touchSession(scopedConversationId, conversationId, userHeader, model, buildSessionExt(request, model));

        return new PreparedChatRequest(rawUserMessage, scopedConversationId, skillSystemPrompt,
                requestSystemContext, normalizeToolNames(toolNames), model, effectiveSkillName, toolContext,
                request.getTemperature(), request.getMaxTokens(), skill, toolEvents,
                toolCallingEnabled, Boolean.TRUE.equals(request.getReasoning()), StringUtils.isNotBlank(directContext),
                attachments, contexts, inputTokens, new ArrayList<>());
    }

    private void validateRequest(ChatCompletionRequest request) {
        if (Objects.isNull(request)) {
            throw new RequestException("Chat completion request is required");
        }
        if (Objects.isNull(request.getMessages()) || request.getMessages().isEmpty()) {
            throw new RequestException("Chat messages are required");
        }
        if (Objects.nonNull(request.getTemperature()) && (request.getTemperature() < 0.0 || request.getTemperature() > 2.0)) {
            throw new RequestException("Temperature must be between 0.0 and 2.0");
        }
        if (Objects.nonNull(request.getMaxTokens()) && request.getMaxTokens() < 1) {
            throw new RequestException("Max tokens must be greater than 0");
        }
    }

    private String extractLastUserMessage(ChatCompletionRequest request) {
        return request.getMessages()
                .stream()
                .filter(message -> Objects.nonNull(message) && "user".equals(message.getRole()))
                .map(ChatMessageDTO::getContent)
                .filter(StringUtils::isNotBlank)
                .reduce((first, second) -> second)
                .orElseThrow(() -> new RequestException("A non-empty user message is required"));
    }

    private String resolveConversationId(ChatCompletionRequest request) {
        String conversationId = StringUtils.trimToNull(request.getConversationId());
        if (Objects.isNull(conversationId)) {
            throw new RequestException("conversationId is required — clients must generate and reuse "
                    + "a stable conversationId so chat memory can be replayed across turns.");
        }
        return conversationId;
    }

    private SkillDefinition resolveSkill(String skillName, String userMessage) {
        String normalizedSkillName = StringUtils.trimToNull(skillName);
        if (Objects.isNull(normalizedSkillName)) {
            normalizedSkillName = inferSkillName(userMessage);
        }
        if (Objects.isNull(normalizedSkillName)) {
            return null;
        }
        SkillDefinition skill = skillRegistry.get(normalizedSkillName);
        if (Objects.isNull(skill)) {
            log.warn("Agentic skill not found, skill={}", normalizedSkillName);
            throw new RequestException("Agentic skill does not exist: {}", normalizedSkillName);
        }
        log.debug("Agentic skill activated, skill={}, toolNames={}", skill.getName(), skill.getTools());
        return skill;
    }

    private String inferSkillName(String userMessage) {
        String text = normalizeInferenceText(userMessage);
        if (StringUtils.containsAny(text, "write", "control", "command", "set ", "read ", "写入", "控制", "命令",
                "下发", "读取")) {
            return "device-control";
        }
        if (StringUtils.containsAny(text, "value", "history", "trend", "event", "alarm", "monitor", "data", "point",
                "值", "历史", "趋势", "事件", "告警", "报警", "监控", "数据", "位号")) {
            return "data-monitor";
        }
        if (StringUtils.containsAny(text, "device", "driver", "profile", "status", "list", "search", "设备", "驱动",
                "模板", "状态", "列表", "查询", "搜索")) {
            return "device-query";
        }
        return null;
    }

    private String normalizeInferenceText(String userMessage) {
        String text = StringUtils.defaultString(userMessage).toLowerCase(Locale.ROOT);
        int confirmationIndex = text.indexOf("before executing any write");
        if (confirmationIndex >= 0) {
            text = text.substring(0, confirmationIndex);
        }
        int attachmentIndex = text.indexOf("attached files available");
        if (attachmentIndex >= 0) {
            text = text.substring(0, attachmentIndex);
        }
        return text;
    }

    private List<String> normalizeToolNames(List<String> toolNames) {
        if (Objects.isNull(toolNames) || toolNames.isEmpty()) {
            return List.of();
        }
        return toolNames.stream().filter(StringUtils::isNotBlank).distinct().toList();
    }

    private String buildSkillSystemPrompt(SkillDefinition skill) {
        List<String> sections = new ArrayList<>();
        if (StringUtils.isNotBlank(skill.getSystemPromptAddition())) {
            sections.add(skill.getSystemPromptAddition().trim());
        }
        if (Objects.nonNull(skill.getExamples()) && !skill.getExamples().isEmpty()) {
            StringBuilder examples = new StringBuilder("Examples:");
            for (SkillDefinition.SkillExample example : skill.getExamples()) {
                if (Objects.isNull(example) || StringUtils.isAnyBlank(example.getUser(), example.getAssistant())) {
                    continue;
                }
                examples.append("\n- User: ").append(example.getUser().trim())
                        .append("\n  Assistant: ").append(example.getAssistant().trim());
            }
            sections.add(examples.toString());
        }
        return sections.isEmpty() ? null : String.join("\n\n", sections);
    }

    private List<AgenticMessageContent.Context> buildContexts(String attachmentContext, String directContext) {
        List<AgenticMessageContent.Context> contexts = new ArrayList<>();
        if (StringUtils.isNotBlank(attachmentContext)) {
            contexts.add(AgenticMessageContent.Context.of("attachment", attachmentContext.trim()));
        }
        if (StringUtils.isNotBlank(directContext)) {
            contexts.add(AgenticMessageContent.Context.of("backend", directContext.trim()));
        }
        return contexts;
    }

    private String buildRequestSystemContext(List<AgenticMessageContent.Context> contexts) {
        List<String> sections = new ArrayList<>();
        for (AgenticMessageContent.Context context : contexts) {
            if (Objects.isNull(context) || StringUtils.isBlank(context.getContent())) {
                continue;
            }
            if ("backend".equals(context.getType())) {
                sections.add("Backend context:\n" + context.getContent().trim()
                        + "\n\nUse the backend context above as the source of truth. Format the answer as Markdown.");
            } else if ("attachment".equals(context.getType())) {
                sections.add("Attachment context:\n" + context.getContent().trim()
                        + "\n\nUse only the metadata above unless a future multimodal model endpoint provides file contents.");
            } else {
                sections.add(context.getContent().trim());
            }
        }
        return sections.isEmpty() ? null : String.join("\n\n", sections);
    }

    private AgenticMessageContent.Tokens buildInputTokens(String userMessage, String skillSystemPrompt,
                                                          String requestSystemContext,
                                                          List<AgenticMessageContent.Context> contexts,
                                                          List<MessageBO> memoryHistory) {
        int textTokens = AgenticTokenEstimator.estimate(userMessage);
        int contextTokens = contexts.stream()
                .map(AgenticMessageContent.Context::getContent)
                .mapToInt(AgenticTokenEstimator::estimate)
                .sum();
        int systemTokens = AgenticTokenEstimator.estimate(ChatClientConfig.SYSTEM_PROMPT)
                + AgenticTokenEstimator.estimate(skillSystemPrompt)
                + AgenticTokenEstimator.estimate(systemInstructions(requestSystemContext, contexts));
        int memoryTokens = estimateMemoryTokens(memoryHistory);
        return AgenticMessageContent.Tokens.of(textTokens + contextTokens + systemTokens + memoryTokens, 0,
                textTokens, contextTokens, systemTokens, memoryTokens);
    }

    private String systemInstructions(String requestSystemContext, List<AgenticMessageContent.Context> contexts) {
        if (StringUtils.isBlank(requestSystemContext)) {
            return "";
        }
        List<String> instructions = new ArrayList<>();
        if (contexts.stream().anyMatch(context -> "backend".equals(context.getType()))) {
            instructions.add("Use the backend context as the source of truth. Format the answer as Markdown.");
        }
        if (contexts.stream().anyMatch(context -> "attachment".equals(context.getType()))) {
            instructions.add("Use attachment metadata only unless a future multimodal model endpoint provides file contents.");
        }
        return String.join("\n", instructions);
    }

    private List<MessageBO> loadMemoryHistory(String scopedConversationId) {
        if (!properties.isMemoryEnabled()) {
            return List.of();
        }
        try {
            return messageService.loadHistory(scopedConversationId, properties.getHistoryWindowSize());
        } catch (Exception e) {
            log.debug("Agentic memory history load failed, conversationId={}", scopedConversationId, e);
            return List.of();
        }
    }

    private int estimateMemoryTokens(List<MessageBO> history) {
        if (!properties.isMemoryEnabled() || Objects.isNull(history) || history.isEmpty()) {
            return 0;
        }
        return history.stream()
                .map(message -> Objects.nonNull(message.getContent()) ? message.getContent().getText() : null)
                .map(StringUtils::defaultString)
                .filter(StringUtils::isNotBlank)
                .mapToInt(AgenticTokenEstimator::estimate)
                .sum();
    }

    private ChatClient.ChatClientRequestSpec buildPrompt(PreparedChatRequest prepared) {
        ChatClient chatClient = chatClientFactory.getOrCreate(prepared.model());
        ChatClient.ChatClientRequestSpec promptSpec = chatClient.prompt()
                .user(prepared.userMessage())
                .toolContext(prepared.toolContext())
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, prepared.scopedConversationId()));

        String systemPrompt = buildSystemPrompt(prepared);
        if (StringUtils.isNotBlank(systemPrompt)) {
            promptSpec = promptSpec.system(systemPrompt);
        }
        promptSpec = applyToolCallbacks(promptSpec, prepared);
        promptSpec = applyRequestOptions(promptSpec, prepared.model(), prepared.temperature(), prepared.maxTokens());
        return promptSpec;
    }

    private ChatClient.ChatClientRequestSpec applyToolCallbacks(ChatClient.ChatClientRequestSpec promptSpec,
                                                                PreparedChatRequest prepared) {
        if (!prepared.toolCallingEnabled()) {
            return promptSpec;
        }
        promptSpec = promptSpec.toolCallbacks(toolCallbackProvider);
        if (!prepared.toolNames().isEmpty()) {
            promptSpec = promptSpec.toolNames(prepared.toolNames().toArray(String[]::new));
        }
        return promptSpec;
    }

    private String buildSystemPrompt(PreparedChatRequest prepared) {
        List<String> sections = new ArrayList<>();
        sections.add(ChatClientConfig.SYSTEM_PROMPT);
        if (StringUtils.isNotBlank(prepared.skillSystemPrompt())) {
            sections.add(prepared.skillSystemPrompt().trim());
        }
        if (StringUtils.isNotBlank(prepared.requestSystemContext())) {
            sections.add(prepared.requestSystemContext().trim());
        }
        return String.join("\n\n", sections);
    }

    private ChatClient.ChatClientRequestSpec applyRequestOptions(ChatClient.ChatClientRequestSpec promptSpec,
                                                                 String model, Double temperature, Integer maxTokens) {
        ChatOptions.Builder<?> optionsBuilder = chatClientFactory.buildChatOptionsBuilder(model, temperature, maxTokens);
        return Objects.nonNull(optionsBuilder) ? promptSpec.options(optionsBuilder) : promptSpec;
    }

    private String formatChunk(String id, long created, String model, StreamDelta streamDelta) {
        ChatCompletionChunkResponse chunk = ChatCompletionChunkResponse.builder()
                .id(id)
                .object("chat.completion.chunk")
                .created(created)
                .model(model)
                .choices(List.of(ChatCompletionChunkResponse.ChunkChoice.builder()
                        .index(0)
                        .delta(new ChatCompletionChunkResponse.Delta(null, streamDelta.content(),
                                streamDelta.reasoningContent()))
                        .finishReason(null)
                        .build()))
                .build();
        return toJson(chunk);
    }

    private String formatFinalChunk(String id, long created, String model) {
        ChatCompletionChunkResponse chunk = ChatCompletionChunkResponse.builder()
                .id(id)
                .object("chat.completion.chunk")
                .created(created)
                .model(model)
                .choices(List.of(ChatCompletionChunkResponse.ChunkChoice.builder()
                        .index(0)
                        .delta(new ChatCompletionChunkResponse.Delta(null, null, null))
                        .finishReason("stop")
                        .build()))
                .build();
        return toJson(chunk);
    }

    private List<ServerSentEvent<String>> initialEvents(PreparedChatRequest prepared) {
        List<ServerSentEvent<String>> events = new ArrayList<>();
        String skillName = Objects.nonNull(prepared.skillDefinition()) ? prepared.skillDefinition().getName() : "general";
        String skillDescription = Objects.nonNull(prepared.skillDefinition()) ? prepared.skillDefinition().getDescription()
                : "General assistant mode";
        events.add(ServerSentEvent.<String>builder()
                .data(formatEvent("skill", "Auto skill", skillDescription, skillName))
                .build());
        if (prepared.toolCallingEnabled() && !prepared.toolNames().isEmpty()) {
            events.add(ServerSentEvent.<String>builder()
                    .data(formatEvent("tools", "Available tools", String.join(", ", prepared.toolNames()), skillName))
                    .build());
        }
        if (prepared.directContextProvided()) {
            events.add(ServerSentEvent.<String>builder()
                    .data(formatEvent("tool", "Backend context loaded", "Queried DC3 backend before model response",
                            skillName))
                    .build());
        }
        if (prepared.reasoning()) {
            events.add(ServerSentEvent.<String>builder()
                    .data(formatEvent("reasoning", "Thinking", "Reasoning mode requested for this model.", skillName))
                    .build());
        }
        return events;
    }

    private List<ServerSentEvent<String>> chunkEvents(PreparedChatRequest prepared, String chatId, long created,
                                                      StreamDelta streamDelta) {
        List<ServerSentEvent<String>> events = new ArrayList<>();
        AgenticRequestContext.ToolEvent event = prepared.toolEvents().poll();
        while (Objects.nonNull(event)) {
            prepared.toolTraceEvents().add(event);
            events.add(ServerSentEvent.<String>builder()
                    .data(formatEvent("tool", event.description(), event.domain(), event.toolName()))
                    .build());
            event = prepared.toolEvents().poll();
        }
        events.add(ServerSentEvent.<String>builder()
                .data(formatChunk(chatId, created, prepared.model(), streamDelta))
                .build());
        return events;
    }

    private StreamDelta extractStreamDelta(ChatResponse response) {
        if (Objects.isNull(response) || Objects.isNull(response.getResult())) {
            return StreamDelta.empty();
        }
        Generation generation = response.getResult();
        String content = Objects.nonNull(generation.getOutput()) ? generation.getOutput().getText() : null;
        return new StreamDelta(StringUtils.defaultString(content), extractReasoningContent(generation));
    }

    private String extractReasoningContent(Generation generation) {
        if (Objects.isNull(generation) || Objects.isNull(generation.getOutput())) {
            return null;
        }
        Object chunkChoice = generation.getOutput().getMetadata().get("chunkChoice");
        if (Objects.isNull(chunkChoice)) {
            return null;
        }

        if (chunkChoice instanceof ChatCompletionChunk.Choice openAiChunkChoice) {
            Object rawValue = openAiChunkChoice.delta()._additionalProperties().get("reasoning_content");
            if (!(rawValue instanceof JsonValue value)) {
                return null;
            }
            Optional<String> reasoningContent = value.asString();
            return reasoningContent.orElse(null);
        }
        return null;
    }

    private String formatEvent(String type, String title, String detail, String name) {
        Map<String, Object> event = new HashMap<>();
        event.put("object", "agentic.event");
        event.put("type", type);
        event.put("title", StringUtils.defaultString(title));
        event.put("detail", StringUtils.defaultString(detail));
        event.put("name", StringUtils.defaultString(name));
        event.put("created", Instant.now().getEpochSecond());
        return toJson(event);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (DatabindException e) {
            log.error("Agentic response serialization failed, responseType={}", obj.getClass().getSimpleName(), e);
            return "{}";
        }
    }

    private String newChatId() {
        return "chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    private String buildDirectContext(String skillName, RequestHeader.UserHeader userHeader,
                                      Queue<AgenticRequestContext.ToolEvent> toolEvents) {
        if (StringUtils.isBlank(skillName)) {
            return null;
        }
        try {
            if ("device-query".equals(skillName)) {
                return buildDeviceQueryContext(userHeader, toolEvents);
            }
            if ("data-monitor".equals(skillName)) {
                return buildDataMonitorContext(userHeader, toolEvents);
            }
        } catch (Exception e) {
            log.warn("Agentic direct context failed, skill={}, tenantId={}, userId={}", skillName,
                    userHeader.getTenantId(), userHeader.getUserId(), e);
            toolEvents.offer(new AgenticRequestContext.ToolEvent("directContext", "agentic",
                    "Backend context query failed: " + e.getMessage(), Instant.now().toEpochMilli()));
        }
        return null;
    }

    private String buildDeviceQueryContext(RequestHeader.UserHeader userHeader,
                                           Queue<AgenticRequestContext.ToolEvent> toolEvents) {
        FacadeDeviceQuery query = new FacadeDeviceQuery();
        query.setTenantId(userHeader.getTenantId());
        query.setPage(page(1, 50));
        FacadePage<FacadeDeviceBO> page = deviceFacade.selectByPage(query);
        if (Objects.isNull(page) || page.getRecords().isEmpty()) {
            return "No devices found.";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Device page ").append(page.getCurrent()).append('/').append(page.getPages())
                .append(", total=").append(page.getTotal()).append('\n');
        builder.append("| ID | Name | Code | Driver ID | Enabled | Profiles |\n");
        builder.append("| --- | --- | --- | --- | --- | --- |\n");
        page.getRecords().stream().limit(50).forEach(device -> builder.append("| ")
                .append(device.getId()).append(" | ")
                .append(escapeTable(device.getDeviceName())).append(" | ")
                .append(escapeTable(device.getDeviceCode())).append(" | ")
                .append(device.getDriverId()).append(" | ")
                .append(device.getEnableFlag()).append(" | ")
                .append(device.getProfileIds()).append(" |\n"));
        return builder.toString();
    }

    private String buildDataMonitorContext(RequestHeader.UserHeader userHeader,
                                           Queue<AgenticRequestContext.ToolEvent> toolEvents) {
        FacadeDeviceQuery deviceQuery = new FacadeDeviceQuery();
        deviceQuery.setTenantId(userHeader.getTenantId());
        deviceQuery.setPage(page(1, 10));
        FacadePage<FacadeDeviceBO> devices = deviceFacade.selectByPage(deviceQuery);
        FacadeDriverQuery driverQuery = new FacadeDriverQuery();
        driverQuery.setTenantId(userHeader.getTenantId());
        driverQuery.setPage(page(1, 10));
        FacadePage<FacadeDriverBO> drivers = driverFacade.selectByPage(driverQuery);
        FacadePointQuery pointQuery = new FacadePointQuery();
        pointQuery.setTenantId(userHeader.getTenantId());
        pointQuery.setPage(page(1, 10));
        FacadePage<FacadePointBO> points = pointFacade.selectByPage(pointQuery);
        return "Monitoring snapshot:\n"
                + "- devices total: " + total(devices) + "\n"
                + "- drivers total: " + total(drivers) + "\n"
                + "- points total: " + total(points) + "\n"
                + "Sample devices: " + sampleDeviceNames(devices);
    }

    private Pages page(long current, long size) {
        Pages page = new Pages();
        page.setCurrent(current);
        page.setSize(size);
        return page;
    }

    private long total(FacadePage<?> page) {
        return Objects.isNull(page) ? 0 : page.getTotal();
    }

    private String sampleDeviceNames(FacadePage<FacadeDeviceBO> page) {
        if (Objects.isNull(page) || page.getRecords().isEmpty()) {
            return "none";
        }
        return page.getRecords().stream()
                .limit(5)
                .map(FacadeDeviceBO::getDeviceName)
                .filter(StringUtils::isNotBlank)
                .toList()
                .toString();
    }

    private String escapeTable(String value) {
        return StringUtils.defaultString(value).replace("|", "\\|").replace("\n", " ");
    }

    private SessionExt buildSessionExt(ChatCompletionRequest request, String model) {
        if (Objects.isNull(request.getReasoning()) && Objects.isNull(request.getTemperature())
                && Objects.isNull(request.getMaxTokens()) && Objects.isNull(request.getConfirmActions())
                && StringUtils.isBlank(model)) {
            return null;
        }
        SessionExt sessionExt = new SessionExt();
        sessionExt.setModel(model);
        sessionExt.setReasoningEnabled(request.getReasoning());
        sessionExt.setTemperature(request.getTemperature());
        sessionExt.setMaxTokens(request.getMaxTokens());
        sessionExt.setRequireConfirmation(request.getConfirmActions());
        return sessionExt;
    }

    private void touchSession(String scopedConversationId, String conversationId, RequestHeader.UserHeader userHeader,
                              String model, SessionExt sessionExt) {
        try {
            sessionService.touch(scopedConversationId, userHeader.getTenantId(), userHeader.getUserId(), model,
                    sessionExt);
        } catch (Exception e) {
            log.warn(
                    "Agentic session touch failed, tenantId={}, userId={}, conversationId={}",
                    userHeader.getTenantId(), userHeader.getUserId(), conversationId, e);
        }
    }

    private void persistUserMessage(PreparedChatRequest prepared, RequestHeader.UserHeader userHeader) {
        messageService.save(prepared.scopedConversationId(), "user", buildUserContent(prepared), prepared.model(),
                userHeader);
    }

    private void persistAssistantMessage(PreparedChatRequest prepared, String content, RequestHeader.UserHeader userHeader) {
        if (StringUtils.isBlank(content)) {
            return;
        }
        messageService.save(prepared.scopedConversationId(), "assistant", buildAssistantContent(prepared, content), prepared.model(),
                userHeader);
    }

    private AgenticMessageContent buildUserContent(PreparedChatRequest prepared) {
        AgenticMessageContent content = AgenticMessageContent.ofText(prepared.userMessage());
        if (!prepared.attachments().isEmpty()) {
            content.setAttachments(prepared.attachments());
        }
        return content;
    }

    private AgenticMessageContent buildAssistantContent(PreparedChatRequest prepared, String text) {
        List<AgenticRequestContext.ToolEvent> toolEvents = drainToolEvents(prepared);
        List<String> tools = toolEvents.stream()
                .filter(event -> !"agentic".equals(event.domain()))
                .map(AgenticRequestContext.ToolEvent::toolName)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();

        AgenticMessageContent content = AgenticMessageContent.ofText(text);
        content.setFormat("markdown");
        content.setSkills(skillNames(prepared));
        content.setTools(tools);
        content.setTraces(buildTraceEvents(prepared, toolEvents));
        content.setReasoning(prepared.reasoning());
        content.setDirectContextProvided(prepared.directContextProvided());
        content.setContexts(prepared.contexts());
        content.setTokens(outputTokens(prepared.inputTokens(), text));
        return content;
    }

    private List<AgenticMessageContent.Trace> buildTraceEvents(PreparedChatRequest prepared,
                                                               List<AgenticRequestContext.ToolEvent> toolEvents) {
        List<AgenticMessageContent.Trace> traces = new ArrayList<>();
        long created = Instant.now().getEpochSecond();
        String skillName = Objects.nonNull(prepared.skillDefinition()) ? prepared.skillDefinition().getName() : "general";
        String skillDescription = Objects.nonNull(prepared.skillDefinition()) ? prepared.skillDefinition().getDescription()
                : "General assistant mode";
        traces.add(AgenticMessageContent.Trace.of("skill", "Auto skill", skillDescription, skillName, created));
        if (prepared.toolCallingEnabled() && !prepared.toolNames().isEmpty()) {
            traces.add(AgenticMessageContent.Trace.of("tools", "Available tools", String.join(", ", prepared.toolNames()),
                    skillName, created));
        }
        if (prepared.directContextProvided()) {
            traces.add(AgenticMessageContent.Trace.of("tool", "Backend context loaded",
                    "Queried DC3 backend before model response", skillName, created));
        }
        if (prepared.reasoning()) {
            traces.add(AgenticMessageContent.Trace.of("reasoning", "Thinking",
                    "Reasoning mode requested for this model.", skillName, created));
        }
        for (AgenticRequestContext.ToolEvent event : toolEvents) {
            traces.add(AgenticMessageContent.Trace.of("tool", event.description(), event.domain(), event.toolName(),
                    event.timestamp() / 1000));
        }
        return traces;
    }

    private List<AgenticRequestContext.ToolEvent> drainToolEvents(PreparedChatRequest prepared) {
        AgenticRequestContext.ToolEvent event = prepared.toolEvents().poll();
        while (Objects.nonNull(event)) {
            prepared.toolTraceEvents().add(event);
            event = prepared.toolEvents().poll();
        }
        return prepared.toolTraceEvents();
    }

    private AgenticMessageContent.Tokens outputTokens(AgenticMessageContent.Tokens inputTokens, String assistantText) {
        int outputTokens = AgenticTokenEstimator.estimate(assistantText);
        AgenticMessageContent.Tokens tokens = new AgenticMessageContent.Tokens();
        tokens.setInput(inputTokens.getInput());
        tokens.setOutput(outputTokens);
        tokens.setText(inputTokens.getText());
        tokens.setContext(inputTokens.getContext());
        tokens.setSystem(inputTokens.getSystem());
        tokens.setMemory(inputTokens.getMemory());
        return tokens;
    }

    private List<String> skillNames(PreparedChatRequest prepared) {
        return StringUtils.isBlank(prepared.skill()) ? List.of() : List.of(prepared.skill());
    }

    private List<Long> normalizeAttachments(ChatCompletionRequest request) {
        if (Objects.isNull(request.getAttachments()) || request.getAttachments().isEmpty()) {
            return List.of();
        }
        return request.getAttachments().stream().filter(Objects::nonNull).distinct().toList();
    }

    private record PreparedChatRequest(String userMessage, String scopedConversationId, String skillSystemPrompt,
                                       String requestSystemContext, List<String> toolNames, String model, String skill,
                                       Map<String, Object> toolContext, Double temperature, Integer maxTokens,
                                       SkillDefinition skillDefinition,
                                       Queue<AgenticRequestContext.ToolEvent> toolEvents, boolean toolCallingEnabled,
                                       boolean reasoning, boolean directContextProvided, List<Long> attachments,
                                       List<AgenticMessageContent.Context> contexts,
                                       AgenticMessageContent.Tokens inputTokens,
                                       List<AgenticRequestContext.ToolEvent> toolTraceEvents) {
    }

    private record StreamDelta(String content, String reasoningContent) {

        static StreamDelta empty() {
            return new StreamDelta("", null);
        }

        boolean hasContent() {
            return StringUtils.isNotEmpty(content) || StringUtils.isNotEmpty(reasoningContent);
        }

    }

}
