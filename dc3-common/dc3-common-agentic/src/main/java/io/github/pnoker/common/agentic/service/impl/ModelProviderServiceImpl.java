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

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.pnoker.common.agentic.config.ChatClientFactory;
import io.github.pnoker.common.agentic.dal.ModelProviderManager;
import io.github.pnoker.common.agentic.entity.model.ModelProviderDO;
import io.github.pnoker.common.agentic.entity.request.ModelProviderRequest;
import io.github.pnoker.common.agentic.entity.vo.ModelProviderVO;
import io.github.pnoker.common.agentic.service.ModelProviderService;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class ModelProviderServiceImpl implements ModelProviderService {

    private static final byte ENABLED = 0;

    private final ModelProviderManager modelProviderManager;
    private final ChatClientFactory chatClientFactory;

    public ModelProviderServiceImpl(ModelProviderManager modelProviderManager,
                                    ChatClientFactory chatClientFactory) {
        this.modelProviderManager = modelProviderManager;
        this.chatClientFactory = chatClientFactory;
    }

    @Override
    public List<ModelProviderVO> list() {
        return modelProviderManager.list(Wrappers.<ModelProviderDO>query()
                        .lambda()
                        .orderByDesc(ModelProviderDO::getDefaultFlag)
                        .orderByAsc(ModelProviderDO::getName))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public ModelProviderVO save(ModelProviderRequest request, RequestHeader.UserHeader header) {
        validate(request);
        ModelProviderDO entity = new ModelProviderDO();
        apply(entity, request);
        entity.setCreateTime(LocalDateTime.now());
        entity.setOperateTime(entity.getCreateTime());
        entity.setCreatorId(header.getUserId());
        entity.setCreatorName(header.getUserName());
        entity.setOperatorId(header.getUserId());
        entity.setOperatorName(header.getUserName());
        modelProviderManager.save(entity);
        normalizeDefault(entity);
        return toVO(entity);
    }

    @Override
    public ModelProviderVO update(ModelProviderRequest request, RequestHeader.UserHeader header) {
        if (Objects.isNull(request) || Objects.isNull(request.getId())) {
            throw new RequestException("Provider ID is required");
        }
        validate(request);
        ModelProviderDO entity = modelProviderManager.getById(request.getId());
        if (Objects.isNull(entity)) {
            throw new NotFoundException("Provider does not exist");
        }
        apply(entity, request);
        entity.setOperateTime(LocalDateTime.now());
        entity.setOperatorId(header.getUserId());
        entity.setOperatorName(header.getUserName());
        modelProviderManager.updateById(entity);
        normalizeDefault(entity);
        chatClientFactory.evict(entity.getId());
        return toVO(entity);
    }

    @Override
    public void remove(Long id) {
        modelProviderManager.removeById(id);
        chatClientFactory.evict(id);
    }

    private void validate(ModelProviderRequest request) {
        if (Objects.isNull(request) || StringUtils.isBlank(request.getName())) {
            throw new RequestException("Provider name is required");
        }
        if (StringUtils.isBlank(request.getBaseUrl())) {
            throw new RequestException("Provider base URL is required");
        }
    }

    private void apply(ModelProviderDO entity, ModelProviderRequest request) {
        entity.setName(request.getName().trim());
        entity.setProviderType(StringUtils.defaultIfBlank(request.getProviderType(), "openai-compatible").trim());
        entity.setBaseUrl(request.getBaseUrl().trim());
        if (StringUtils.isNotBlank(request.getApiKey())) {
            entity.setApiKey(request.getApiKey().trim());
        }
        entity.setDefaultFlag(Objects.nonNull(request.getDefaultFlag()) ? request.getDefaultFlag() : (byte) 0);
        entity.setEnableFlag(Objects.nonNull(request.getEnableFlag()) ? request.getEnableFlag() : ENABLED);
        entity.setRemark(StringUtils.defaultString(request.getRemark()));
    }

    private void normalizeDefault(ModelProviderDO entity) {
        if (!Objects.equals(entity.getDefaultFlag(), (byte) 1)) {
            return;
        }
        modelProviderManager.list(Wrappers.<ModelProviderDO>query()
                        .lambda()
                        .eq(ModelProviderDO::getDefaultFlag, (byte) 1))
                .stream()
                .filter(item -> !Objects.equals(item.getId(), entity.getId()))
                .forEach(item -> {
                    item.setDefaultFlag((byte) 0);
                    modelProviderManager.updateById(item);
                });
    }

    private ModelProviderVO toVO(ModelProviderDO entity) {
        ModelProviderVO vo = new ModelProviderVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setProviderType(entity.getProviderType());
        vo.setBaseUrl(entity.getBaseUrl());
        vo.setDefaultFlag(entity.getDefaultFlag());
        vo.setEnableFlag(entity.getEnableFlag());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setOperateTime(entity.getOperateTime());
        return vo;
    }

}
