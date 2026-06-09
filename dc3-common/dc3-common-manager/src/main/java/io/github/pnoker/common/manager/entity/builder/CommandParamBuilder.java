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

package io.github.pnoker.common.manager.entity.builder;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.entity.ext.CommandParamExt;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ParamDirectionTypeEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.manager.entity.bo.CommandParamBO;
import io.github.pnoker.common.manager.entity.model.CommandParamDO;
import io.github.pnoker.common.manager.entity.vo.CommandParamVO;
import io.github.pnoker.common.utils.CodeUtil;
import io.github.pnoker.common.utils.JsonUtil;
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
 * MapStruct builder converting between command param BO, VO, and DO.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface CommandParamBuilder {

    @Mapping(target = "tenantId", ignore = true)
    CommandParamBO buildBOByVO(CommandParamVO entityVO);

    List<CommandParamBO> buildBOListByVOList(List<CommandParamVO> entityVOList);

    @Mapping(target = "paramExt", ignore = true)
    @Mapping(target = "paramDirectionFlag", ignore = true)
    @Mapping(target = "paramTypeFlag", ignore = true)
    @Mapping(target = "requiredFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    CommandParamDO buildDOByBO(CommandParamBO entityBO);

    @AfterMapping
    default void afterBuildDO(CommandParamBO entityBO, @MappingTarget CommandParamDO entityDO) {
        if (StringUtils.isEmpty(entityBO.getParamCode())) {
            entityDO.setParamCode(CodeUtil.getCode());
        }

        CommandParamExt entityExt = entityBO.getParamExt();
        JsonExt ext = new JsonExt();
        if (Objects.nonNull(entityExt)) {
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.toJsonString(entityExt.getContent()));
        }
        entityDO.setParamExt(ext);

        Optional.ofNullable(entityBO.getParamDirectionFlag()).ifPresent(value -> entityDO.setParamDirectionFlag(value.getIndex()));
        Optional.ofNullable(entityBO.getParamTypeFlag()).ifPresent(value -> entityDO.setParamTypeFlag(value.getIndex()));
        Optional.ofNullable(entityBO.getRequiredFlag()).ifPresent(value -> entityDO.setRequiredFlag(value ? (byte) 1 : (byte) 0));
        Optional.ofNullable(entityBO.getEnableFlag()).ifPresent(value -> entityDO.setEnableFlag(value.getIndex()));
    }

    List<CommandParamDO> buildDOListByBOList(List<CommandParamBO> entityBOList);

    @Mapping(target = "paramExt", ignore = true)
    @Mapping(target = "paramDirectionFlag", ignore = true)
    @Mapping(target = "paramTypeFlag", ignore = true)
    @Mapping(target = "requiredFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    CommandParamBO buildBOByDO(CommandParamDO entityDO);

    @AfterMapping
    default void afterBuildBO(CommandParamDO entityDO, @MappingTarget CommandParamBO entityBO) {
        JsonExt entityExt = entityDO.getParamExt();
        if (Objects.nonNull(entityExt)) {
            CommandParamExt ext = new CommandParamExt();
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.parseObject(entityExt.getContent(), CommandParamExt.Content.class));
            entityBO.setParamExt(ext);
        }

        entityBO.setParamDirectionFlag(ParamDirectionTypeEnum.ofIndex(entityDO.getParamDirectionFlag()));
        entityBO.setParamTypeFlag(PointTypeEnum.ofIndex(entityDO.getParamTypeFlag()));
        entityBO.setRequiredFlag(Objects.equals(entityDO.getRequiredFlag(), (byte) 1));
        entityBO.setEnableFlag(EnableFlagEnum.ofIndex(entityDO.getEnableFlag()));
    }

    List<CommandParamBO> buildBOListByDOList(List<CommandParamDO> entityDOList);

    CommandParamVO buildVOByBO(CommandParamBO entityBO);

    List<CommandParamVO> buildVOListByBOList(List<CommandParamBO> entityBOList);

    default Page<CommandParamBO> buildBOPageByDOPage(Page<CommandParamDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    default Page<CommandParamVO> buildVOPageByBOPage(Page<CommandParamBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

}
