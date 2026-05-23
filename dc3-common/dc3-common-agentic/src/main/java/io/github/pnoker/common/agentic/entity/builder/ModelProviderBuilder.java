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
package io.github.pnoker.common.agentic.entity.builder;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.agentic.entity.model.ModelProviderDO;
import io.github.pnoker.common.agentic.entity.request.ModelProviderRequest;
import io.github.pnoker.common.agentic.entity.vo.ModelProviderVO;
import io.github.pnoker.common.enums.AgenticModelProviderTypeEnum;
import io.github.pnoker.common.enums.DefaultFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.utils.MapStructUtil;
import io.github.pnoker.common.utils.PageUtil;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * MapStruct builder converting between model provider BO, VO, and DO.
 *
 * @author pnoker
 * @version 2026.5.11
 * @since 2026.5.11
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface ModelProviderBuilder {

    @Mapping(target = "apiKey", ignore = true)
    ModelProviderBO buildBOByVO(ModelProviderVO entityVO);

    List<ModelProviderBO> buildBOListByVOList(List<ModelProviderVO> entityVOList);

    @Mapping(target = "providerType", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    @Mapping(target = "creatorName", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "operatorId", ignore = true)
    @Mapping(target = "operatorName", ignore = true)
    @Mapping(target = "operateTime", ignore = true)
    ModelProviderBO buildBOByRequest(ModelProviderRequest entityRequest);

    @AfterMapping
    default void afterProcess(ModelProviderRequest entityRequest, @MappingTarget ModelProviderBO entityBO) {
        if (Objects.isNull(entityRequest)) {
            return;
        }
        entityBO.setProviderType(providerType(entityRequest.getProviderType()));
    }

    @Mapping(target = "providerType", ignore = true)
    @Mapping(target = "defaultFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    ModelProviderDO buildDOByBO(ModelProviderBO entityBO);

    @AfterMapping
    default void afterProcess(ModelProviderBO entityBO, @MappingTarget ModelProviderDO entityDO) {
        AgenticModelProviderTypeEnum providerType = entityBO.getProviderType();
        Optional.ofNullable(providerType).ifPresent(value -> entityDO.setProviderType(value.getIndex()));

        DefaultFlagEnum defaultFlag = entityBO.getDefaultFlag();
        Optional.ofNullable(defaultFlag).ifPresent(value -> entityDO.setDefaultFlag(value.getIndex()));

        EnableFlagEnum enableFlag = entityBO.getEnableFlag();
        Optional.ofNullable(enableFlag).ifPresent(value -> entityDO.setEnableFlag(value.getIndex()));
    }

    List<ModelProviderDO> buildDOListByBOList(List<ModelProviderBO> entityBOList);

    @Mapping(target = "providerType", ignore = true)
    @Mapping(target = "defaultFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    ModelProviderBO buildBOByDO(ModelProviderDO entityDO);

    @AfterMapping
    default void afterProcess(ModelProviderDO entityDO, @MappingTarget ModelProviderBO entityBO) {
        Byte providerType = entityDO.getProviderType();
        entityBO.setProviderType(AgenticModelProviderTypeEnum.ofIndex(providerType));

        Byte defaultFlag = entityDO.getDefaultFlag();
        entityBO.setDefaultFlag(DefaultFlagEnum.ofIndex(defaultFlag));

        Byte enableFlag = entityDO.getEnableFlag();
        entityBO.setEnableFlag(EnableFlagEnum.ofIndex(enableFlag));
    }

    List<ModelProviderBO> buildBOListByDOList(List<ModelProviderDO> entityDOList);

    ModelProviderVO buildVOByBO(ModelProviderBO entityBO);

    List<ModelProviderVO> buildVOListByBOList(List<ModelProviderBO> entityBOList);

    default Page<ModelProviderBO> buildBOPageByDOPage(Page<ModelProviderDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    default Page<ModelProviderVO> buildVOPageByBOPage(Page<ModelProviderBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

    default AgenticModelProviderTypeEnum providerType(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String normalized = value.trim();
        AgenticModelProviderTypeEnum providerType = AgenticModelProviderTypeEnum.ofCode(normalized);
        if (Objects.nonNull(providerType)) {
            return providerType;
        }
        try {
            return AgenticModelProviderTypeEnum.ofIndex(Byte.valueOf(normalized));
        } catch (NumberFormatException e) {
            return AgenticModelProviderTypeEnum.ofName(normalized.toUpperCase().replace('-', '_'));
        }
    }

}
