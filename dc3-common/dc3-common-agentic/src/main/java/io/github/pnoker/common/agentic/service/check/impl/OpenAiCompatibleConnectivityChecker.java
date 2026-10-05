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
package io.github.pnoker.common.agentic.service.check.impl;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.errors.OpenAIInvalidDataException;
import com.openai.errors.OpenAIServiceException;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import com.openai.models.models.Model;
import com.openai.models.models.ModelListPageAsync;
import io.github.pnoker.common.agentic.config.AgenticProperties;
import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.agentic.entity.model.ConnectivityLevelResult;
import io.github.pnoker.common.agentic.service.check.AgenticConnectivityChecker;
import io.github.pnoker.common.agentic.service.check.ConnectivityErrorClassifier;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.enums.AgenticModelProviderTypeEnum;
import io.github.pnoker.common.enums.ConnectivityErrorTypeEnum;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * OpenAI-compatible connectivity probe. One-shot OkHttp client per probe with
 * the configured timeout budget and zero retries; the API key is always set
 * explicitly (empty string when unconfigured) so the SDK never silently falls
 * back to the {@code OPENAI_API_KEY} environment variable.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class OpenAiCompatibleConnectivityChecker implements AgenticConnectivityChecker {

    private final AgenticProperties properties;

    @Override
    public boolean supports(AgenticModelProviderTypeEnum providerType) {
        return AgenticModelProviderTypeEnum.OPENAI_COMPATIBLE.equals(providerType);
    }

    @Override
    public Mono<ConnectivityLevelResult> checkL1(ModelProviderBO target) {
        Duration budget = Duration.ofSeconds(properties.getCheckL1TimeoutSeconds());
        return Mono.defer(() -> {
            if (StringUtils.isBlank(target.getBaseUrl())) {
                return Mono.just(ConnectivityLevelResult.fail(
                        0, ConnectivityErrorTypeEnum.UNREACHABLE, "Base URL is blank", null));
            }
            long startNanos = System.nanoTime();
            OpenAIClient client = createClient(target, budget);
            return Mono.fromFuture(client.async().models().list())
                    .timeout(budget)
                    .map(page -> ConnectivityLevelResult.passL1(elapsedMs(startNanos), modelIds(page, target)))
                    .onErrorResume(error -> Mono.just(failure(startNanos, target, error)))
                    .doFinally(signal -> client.close());
        });
    }

    @Override
    public Mono<ConnectivityLevelResult> checkL2(ModelProviderBO target, String model) {
        Duration budget = Duration.ofSeconds(properties.getCheckL2TimeoutSeconds());
        return Mono.defer(() -> {
            if (StringUtils.isBlank(model)) {
                return Mono.just(ConnectivityLevelResult.fail(
                        0, ConnectivityErrorTypeEnum.MODEL_NOT_FOUND, "Probe model is blank", null));
            }
            if (StringUtils.isBlank(target.getBaseUrl())) {
                return Mono.just(ConnectivityLevelResult.fail(
                        0, ConnectivityErrorTypeEnum.UNREACHABLE, "Base URL is blank", null));
            }
            long startNanos = System.nanoTime();
            OpenAIClient client = createClient(target, budget);
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(model)
                    .messages(List.of(userMessage()))
                    .maxTokens(AgenticConstant.Check.PROBE_MAX_TOKENS)
                    .build();
            return Mono.fromFuture(client.async().chat().completions().create(params))
                    .timeout(budget)
                    .map(ignored -> ConnectivityLevelResult.passL2(elapsedMs(startNanos), model))
                    .onErrorResume(error -> Mono.just(failure(startNanos, target, error)))
                    .doFinally(signal -> client.close());
        });
    }

    private OpenAIClient createClient(ModelProviderBO target, Duration budget) {
        // Explicit credential (placeholder when unconfigured): the SDK refuses
        // empty-key requests and would otherwise fall back to OPENAI_API_KEY.
        return OpenAIOkHttpClient.builder()
                .baseUrl(normalizeBaseUrl(target))
                .apiKey(effectiveApiKey(target))
                .timeout(budget)
                .maxRetries(0)
                .build();
    }

    private String effectiveApiKey(ModelProviderBO target) {
        return StringUtils.isBlank(target.getApiKey()) ? AgenticConstant.Check.NO_KEY_PLACEHOLDER : target.getApiKey();
    }

    private List<String> modelIds(ModelListPageAsync page, ModelProviderBO target) {
        int cap = properties.getCheckMaxModels();
        List<String> ids = page.items().stream()
                .map(Model::id)
                .filter(StringUtils::isNotBlank)
                .limit(cap)
                .toList();
        log.debug(
                "L1 probe listed {} model ids (cap {}) for provider type {}",
                ids.size(),
                cap,
                target.getProviderType());
        return ids;
    }

    private ConnectivityLevelResult failure(long startNanos, ModelProviderBO target, Throwable error) {
        Integer upstreamStatus = null;
        String snippet;
        if (error instanceof OpenAIServiceException serviceException) {
            upstreamStatus = serviceException.statusCode();
            snippet = bodySnippet(serviceException.body());
        } else {
            snippet = error.getMessage();
        }
        ConnectivityErrorTypeEnum type = error instanceof OpenAIInvalidDataException
                ? ConnectivityErrorTypeEnum.BAD_RESPONSE
                : ConnectivityErrorClassifier.classify(upstreamStatus, error, snippet);
        if (type == ConnectivityErrorTypeEnum.AUTH_FAILED && StringUtils.isBlank(target.getApiKey())) {
            type = ConnectivityErrorTypeEnum.NO_KEY_CONFIGURED;
        }
        return ConnectivityLevelResult.fail(
                elapsedMs(startNanos),
                type,
                ConnectivityErrorClassifier.sanitizeMessage(snippet, target.getApiKey()),
                upstreamStatus);
    }

    private String bodySnippet(JsonValue body) {
        return body == null ? null : String.valueOf(body);
    }

    private ChatCompletionMessageParam userMessage() {
        return ChatCompletionMessageParam.ofUser(ChatCompletionUserMessageParam.builder()
                .content(AgenticConstant.Check.PROBE_PROMPT)
                .role(JsonValue.from(AgenticConstant.Chat.ROLE_USER))
                .build());
    }

    private String normalizeBaseUrl(ModelProviderBO target) {
        return StringUtils.removeEnd(StringUtils.trim(target.getBaseUrl()), "/");
    }

    private long elapsedMs(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }
}
