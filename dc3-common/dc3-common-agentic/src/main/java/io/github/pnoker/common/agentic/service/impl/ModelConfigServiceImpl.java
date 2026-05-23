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
import io.github.pnoker.common.agentic.config.AgenticProperties;
import io.github.pnoker.common.agentic.dal.ModelConfigManager;
import io.github.pnoker.common.agentic.dal.ModelProviderManager;
import io.github.pnoker.common.agentic.entity.bo.ModelConfigBO;
import io.github.pnoker.common.agentic.entity.builder.ModelConfigBuilder;
import io.github.pnoker.common.agentic.entity.model.ModelConfigDO;
import io.github.pnoker.common.agentic.entity.model.ModelProviderDO;
import io.github.pnoker.common.agentic.entity.vo.ModelVO;
import io.github.pnoker.common.agentic.service.ModelConfigService;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.DefaultFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Implements model configuration listing, save, update, and remove operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Service
@RequiredArgsConstructor
public class ModelConfigServiceImpl implements ModelConfigService {

    private final ModelConfigManager modelConfigManager;
    private final ModelProviderManager modelProviderManager;
    private final ModelConfigBuilder modelConfigBuilder;
    private final AgenticProperties properties;

    @Value("${spring.ai.openai.chat.options.model:gpt-4o}")
    private String fallbackModel;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double fallbackTemperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:2048}")
    private Integer fallbackMaxTokens;

    @Override
    public List<ModelVO> listOptions() {
        List<ModelConfigBO> configs = enabledConfigs();
        if (configs.isEmpty()) {
            return List.of(new ModelVO(fallbackModel, fallbackModel, true,
                    properties.isFallbackToolCallingEnabled(), properties.isFallbackVisionEnabled(),
                    properties.isFallbackReasoningEnabled(), fallbackTemperature, fallbackMaxTokens));
        }
        return configs.stream().map(item -> new ModelVO(item.getModel(), item.getLabel(), truthy(item.getStream()),
                truthy(item.getToolCall()), truthy(item.getVision()), truthy(item.getReasoning()),
                item.getTemperature(), item.getMaxTokens())).toList();
    }

    @Override
    public List<ModelConfigBO> listConfigs() {
        List<ModelConfigDO> entityDOList = modelConfigManager.list(Wrappers.<ModelConfigDO>query()
                .lambda()
                .orderByDesc(ModelConfigDO::getDefaultFlag)
                .orderByAsc(ModelConfigDO::getModel));
        return modelConfigBuilder.buildBOListByDOList(entityDOList).stream()
                .map(this::fillProviderName)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelConfigBO save(ModelConfigBO entityBO, RequestHeader.UserHeader header) {
        validate(entityBO);
        ModelConfigBO targetBO = new ModelConfigBO();
        apply(targetBO, entityBO);
        fillCreateAudit(targetBO, header);
        ModelConfigDO entityDO = modelConfigBuilder.buildDOByBO(targetBO);
        modelConfigManager.save(entityDO);
        normalizeDefault(entityDO);
        return fillProviderName(modelConfigBuilder.buildBOByDO(entityDO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelConfigBO update(ModelConfigBO entityBO, RequestHeader.UserHeader header) {
        if (Objects.isNull(entityBO) || Objects.isNull(entityBO.getId())) {
            throw new RequestException("Model config ID is required");
        }
        validate(entityBO);
        ModelConfigDO existingDO = modelConfigManager.getById(entityBO.getId());
        if (Objects.isNull(existingDO)) {
            throw new NotFoundException("Model config does not exist");
        }
        ModelConfigBO targetBO = modelConfigBuilder.buildBOByDO(existingDO);
        apply(targetBO, entityBO);
        fillOperateAudit(targetBO, header);
        ModelConfigDO entityDO = modelConfigBuilder.buildDOByBO(targetBO);
        modelConfigManager.updateById(entityDO);
        normalizeDefault(entityDO);
        return fillProviderName(modelConfigBuilder.buildBOByDO(entityDO));
    }

    @Override
    public void remove(Long id) {
        modelConfigManager.removeById(id);
    }

    private List<ModelConfigBO> enabledConfigs() {
        LambdaQueryWrapper<ModelConfigDO> wrapper = Wrappers.<ModelConfigDO>query()
                .lambda()
                .eq(ModelConfigDO::getEnableFlag, EnableFlagEnum.ENABLE)
                .orderByDesc(ModelConfigDO::getDefaultFlag)
                .orderByAsc(ModelConfigDO::getModel);
        return modelConfigBuilder.buildBOListByDOList(modelConfigManager.list(wrapper));
    }

    private void validate(ModelConfigBO entityBO) {
        if (Objects.isNull(entityBO) || StringUtils.isBlank(entityBO.getModel())) {
            throw new RequestException("Model is required");
        }
        if (Objects.isNull(entityBO.getProviderId()) || entityBO.getProviderId() == 0) {
            throw new RequestException("Provider is required");
        }
        ModelProviderDO provider = modelProviderManager.getById(entityBO.getProviderId());
        if (Objects.isNull(provider)) {
            throw new NotFoundException("Provider does not exist");
        }
        if (Objects.nonNull(entityBO.getTemperature())
                && (entityBO.getTemperature() < 0.0 || entityBO.getTemperature() > 2.0)) {
            throw new RequestException("Temperature must be between 0.0 and 2.0");
        }
        if (Objects.nonNull(entityBO.getMaxTokens()) && entityBO.getMaxTokens() < 1) {
            throw new RequestException("Max tokens must be greater than 0");
        }
    }

    private void apply(ModelConfigBO targetBO, ModelConfigBO sourceBO) {
        targetBO.setModel(sourceBO.getModel().trim());
        targetBO.setLabel(StringUtils.defaultIfBlank(sourceBO.getLabel(), sourceBO.getModel()).trim());
        targetBO.setProviderId(sourceBO.getProviderId());
        targetBO.setStream(defaultBool(sourceBO.getStream(), true));
        targetBO.setToolCall(defaultBool(sourceBO.getToolCall(), true));
        targetBO.setVision(defaultBool(sourceBO.getVision(), false));
        targetBO.setReasoning(defaultBool(sourceBO.getReasoning(), false));
        targetBO.setTemperature(Objects.nonNull(sourceBO.getTemperature()) ? sourceBO.getTemperature()
                : fallbackTemperature);
        targetBO.setMaxTokens(Objects.nonNull(sourceBO.getMaxTokens()) ? sourceBO.getMaxTokens()
                : fallbackMaxTokens);
        targetBO.setDefaultFlag(Objects.nonNull(sourceBO.getDefaultFlag()) ? sourceBO.getDefaultFlag()
                : DefaultFlagEnum.NOT_DEFAULT);
        targetBO.setEnableFlag(Objects.nonNull(sourceBO.getEnableFlag()) ? sourceBO.getEnableFlag()
                : EnableFlagEnum.ENABLE);
        targetBO.setRemark(StringUtils.defaultString(sourceBO.getRemark()));
    }

    private void fillCreateAudit(ModelConfigBO entityBO, RequestHeader.UserHeader header) {
        LocalDateTime now = LocalDateTime.now();
        entityBO.setCreateTime(now);
        entityBO.setOperateTime(now);
        entityBO.setCreatorId(header.getUserId());
        entityBO.setCreatorName(header.getUserName());
        entityBO.setOperatorId(header.getUserId());
        entityBO.setOperatorName(header.getUserName());
        entityBO.setTenantId(header.getTenantId());
    }

    private void fillOperateAudit(ModelConfigBO entityBO, RequestHeader.UserHeader header) {
        entityBO.setOperateTime(LocalDateTime.now());
        entityBO.setOperatorId(header.getUserId());
        entityBO.setOperatorName(header.getUserName());
    }

    private void normalizeDefault(ModelConfigDO entityDO) {
        if (!Objects.equals(entityDO.getDefaultFlag(), DefaultFlagEnum.DEFAULT.getIndex())) {
            return;
        }
        modelConfigManager.update(Wrappers.<ModelConfigDO>lambdaUpdate()
                .set(ModelConfigDO::getDefaultFlag, DefaultFlagEnum.NOT_DEFAULT.getIndex())
                .eq(ModelConfigDO::getTenantId, entityDO.getTenantId())
                .eq(ModelConfigDO::getDefaultFlag, DefaultFlagEnum.DEFAULT.getIndex())
                .ne(ModelConfigDO::getId, entityDO.getId()));
    }

    private Boolean defaultBool(Boolean value, boolean defaultValue) {
        return Objects.nonNull(value) ? value : defaultValue;
    }

    private boolean truthy(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private ModelConfigBO fillProviderName(ModelConfigBO entityBO) {
        entityBO.setProviderName(resolveProviderName(entityBO.getProviderId()));
        return entityBO;
    }

    private String resolveProviderName(Long providerId) {
        if (Objects.isNull(providerId) || providerId == 0) {
            return null;
        }
        ModelProviderDO provider = modelProviderManager.getById(providerId);
        return Objects.isNull(provider) ? null : provider.getName();
    }

}
