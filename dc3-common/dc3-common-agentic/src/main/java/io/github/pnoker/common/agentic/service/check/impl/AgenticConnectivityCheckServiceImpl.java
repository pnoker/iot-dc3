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

import io.github.pnoker.common.agentic.config.AgenticProperties;
import io.github.pnoker.common.agentic.entity.bo.ModelConfigBO;
import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.agentic.entity.model.ConnectivityCheckProfile;
import io.github.pnoker.common.agentic.entity.model.ConnectivityCheckResult;
import io.github.pnoker.common.agentic.entity.model.ConnectivityLevelResult;
import io.github.pnoker.common.agentic.entity.vo.ProviderCheckRequestVO;
import io.github.pnoker.common.agentic.repository.ReactiveModelConfigStore;
import io.github.pnoker.common.agentic.repository.ReactiveModelProviderStore;
import io.github.pnoker.common.agentic.service.check.AgenticConnectivityCheckService;
import io.github.pnoker.common.agentic.service.check.AgenticConnectivityChecker;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.ConnectivityCheckLevelEnum;
import io.github.pnoker.common.enums.ConnectivityErrorTypeEnum;
import io.github.pnoker.common.enums.ConnectivityStatusEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Connectivity check orchestration. Single-instance semantics: the in-flight
 * merge and the TTL result cache are process-local maps, so a multi-replica
 * deployment degrades to each replica probing once (last write wins on the
 * health profile) instead of sharing probes.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class AgenticConnectivityCheckServiceImpl implements AgenticConnectivityCheckService {

    private static final ConnectivityLevelResult SKIPPED_L1 =
            new ConnectivityLevelResult(ConnectivityStatusEnum.SKIPPED, 0, null, null, null, null, null);
    private static final ConnectivityLevelResult SKIPPED_L2 =
            new ConnectivityLevelResult(ConnectivityStatusEnum.SKIPPED, 0, null, null, null, null, null);

    private final ReactiveModelProviderStore modelProviderStore;
    private final ReactiveModelConfigStore modelConfigStore;
    private final AgenticProperties properties;
    private final List<AgenticConnectivityChecker> checkers;

    private final ConcurrentHashMap<String, Mono<ConnectivityCheckResult>> inFlightChecks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CachedCheck> cachedChecks = new ConcurrentHashMap<>();

    @Override
    public Mono<ConnectivityCheckResult> checkProvider(
            ProviderCheckRequestVO request, RequestHeader.PrincipalHeader header) {
        if (request == null) return Mono.error(new RequestException("Check request is required"));
        return Mono.defer(() -> {
            ConnectivityCheckLevelEnum level =
                    request.getLevel() == null ? ConnectivityCheckLevelEnum.BOTH : request.getLevel();
            boolean includeL1 = level == ConnectivityCheckLevelEnum.L1 || level == ConnectivityCheckLevelEnum.BOTH;
            boolean includeL2 = level == ConnectivityCheckLevelEnum.L2 || level == ConnectivityCheckLevelEnum.BOTH;
            return resolveTarget(request, header).flatMap(target -> {
                Long providerId = parseProviderId(request);
                // The cache key carries the level: an L1-only call (model-select
                // dropdown) must never serve a cached BOTH result or vice versa.
                String key = providerId != null
                        ? header.getTenantId() + ":provider:" + providerId + ":" + level.getCode()
                        : header.getTenantId() + ":draft:" + draftFingerprint(target) + ":" + level.getCode();
                return coordinated(
                        key,
                        () -> runProviderCheck(target, providerId, request.getModel(), includeL1, includeL2, header));
            });
        });
    }

    @Override
    public Mono<ConnectivityCheckResult> checkModelConfig(Long id, RequestHeader.PrincipalHeader header) {
        if (id == null) return Mono.error(new RequestException("Model config ID is required"));
        String key = header.getTenantId() + ":config:" + id;
        return coordinated(
                key,
                () -> modelConfigStore
                        .get(id, header)
                        .switchIfEmpty(Mono.error(new NotFoundException("Model config does not exist")))
                        .flatMap(config -> modelProviderStore
                                .get(config.getProviderId(), header)
                                .switchIfEmpty(Mono.error(new NotFoundException("Provider does not exist")))
                                .flatMap(provider -> runModelCheck(id, provider, config.getModel(), header))));
    }

    private Mono<ConnectivityCheckResult> runProviderCheck(
            ModelProviderBO target,
            Long providerId,
            String requestedModel,
            boolean includeL1,
            boolean includeL2,
            RequestHeader.PrincipalHeader header) {
        Mono<ConnectivityLevelResult> l1Stage = includeL1 ? executeL1(target) : Mono.just(SKIPPED_L1);
        return l1Stage.flatMap(l1 -> {
                    if (!includeL2) {
                        return Mono.just(assemble(l1, null, includeL1, false));
                    }
                    if (infrastructureFailure(l1)) {
                        return Mono.just(assemble(l1, SKIPPED_L2, includeL1, true));
                    }
                    return resolveProbeModel(requestedModel, providerId, l1, header)
                            .defaultIfEmpty("")
                            .flatMap(model -> {
                                if (StringUtils.isBlank(model)) {
                                    ConnectivityLevelResult noModel = ConnectivityLevelResult.fail(
                                            0,
                                            ConnectivityErrorTypeEnum.MODEL_NOT_FOUND,
                                            "No probe model available",
                                            null);
                                    return Mono.just(assemble(l1, noModel, includeL1, true));
                                }
                                return executeL2(target, model).map(l2 -> assemble(l1, l2, includeL1, true));
                            });
                })
                .flatMap(result -> persistProviderProfile(providerId, includeL2, result, header)
                        .thenReturn(result));
    }

    private Mono<ConnectivityCheckResult> runModelCheck(
            Long configId, ModelProviderBO provider, String model, RequestHeader.PrincipalHeader header) {
        return executeL2(provider, model)
                .map(l2 -> assemble(SKIPPED_L1, l2, false, true))
                .flatMap(
                        result -> persistConfigProfile(configId, result, header).thenReturn(result));
    }

    private Mono<ModelProviderBO> resolveTarget(ProviderCheckRequestVO request, RequestHeader.PrincipalHeader header) {
        Long providerId = parseProviderId(request);
        if (providerId == null) {
            if (request.getProviderType() == null) {
                return Mono.error(new RequestException("Provider type is required for draft checks"));
            }
            if (StringUtils.isBlank(request.getBaseUrl())) {
                return Mono.error(new RequestException("Base URL is required for draft checks"));
            }
            ModelProviderBO draft = new ModelProviderBO();
            draft.setProviderType(request.getProviderType());
            draft.setBaseUrl(request.getBaseUrl());
            draft.setApiKey(StringUtils.defaultString(request.getApiKey()));
            return Mono.just(draft);
        }
        return modelProviderStore
                .get(providerId, header)
                .switchIfEmpty(Mono.error(new NotFoundException("Provider does not exist")))
                .map(existing -> {
                    ModelProviderBO effective = new ModelProviderBO();
                    effective.setId(existing.getId());
                    effective.setProviderType(
                            request.getProviderType() != null ? request.getProviderType() : existing.getProviderType());
                    effective.setBaseUrl(
                            StringUtils.isNotBlank(request.getBaseUrl())
                                    ? request.getBaseUrl()
                                    : existing.getBaseUrl());
                    // Absent key falls back to the stored one; an explicit empty string is a deliberate no-key probe.
                    effective.setApiKey(request.getApiKey() == null ? existing.getApiKey() : request.getApiKey());
                    return effective;
                });
    }

    private Mono<ConnectivityLevelResult> executeL1(ModelProviderBO target) {
        return checkerFor(target).checkL1(target).onErrorResume(error -> Mono.just(unexpectedFailure(target, error)));
    }

    private Mono<ConnectivityLevelResult> executeL2(ModelProviderBO target, String model) {
        return checkerFor(target)
                .checkL2(target, model)
                .onErrorResume(error -> Mono.just(unexpectedFailure(target, error)));
    }

    private Mono<String> resolveProbeModel(
            String requested, Long providerId, ConnectivityLevelResult l1, RequestHeader.PrincipalHeader header) {
        if (StringUtils.isNotBlank(requested)) return Mono.just(requested);
        if (providerId == null) return firstListedModel(l1);
        return modelConfigStore
                .list(header, true)
                .filter(config -> providerId.equals(config.getProviderId()))
                .next()
                .map(ModelConfigBO::getModel)
                .switchIfEmpty(firstListedModel(l1));
    }

    private Mono<String> firstListedModel(ConnectivityLevelResult l1) {
        if (l1 == null || l1.models() == null || l1.models().isEmpty()) return Mono.empty();
        return Mono.just(l1.models().get(0));
    }

    private AgenticConnectivityChecker checkerFor(ModelProviderBO target) {
        return checkers.stream()
                .filter(checker -> checker.supports(target.getProviderType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No connectivity checker for provider type " + target.getProviderType()));
    }

    private ConnectivityCheckResult assemble(
            ConnectivityLevelResult l1, ConnectivityLevelResult l2, boolean includeL1, boolean includeL2) {
        ConnectivityStatusEnum overall = overall(l1, l2, includeL1, includeL2);
        return new ConnectivityCheckResult(
                overall,
                includeL1 ? l1 : SKIPPED_L1,
                includeL2 ? l2 : SKIPPED_L2,
                dimensions(l1, l2, includeL1, includeL2),
                nowUtc());
    }

    private ConnectivityStatusEnum overall(
            ConnectivityLevelResult l1, ConnectivityLevelResult l2, boolean includeL1, boolean includeL2) {
        boolean l1Passed = !includeL1 || l1 == null || l1.status() == ConnectivityStatusEnum.PASS;
        boolean l2Passed = !includeL2 || l2 == null || l2.status() == ConnectivityStatusEnum.PASS;
        return l1Passed && l2Passed ? ConnectivityStatusEnum.PASS : ConnectivityStatusEnum.FAIL;
    }

    private List<String> dimensions(
            ConnectivityLevelResult l1, ConnectivityLevelResult l2, boolean includeL1, boolean includeL2) {
        Set<String> dims = new LinkedHashSet<>();
        if (includeL1) {
            dims.add(AgenticConstant.Check.DIMENSION_CONNECTIVITY);
            if (l1 != null && l1.status() == ConnectivityStatusEnum.PASS) {
                dims.add(AgenticConstant.Check.DIMENSION_AUTH);
                dims.add(AgenticConstant.Check.DIMENSION_MODEL_VISIBLE);
            }
        }
        if (includeL2) {
            dims.add(AgenticConstant.Check.DIMENSION_INFERENCE);
            if (l2 != null && l2.status() == ConnectivityStatusEnum.PASS) {
                dims.add(AgenticConstant.Check.DIMENSION_AUTH);
                dims.add(AgenticConstant.Check.DIMENSION_MODEL_VISIBLE);
            }
        }
        return List.copyOf(dims);
    }

    private boolean infrastructureFailure(ConnectivityLevelResult l1) {
        return l1 != null
                && l1.status() == ConnectivityStatusEnum.FAIL
                && (l1.errorType() == ConnectivityErrorTypeEnum.UNREACHABLE
                        || l1.errorType() == ConnectivityErrorTypeEnum.TLS_ERROR
                        || l1.errorType() == ConnectivityErrorTypeEnum.TIMEOUT);
    }

    private ConnectivityLevelResult unexpectedFailure(ModelProviderBO target, Throwable error) {
        log.warn(
                "Unexpected connectivity probe failure for provider type {}: {}",
                target.getProviderType(),
                error.getMessage(),
                error);
        return ConnectivityLevelResult.fail(
                0, ConnectivityErrorTypeEnum.UPSTREAM_ERROR, StringUtils.defaultString(error.getMessage()), null);
    }

    private Mono<Void> persistProviderProfile(
            Long providerId, boolean includeL2, ConnectivityCheckResult result, RequestHeader.PrincipalHeader header) {
        if (providerId == null || !includeL2) return Mono.empty();
        return modelProviderStore
                .updateCheckProfile(providerId, profileOf(result, true), header)
                .onErrorResume(error -> {
                    log.warn("Failed to persist provider {} health profile: {}", providerId, error.getMessage(), error);
                    return Mono.empty();
                })
                .then();
    }

    private Mono<Void> persistConfigProfile(
            Long configId, ConnectivityCheckResult result, RequestHeader.PrincipalHeader header) {
        return modelConfigStore
                .updateCheckProfile(configId, profileOf(result, false), header)
                .onErrorResume(error -> {
                    log.warn(
                            "Failed to persist model config {} health profile: {}",
                            configId,
                            error.getMessage(),
                            error);
                    return Mono.empty();
                })
                .then();
    }

    private ConnectivityCheckProfile profileOf(ConnectivityCheckResult result, boolean includeModel) {
        ConnectivityLevelResult l1 = result.l1();
        ConnectivityLevelResult l2 = result.l2();
        ConnectivityLevelResult latencySource = l2 != null && l2.status() != ConnectivityStatusEnum.SKIPPED
                ? l2
                : (l1 != null && l1.status() != ConnectivityStatusEnum.SKIPPED ? l1 : null);
        ConnectivityLevelResult failureSource = firstFailure(l1, l2);
        return new ConnectivityCheckProfile(
                result.overall(),
                result.checkedAt(),
                latencySource == null ? null : (int) latencySource.latencyMs(),
                failureSource == null || failureSource.errorType() == null
                        ? null
                        : failureSource.errorType().name(),
                failureSource == null ? null : failureSource.message(),
                includeModel && l2 != null ? l2.model() : null);
    }

    private ConnectivityLevelResult firstFailure(ConnectivityLevelResult l1, ConnectivityLevelResult l2) {
        if (l1 != null && l1.status() == ConnectivityStatusEnum.FAIL) return l1;
        if (l2 != null && l2.status() == ConnectivityStatusEnum.FAIL) return l2;
        return null;
    }

    private Mono<ConnectivityCheckResult> coordinated(String key, Supplier<Mono<ConnectivityCheckResult>> run) {
        CachedCheck cached = cachedChecks.get(key);
        if (cached != null && cached.fresh(ttlMillis())) {
            return Mono.just(cached.value());
        }
        return inFlightChecks.computeIfAbsent(
                key,
                k -> run.get()
                        .doOnNext(value -> cachedChecks.put(k, new CachedCheck(value)))
                        .doFinally(signal -> inFlightChecks.remove(k))
                        .cache());
    }

    private long ttlMillis() {
        return properties.getCheckCacheTtlSeconds() * 1000L;
    }

    private Long parseProviderId(ProviderCheckRequestVO request) {
        if (StringUtils.isBlank(request.getId())) return null;
        try {
            return Long.valueOf(request.getId().trim());
        } catch (NumberFormatException exception) {
            throw new RequestException("Provider id must be numeric");
        }
    }

    private String draftFingerprint(ModelProviderBO target) {
        String material = target.getProviderType() + "|" + StringUtils.defaultString(target.getBaseUrl()) + "|"
                + StringUtils.defaultString(target.getApiKey());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(material.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest unavailable", exception);
        }
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /**
     * TTL-stamped check result. Entries are replaced on new checks; there is
     * no background eviction, so the map is bounded by the number of distinct
     * probe targets per process lifetime.
     */
    private record CachedCheck(ConnectivityCheckResult value, long cachedAtMillis) {

        private CachedCheck(ConnectivityCheckResult value) {
            this(value, System.currentTimeMillis());
        }

        private boolean fresh(long ttlMillis) {
            return System.currentTimeMillis() - cachedAtMillis < ttlMillis;
        }
    }
}
