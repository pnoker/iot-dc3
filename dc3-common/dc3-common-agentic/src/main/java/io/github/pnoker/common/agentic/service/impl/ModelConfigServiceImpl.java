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

import io.github.pnoker.common.agentic.config.AgenticProperties;
import io.github.pnoker.common.agentic.entity.bo.ModelConfigBO;
import io.github.pnoker.common.agentic.entity.vo.ModelVO;
import io.github.pnoker.common.agentic.repository.ReactiveModelConfigStore;
import io.github.pnoker.common.agentic.repository.ReactiveModelProviderStore;
import io.github.pnoker.common.agentic.service.ModelConfigService;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.DefaultFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Reactive model configuration application service. */
@Service
@RequiredArgsConstructor
public class ModelConfigServiceImpl implements ModelConfigService {

    private final ReactiveModelConfigStore modelConfigStore;
    private final ReactiveModelProviderStore modelProviderStore;
    private final AgenticProperties properties;

    @Value("${spring.ai.openai.chat.options.model:gpt-4o}")
    private String fallbackModel;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double fallbackTemperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:2048}")
    private Integer fallbackMaxTokens;

    @Override
    public Mono<List<ModelVO>> listOptions(RequestHeader.PrincipalHeader header) {
        return modelConfigStore.list(header, true).collectList().map(configs -> {
            if (configs.isEmpty()) {
                return List.of(new ModelVO(
                        fallbackModel,
                        fallbackModel,
                        true,
                        properties.isFallbackToolCallingEnabled(),
                        properties.isFallbackVisionEnabled(),
                        properties.isFallbackReasoningEnabled(),
                        fallbackTemperature,
                        fallbackMaxTokens));
            }
            return configs.stream()
                    .map(item -> new ModelVO(
                            item.getModel(),
                            item.getLabel(),
                            truthy(item.getStream()),
                            truthy(item.getToolCall()),
                            truthy(item.getVision()),
                            truthy(item.getReasoning()),
                            item.getTemperature(),
                            item.getMaxTokens()))
                    .toList();
        });
    }

    @Override
    public Mono<List<ModelConfigBO>> listConfigs(RequestHeader.PrincipalHeader header) {
        return modelConfigStore.list(header, false).collectList();
    }

    @Override
    public Mono<ModelConfigBO> add(ModelConfigBO entityBO, RequestHeader.PrincipalHeader header) {
        return Mono.defer(() -> validate(entityBO, header)
                .then(Mono.defer(() -> modelConfigStore.insert(normalize(entityBO, null, header), header))));
    }

    @Override
    public Mono<ModelConfigBO> update(ModelConfigBO entityBO, RequestHeader.PrincipalHeader header) {
        return Mono.defer(() -> {
            if (entityBO == null || entityBO.getId() == null) {
                return Mono.error(new RequestException("Model config ID is required"));
            }
            return validate(entityBO, header)
                    .then(Mono.defer(() -> modelConfigStore.get(entityBO.getId(), header)))
                    .switchIfEmpty(Mono.error(new NotFoundException("Model config does not exist")))
                    .map(existing -> normalize(entityBO, existing, header))
                    .flatMap(value -> Mono.defer(() -> modelConfigStore.update(value, header)))
                    .switchIfEmpty(Mono.error(new NotFoundException("Model config does not exist")));
        });
    }

    @Override
    public Mono<Void> delete(Long id, RequestHeader.PrincipalHeader header) {
        return modelConfigStore
                .delete(id, header)
                .flatMap(deleted -> deleted
                        ? Mono.<Void>empty()
                        : Mono.error(new NotFoundException("Model config does not exist")));
    }

    private Mono<Void> validate(ModelConfigBO entityBO, RequestHeader.PrincipalHeader header) {
        if (entityBO == null || StringUtils.isBlank(entityBO.getModel())) {
            return Mono.error(new RequestException("Model is required"));
        }
        if (entityBO.getProviderId() == null || entityBO.getProviderId() == 0) {
            return Mono.error(new RequestException("Provider is required"));
        }
        if (entityBO.getTemperature() != null && (entityBO.getTemperature() < 0.0 || entityBO.getTemperature() > 2.0)) {
            return Mono.error(new RequestException("Temperature must be between 0.0 and 2.0"));
        }
        if (entityBO.getMaxTokens() != null && entityBO.getMaxTokens() < 1) {
            return Mono.error(new RequestException("Max tokens must be greater than 0"));
        }
        return modelProviderStore
                .get(entityBO.getProviderId(), header)
                .switchIfEmpty(Mono.error(new NotFoundException("Provider does not exist")))
                .then();
    }

    private ModelConfigBO normalize(
            ModelConfigBO source, ModelConfigBO existing, RequestHeader.PrincipalHeader header) {
        ModelConfigBO value = new ModelConfigBO();
        value.setId(source.getId());
        value.setModel(source.getModel().trim());
        value.setLabel(
                StringUtils.defaultIfBlank(source.getLabel(), source.getModel()).trim());
        value.setProviderId(source.getProviderId());
        value.setStream(source.getStream() == null ? true : source.getStream());
        value.setToolCall(source.getToolCall() == null ? true : source.getToolCall());
        value.setVision(source.getVision() == null ? false : source.getVision());
        value.setReasoning(source.getReasoning() == null ? false : source.getReasoning());
        value.setTemperature(source.getTemperature() == null ? fallbackTemperature : source.getTemperature());
        value.setMaxTokens(source.getMaxTokens() == null ? fallbackMaxTokens : source.getMaxTokens());
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

    private boolean truthy(Boolean value) {
        return Boolean.TRUE.equals(value);
    }
}
