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
import io.github.pnoker.common.entity.ext.CommandAttributeExt;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.manager.entity.bo.CommandAttributeBO;
import io.github.pnoker.common.manager.entity.model.CommandAttributeDO;
import io.github.pnoker.common.manager.entity.vo.CommandAttributeVO;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.MapStructUtil;
import io.github.pnoker.common.utils.PageUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * MapStruct builder converting between command attribute BO, VO, and DO.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface CommandAttributeBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    @Mapping(target = "tenantId", ignore = true)
    CommandAttributeBO buildBOByVO(CommandAttributeVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     */
    List<CommandAttributeBO> buildBOListByVOList(List<CommandAttributeVO> entityVOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "attributeExt", ignore = true)
    @Mapping(target = "attributeTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    CommandAttributeDO buildDOByBO(CommandAttributeBO entityBO);

    @AfterMapping
    default void afterProcess(CommandAttributeBO entityBO, @MappingTarget CommandAttributeDO entityDO) {
        // Json Ext
        CommandAttributeExt entityExt = entityBO.getAttributeExt();
        JsonExt ext = new JsonExt();
        if (Objects.nonNull(entityExt)) {
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.toJsonString(entityExt.getContent()));
        }
        entityDO.setAttributeExt(ext);

        // AttributeType Flag
        AttributeTypeEnum attributeTypeFlag = entityBO.getAttributeTypeFlag();
        Optional.ofNullable(attributeTypeFlag).ifPresent(value -> entityDO.setAttributeTypeFlag(value.getIndex()));

        // Enable Flag
        EnableFlagEnum enableFlag = entityBO.getEnableFlag();
        Optional.ofNullable(enableFlag).ifPresent(value -> entityDO.setEnableFlag(value.getIndex()));
    }

    /**
     * BOList to DOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityDO Array
     */
    List<CommandAttributeDO> buildDOListByBOList(List<CommandAttributeBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    @Mapping(target = "attributeExt", ignore = true)
    @Mapping(target = "attributeTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    CommandAttributeBO buildBOByDO(CommandAttributeDO entityDO);

    @AfterMapping
    default void afterProcess(CommandAttributeDO entityDO, @MappingTarget CommandAttributeBO entityBO) {
        // Json Ext
        JsonExt entityExt = entityDO.getAttributeExt();
        if (Objects.nonNull(entityExt)) {
            CommandAttributeExt ext = new CommandAttributeExt();
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.parseObject(entityExt.getContent(), CommandAttributeExt.Content.class));
            entityBO.setAttributeExt(ext);
        }

        // AttributeType Flag
        Byte attributeTypeFlag = entityDO.getAttributeTypeFlag();
        entityBO.setAttributeTypeFlag(AttributeTypeEnum.ofIndex(attributeTypeFlag));

        // Enable Flag
        Byte enableFlag = entityDO.getEnableFlag();
        entityBO.setEnableFlag(EnableFlagEnum.ofIndex(enableFlag));
    }

    /**
     * DOList to BOList
     *
     * @param entityDOList EntityDO Array
     * @return EntityBO Array
     */
    List<CommandAttributeBO> buildBOListByDOList(List<CommandAttributeDO> entityDOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    CommandAttributeVO buildVOByBO(CommandAttributeBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     */
    List<CommandAttributeVO> buildVOListByBOList(List<CommandAttributeBO> entityBOList);

    /**
     * DOPage to BOPage
     *
     * @param entityPageDO EntityDO Page
     * @return EntityBO Page
     */
    default Page<CommandAttributeBO> buildBOPageByDOPage(Page<CommandAttributeDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    default Page<CommandAttributeVO> buildVOPageByBOPage(Page<CommandAttributeBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

}
