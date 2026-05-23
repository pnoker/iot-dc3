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
import io.github.pnoker.common.agentic.entity.bo.ModelConfigBO;
import io.github.pnoker.common.agentic.entity.model.ModelConfigDO;
import io.github.pnoker.common.agentic.entity.request.ModelConfigRequest;
import io.github.pnoker.common.agentic.entity.vo.ModelConfigVO;
import io.github.pnoker.common.enums.DefaultFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.utils.MapStructUtil;
import io.github.pnoker.common.utils.PageUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Optional;

/**
 * MapStruct builder converting between model configuration BO, VO, and DO.
 *
 * @author pnoker
 * @version 2026.5.11
 * @since 2026.5.11
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface ModelConfigBuilder {

    ModelConfigBO buildBOByVO(ModelConfigVO entityVO);

    List<ModelConfigBO> buildBOListByVOList(List<ModelConfigVO> entityVOList);

    @Mapping(target = "providerName", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    @Mapping(target = "creatorName", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "operatorId", ignore = true)
    @Mapping(target = "operatorName", ignore = true)
    @Mapping(target = "operateTime", ignore = true)
    ModelConfigBO buildBOByRequest(ModelConfigRequest entityRequest);

    @Mapping(target = "defaultFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    ModelConfigDO buildDOByBO(ModelConfigBO entityBO);

    @AfterMapping
    default void afterProcess(ModelConfigBO entityBO, @MappingTarget ModelConfigDO entityDO) {
        DefaultFlagEnum defaultFlag = entityBO.getDefaultFlag();
        Optional.ofNullable(defaultFlag).ifPresent(value -> entityDO.setDefaultFlag(value.getIndex()));

        EnableFlagEnum enableFlag = entityBO.getEnableFlag();
        Optional.ofNullable(enableFlag).ifPresent(value -> entityDO.setEnableFlag(value.getIndex()));
    }

    List<ModelConfigDO> buildDOListByBOList(List<ModelConfigBO> entityBOList);

    @Mapping(target = "providerName", ignore = true)
    @Mapping(target = "defaultFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    ModelConfigBO buildBOByDO(ModelConfigDO entityDO);

    @AfterMapping
    default void afterProcess(ModelConfigDO entityDO, @MappingTarget ModelConfigBO entityBO) {
        Byte defaultFlag = entityDO.getDefaultFlag();
        entityBO.setDefaultFlag(DefaultFlagEnum.ofIndex(defaultFlag));

        Byte enableFlag = entityDO.getEnableFlag();
        entityBO.setEnableFlag(EnableFlagEnum.ofIndex(enableFlag));
    }

    List<ModelConfigBO> buildBOListByDOList(List<ModelConfigDO> entityDOList);

    ModelConfigVO buildVOByBO(ModelConfigBO entityBO);

    List<ModelConfigVO> buildVOListByBOList(List<ModelConfigBO> entityBOList);

    default Page<ModelConfigBO> buildBOPageByDOPage(Page<ModelConfigDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    default Page<ModelConfigVO> buildVOPageByBOPage(Page<ModelConfigBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

}
