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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.pnoker.common.agentic.dal.ModelConfigManager;
import io.github.pnoker.common.agentic.entity.model.ModelConfigDO;
import io.github.pnoker.common.agentic.entity.request.ModelConfigRequest;
import io.github.pnoker.common.agentic.entity.vo.ModelConfigVO;
import io.github.pnoker.common.agentic.entity.vo.ModelVO;
import io.github.pnoker.common.agentic.service.ModelConfigService;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class ModelConfigServiceImpl implements ModelConfigService {

    private static final byte ENABLED = 0;
    private final ModelConfigManager modelConfigManager;
    @Value("${spring.ai.openai.chat.options.model:gpt-4o}")
    private String fallbackModel;
    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String fallbackBaseUrl;
    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double fallbackTemperature;
    @Value("${spring.ai.openai.chat.options.max-tokens:2048}")
    private Integer fallbackMaxTokens;

    public ModelConfigServiceImpl(ModelConfigManager modelConfigManager) {
        this.modelConfigManager = modelConfigManager;
    }

    @Override
    public List<ModelVO> listOptions() {
        List<ModelConfigDO> configs = enabledConfigs();
        if (configs.isEmpty()) {
            return List.of(new ModelVO(fallbackModel, fallbackModel, true, true, true, false, fallbackTemperature,
                    fallbackMaxTokens));
        }
        return configs.stream().map(item -> new ModelVO(item.getModel(), item.getLabel(), truthy(item.getStream()),
                truthy(item.getToolCall()), truthy(item.getVision()), truthy(item.getReasoning()),
                item.getTemperature(), item.getMaxTokens())).toList();
    }

    @Override
    public List<ModelConfigVO> listConfigs() {
        List<ModelConfigDO> configs = modelConfigManager.list(Wrappers.<ModelConfigDO>query()
                .lambda()
                .orderByDesc(ModelConfigDO::getDefaultFlag)
                .orderByAsc(ModelConfigDO::getModel));
        if (configs.isEmpty()) {
            ModelConfigDO fallback = new ModelConfigDO();
            fallback.setModel(fallbackModel);
            fallback.setLabel(fallbackModel);
            fallback.setProvider("openai-compatible");
            fallback.setBaseUrl(fallbackBaseUrl);
            fallback.setStream(true);
            fallback.setToolCall(true);
            fallback.setVision(true);
            fallback.setReasoning(false);
            fallback.setTemperature(fallbackTemperature);
            fallback.setMaxTokens(fallbackMaxTokens);
            fallback.setDefaultFlag((byte) 1);
            fallback.setEnableFlag(ENABLED);
            return List.of(toVO(fallback));
        }
        return configs.stream().map(this::toVO).toList();
    }

    @Override
    public ModelConfigVO save(ModelConfigRequest request) {
        validate(request);
        ModelConfigDO entity = new ModelConfigDO();
        apply(entity, request);
        entity.setCreateTime(LocalDateTime.now());
        entity.setOperateTime(entity.getCreateTime());
        modelConfigManager.save(entity);
        normalizeDefault(entity);
        return toVO(entity);
    }

    @Override
    public ModelConfigVO update(ModelConfigRequest request) {
        if (Objects.isNull(request) || Objects.isNull(request.getId())) {
            throw new RequestException("Model config ID is required");
        }
        validate(request);
        ModelConfigDO entity = modelConfigManager.getById(request.getId());
        if (Objects.isNull(entity)) {
            throw new NotFoundException("Model config does not exist");
        }
        apply(entity, request);
        entity.setOperateTime(LocalDateTime.now());
        modelConfigManager.updateById(entity);
        normalizeDefault(entity);
        return toVO(entity);
    }

    @Override
    public void remove(Long id) {
        modelConfigManager.removeById(id);
    }

    private List<ModelConfigDO> enabledConfigs() {
        LambdaQueryWrapper<ModelConfigDO> wrapper = Wrappers.<ModelConfigDO>query()
                .lambda()
                .eq(ModelConfigDO::getEnableFlag, ENABLED)
                .orderByDesc(ModelConfigDO::getDefaultFlag)
                .orderByAsc(ModelConfigDO::getModel);
        return modelConfigManager.list(wrapper);
    }

    private void validate(ModelConfigRequest request) {
        if (Objects.isNull(request) || StringUtils.isBlank(request.getModel())) {
            throw new RequestException("Model is required");
        }
        if (Objects.nonNull(request.getTemperature())
                && (request.getTemperature() < 0.0 || request.getTemperature() > 2.0)) {
            throw new RequestException("Temperature must be between 0.0 and 2.0");
        }
        if (Objects.nonNull(request.getMaxTokens()) && request.getMaxTokens() < 1) {
            throw new RequestException("Max tokens must be greater than 0");
        }
    }

    private void apply(ModelConfigDO entity, ModelConfigRequest request) {
        entity.setModel(request.getModel().trim());
        entity.setLabel(StringUtils.defaultIfBlank(request.getLabel(), request.getModel()).trim());
        entity.setProvider(StringUtils.defaultIfBlank(request.getProvider(), "openai-compatible").trim());
        entity.setBaseUrl(StringUtils.defaultIfBlank(request.getBaseUrl(), fallbackBaseUrl).trim());
        entity.setStream(defaultBool(request.getStream(), true));
        entity.setToolCall(defaultBool(request.getToolCall(), true));
        entity.setVision(defaultBool(request.getVision(), false));
        entity.setReasoning(defaultBool(request.getReasoning(), false));
        entity.setTemperature(Objects.nonNull(request.getTemperature()) ? request.getTemperature() : fallbackTemperature);
        entity.setMaxTokens(Objects.nonNull(request.getMaxTokens()) ? request.getMaxTokens() : fallbackMaxTokens);
        entity.setDefaultFlag(Objects.nonNull(request.getDefaultFlag()) ? request.getDefaultFlag() : (byte) 0);
        entity.setEnableFlag(Objects.nonNull(request.getEnableFlag()) ? request.getEnableFlag() : ENABLED);
        entity.setRemark(StringUtils.defaultString(request.getRemark()));
    }

    private void normalizeDefault(ModelConfigDO entity) {
        if (!Objects.equals(entity.getDefaultFlag(), (byte) 1)) {
            return;
        }
        enabledConfigs().stream()
                .filter(item -> !Objects.equals(item.getId(), entity.getId()))
                .forEach(item -> {
                    item.setDefaultFlag((byte) 0);
                    modelConfigManager.updateById(item);
                });
    }

    private Boolean defaultBool(Boolean value, boolean defaultValue) {
        return Objects.nonNull(value) ? value : defaultValue;
    }

    private boolean truthy(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private ModelConfigVO toVO(ModelConfigDO entity) {
        ModelConfigVO vo = new ModelConfigVO();
        vo.setId(entity.getId());
        vo.setModel(entity.getModel());
        vo.setLabel(entity.getLabel());
        vo.setProvider(entity.getProvider());
        vo.setBaseUrl(entity.getBaseUrl());
        vo.setStream(entity.getStream());
        vo.setToolCall(entity.getToolCall());
        vo.setVision(entity.getVision());
        vo.setReasoning(entity.getReasoning());
        vo.setTemperature(entity.getTemperature());
        vo.setMaxTokens(entity.getMaxTokens());
        vo.setDefaultFlag(entity.getDefaultFlag());
        vo.setEnableFlag(entity.getEnableFlag());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setOperateTime(entity.getOperateTime());
        return vo;
    }

}
