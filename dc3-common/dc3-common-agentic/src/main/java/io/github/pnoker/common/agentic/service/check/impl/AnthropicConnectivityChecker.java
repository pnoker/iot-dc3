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

import com.anthropic.client.AnthropicClientAsync;
import com.anthropic.client.okhttp.AnthropicOkHttpClientAsync;
import com.anthropic.errors.AnthropicInvalidDataException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.models.ModelInfo;
import com.anthropic.models.models.ModelListPageAsync;
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
 * Anthropic connectivity probe. Mirrors the OpenAI-compatible probe with the
 * anthropic-java SDK: one-shot async OkHttp client per probe, probe timeout
 * budget, zero retries, explicit (possibly empty) API key so the SDK never
 * falls back to the {@code ANTHROPIC_API_KEY} environment variable.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class AnthropicConnectivityChecker implements AgenticConnectivityChecker {

    private final AgenticProperties properties;

    @Override
    public boolean supports(AgenticModelProviderTypeEnum providerType) {
        return AgenticModelProviderTypeEnum.ANTHROPIC.equals(providerType);
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
            AnthropicClientAsync client = createClient(target, budget);
            return Mono.fromFuture(client.models().list())
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
            AnthropicClientAsync client = createClient(target, budget);
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(AgenticConstant.Check.PROBE_MAX_TOKENS)
                    .messages(List.of(userMessage()))
                    .build();
            return Mono.fromFuture(client.messages().create(params))
                    .timeout(budget)
                    .map(ignored -> ConnectivityLevelResult.passL2(elapsedMs(startNanos), model))
                    .onErrorResume(error -> Mono.just(failure(startNanos, target, error)))
                    .doFinally(signal -> client.close());
        });
    }

    private AnthropicClientAsync createClient(ModelProviderBO target, Duration budget) {
        // Explicit credential (placeholder when unconfigured): never fall back
        // to the ANTHROPIC_API_KEY env var.
        return AnthropicOkHttpClientAsync.builder()
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
        List<String> ids = page.data().stream()
                .map(ModelInfo::id)
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
        if (error instanceof AnthropicServiceException serviceException) {
            upstreamStatus = serviceException.statusCode();
            snippet = serviceException.body() == null ? null : String.valueOf(serviceException.body());
        } else {
            snippet = error.getMessage();
        }
        ConnectivityErrorTypeEnum type = error instanceof AnthropicInvalidDataException
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

    private MessageParam userMessage() {
        return MessageParam.builder()
                .content(MessageParam.Content.ofString(AgenticConstant.Check.PROBE_PROMPT))
                .role(MessageParam.Role.USER)
                .build();
    }

    private String normalizeBaseUrl(ModelProviderBO target) {
        return StringUtils.removeEnd(StringUtils.trim(target.getBaseUrl()), "/");
    }

    private long elapsedMs(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }
}
