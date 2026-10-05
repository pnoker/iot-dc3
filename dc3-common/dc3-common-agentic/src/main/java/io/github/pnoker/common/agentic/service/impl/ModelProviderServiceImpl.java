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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
            return modelProviderStore
                    .get(entityBO.getId(), header)
                    .switchIfEmpty(Mono.error(new NotFoundException("Provider does not exist")))
                    .flatMap(existing -> {
                        ModelProviderBO value = normalize(entityBO, existing, header);
                        boolean connectivityChanged = connectivityChanged(existing, value);
                        return modelProviderStore
                                .update(value, header)
                                .switchIfEmpty(Mono.error(new NotFoundException("Provider does not exist")))
                                .doOnNext(updated -> chatClientFactory.evict(updated.getId()))
                                .flatMap(updated -> connectivityChanged
                                        ? modelProviderStore
                                                .clearCheckProfile(updated.getId(), header)
                                                .thenReturn(updated)
                                        : Mono.just(updated));
                    });
        });
    }

    @Override
    public Mono<Void> delete(Long id, RequestHeader.PrincipalHeader header) {
        return modelProviderStore.delete(id, header).flatMap(deleted -> {
            if (!deleted) return Mono.error(new NotFoundException("Provider does not exist"));
            chatClientFactory.evict(id);
            return Mono.<Void>empty();
        });
    }

    /**
     * Whether the update changes fields the last connectivity profile depends
     * on: base URL, provider type or the effective API key. Such changes
     * invalidate the stored health profile.
     */
    private boolean connectivityChanged(ModelProviderBO existing, ModelProviderBO value) {
        return !Objects.equals(existing.getBaseUrl(), value.getBaseUrl())
                || !Objects.equals(existing.getProviderType(), value.getProviderType())
                || !Objects.equals(existing.getApiKey(), value.getApiKey());
    }

    private void validate(ModelProviderBO entityBO) {
        if (entityBO == null || StringUtils.isBlank(entityBO.getName())) {
            throw new RequestException("Provider name is required");
        }
        if (StringUtils.isBlank(entityBO.getBaseUrl())) {
            throw new RequestException("Provider base URL is required");
        }
    }

    private ModelProviderBO normalize(
            ModelProviderBO source, ModelProviderBO existing, RequestHeader.PrincipalHeader header) {
        ModelProviderBO value = new ModelProviderBO();
        value.setId(source.getId());
        value.setName(source.getName().trim());
        value.setProviderType(
                source.getProviderType() == null
                        ? AgenticModelProviderTypeEnum.OPENAI_COMPATIBLE
                        : source.getProviderType());
        value.setBaseUrl(source.getBaseUrl().trim());
        value.setApiKey(
                StringUtils.isBlank(source.getApiKey()) && existing != null
                        ? existing.getApiKey()
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
