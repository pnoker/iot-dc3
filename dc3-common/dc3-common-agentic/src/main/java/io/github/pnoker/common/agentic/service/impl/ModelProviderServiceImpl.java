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
import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.agentic.entity.builder.ModelProviderBuilder;
import io.github.pnoker.common.agentic.entity.model.ModelProviderDO;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Implements model provider listing, save, update, and remove operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Service
@RequiredArgsConstructor
public class ModelProviderServiceImpl implements ModelProviderService {

    private final ModelProviderManager modelProviderManager;
    private final ModelProviderBuilder modelProviderBuilder;
    private final ChatClientFactory chatClientFactory;

    @Override
    public List<ModelProviderBO> list() {
        List<ModelProviderDO> entityDOList = modelProviderManager.list(Wrappers.<ModelProviderDO>query()
                .lambda()
                .orderByDesc(ModelProviderDO::getDefaultFlag)
                .orderByAsc(ModelProviderDO::getName));
        return modelProviderBuilder.buildBOListByDOList(entityDOList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelProviderBO save(ModelProviderBO entityBO, RequestHeader.UserHeader header) {
        validate(entityBO);
        ModelProviderBO targetBO = new ModelProviderBO();
        apply(targetBO, entityBO, false);
        fillCreateAudit(targetBO, header);
        ModelProviderDO entityDO = modelProviderBuilder.buildDOByBO(targetBO);
        modelProviderManager.save(entityDO);
        normalizeDefault(entityDO);
        return modelProviderBuilder.buildBOByDO(entityDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelProviderBO update(ModelProviderBO entityBO, RequestHeader.UserHeader header) {
        if (Objects.isNull(entityBO) || Objects.isNull(entityBO.getId())) {
            throw new RequestException("Provider ID is required");
        }
        validate(entityBO);
        ModelProviderDO existingDO = modelProviderManager.getById(entityBO.getId());
        if (Objects.isNull(existingDO)) {
            throw new NotFoundException("Provider does not exist");
        }
        ModelProviderBO targetBO = modelProviderBuilder.buildBOByDO(existingDO);
        apply(targetBO, entityBO, true);
        fillOperateAudit(targetBO, header);
        ModelProviderDO entityDO = modelProviderBuilder.buildDOByBO(targetBO);
        modelProviderManager.updateById(entityDO);
        normalizeDefault(entityDO);
        chatClientFactory.evict(entityDO.getId());
        return modelProviderBuilder.buildBOByDO(entityDO);
    }

    @Override
    public void remove(Long id) {
        modelProviderManager.removeById(id);
        chatClientFactory.evict(id);
    }

    private void validate(ModelProviderBO entityBO) {
        if (Objects.isNull(entityBO) || StringUtils.isBlank(entityBO.getName())) {
            throw new RequestException("Provider name is required");
        }
        if (StringUtils.isBlank(entityBO.getBaseUrl())) {
            throw new RequestException("Provider base URL is required");
        }
    }

    private void apply(ModelProviderBO targetBO, ModelProviderBO sourceBO, boolean keepExistingApiKey) {
        targetBO.setName(sourceBO.getName().trim());
        targetBO.setProviderType(Objects.nonNull(sourceBO.getProviderType()) ? sourceBO.getProviderType()
                : AgenticModelProviderTypeEnum.OPENAI_COMPATIBLE);
        targetBO.setBaseUrl(sourceBO.getBaseUrl().trim());
        if (StringUtils.isNotBlank(sourceBO.getApiKey())) {
            targetBO.setApiKey(sourceBO.getApiKey().trim());
        } else if (!keepExistingApiKey) {
            targetBO.setApiKey(null);
        }
        targetBO.setDefaultFlag(Objects.nonNull(sourceBO.getDefaultFlag()) ? sourceBO.getDefaultFlag()
                : DefaultFlagEnum.NOT_DEFAULT);
        targetBO.setEnableFlag(Objects.nonNull(sourceBO.getEnableFlag()) ? sourceBO.getEnableFlag()
                : EnableFlagEnum.ENABLE);
        targetBO.setRemark(StringUtils.defaultString(sourceBO.getRemark()));
    }

    private void fillCreateAudit(ModelProviderBO entityBO, RequestHeader.UserHeader header) {
        LocalDateTime now = LocalDateTime.now();
        entityBO.setCreateTime(now);
        entityBO.setOperateTime(now);
        entityBO.setCreatorId(header.getUserId());
        entityBO.setCreatorName(header.getUserName());
        entityBO.setOperatorId(header.getUserId());
        entityBO.setOperatorName(header.getUserName());
        entityBO.setTenantId(header.getTenantId());
    }

    private void fillOperateAudit(ModelProviderBO entityBO, RequestHeader.UserHeader header) {
        entityBO.setOperateTime(LocalDateTime.now());
        entityBO.setOperatorId(header.getUserId());
        entityBO.setOperatorName(header.getUserName());
    }

    private void normalizeDefault(ModelProviderDO entityDO) {
        if (!Objects.equals(entityDO.getDefaultFlag(), DefaultFlagEnum.DEFAULT.getIndex())) {
            return;
        }
        modelProviderManager.update(Wrappers.<ModelProviderDO>lambdaUpdate()
                .set(ModelProviderDO::getDefaultFlag, DefaultFlagEnum.NOT_DEFAULT.getIndex())
                .eq(ModelProviderDO::getTenantId, entityDO.getTenantId())
                .eq(ModelProviderDO::getDefaultFlag, DefaultFlagEnum.DEFAULT.getIndex())
                .ne(ModelProviderDO::getId, entityDO.getId()));
    }

}
