/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.pnoker.common.agentic.service.impl;

import io.github.pnoker.common.agentic.config.ChatClientFactory;
import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.agentic.repository.ReactiveModelProviderStore;
import io.github.pnoker.common.agentic.service.ModelProviderService;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.AgenticModelProviderTypeEnum;
import io.github.pnoker.common.enums.DefaultFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

/** Reactive model provider application service. */
@Service
@RequiredArgsConstructor
public class ModelProviderServiceImpl implements ModelProviderService {

    private final ReactiveModelProviderStore modelProviderStore;
    private final ChatClientFactory chatClientFactory;

    @Override
    public Mono<List<ModelProviderBO>> list(RequestHeader.PrincipalHeader header) {
        return modelProviderStore.list(header).collectList();
    }

    @Override
    public Mono<ModelProviderBO> add(ModelProviderBO entityBO, RequestHeader.PrincipalHeader header) {
        return Mono.defer(() -> {
            validate(entityBO);
            ModelProviderBO value = normalize(entityBO, null, header);
            return modelProviderStore.insert(value, header);
        });
    }

    @Override
    public Mono<ModelProviderBO> update(ModelProviderBO entityBO, RequestHeader.PrincipalHeader header) {
        return Mono.defer(() -> {
            if (entityBO == null || entityBO.getId() == null) {
                return Mono.error(new RequestException("Provider ID is required"));
            }
            validate(entityBO);
            return modelProviderStore.get(entityBO.getId(), header)
                    .switchIfEmpty(Mono.error(new NotFoundException("Provider does not exist")))
                    .map(existing -> normalize(entityBO, existing, header))
                    .flatMap(value -> modelProviderStore.update(value, header))
                    .switchIfEmpty(Mono.error(new NotFoundException("Provider does not exist")))
                    .doOnNext(value -> chatClientFactory.evict(value.getId()));
        });
    }

    @Override
    public Mono<Void> delete(Long id, RequestHeader.PrincipalHeader header) {
        return modelProviderStore.delete(id, header)
                .flatMap(deleted -> {
                    if (!deleted) return Mono.error(new NotFoundException("Provider does not exist"));
                    chatClientFactory.evict(id);
                    return Mono.<Void>empty();
                });
    }

    private void validate(ModelProviderBO entityBO) {
        if (entityBO == null || StringUtils.isBlank(entityBO.getName())) {
            throw new RequestException("Provider name is required");
        }
        if (StringUtils.isBlank(entityBO.getBaseUrl())) {
            throw new RequestException("Provider base URL is required");
        }
    }

    private ModelProviderBO normalize(ModelProviderBO source, ModelProviderBO existing,
                                      RequestHeader.PrincipalHeader header) {
        ModelProviderBO value = new ModelProviderBO();
        value.setId(source.getId());
        value.setName(source.getName().trim());
        value.setProviderType(source.getProviderType() == null ? AgenticModelProviderTypeEnum.OPENAI_COMPATIBLE
                : source.getProviderType());
        value.setBaseUrl(source.getBaseUrl().trim());
        value.setApiKey(StringUtils.isBlank(source.getApiKey()) && existing != null ? existing.getApiKey()
                : StringUtils.defaultString(source.getApiKey()));
        value.setDefaultFlag(source.getDefaultFlag() == null ? DefaultFlagEnum.NOT_DEFAULT : source.getDefaultFlag());
        value.setEnableFlag(source.getEnableFlag() == null ? EnableFlagEnum.ENABLE : source.getEnableFlag());
        value.setRemark(StringUtils.defaultString(source.getRemark()));
        value.setTenantId(header.getTenantId());
        value.setCreatorId(existing == null ? header.getUserId() : existing.getCreatorId());
        value.setCreatorName(existing == null ? header.getUserName() : existing.getCreatorName());
        value.setCreateTime(existing == null ? LocalDateTime.now(ZoneOffset.UTC) : existing.getCreateTime());
        value.setOperatorId(header.getUserId());
        value.setOperatorName(header.getUserName());
        value.setOperateTime(LocalDateTime.now(ZoneOffset.UTC));
        return value;
    }
}
