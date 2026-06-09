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
import io.github.pnoker.common.entity.ext.EventAttributeExt;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.manager.entity.bo.EventAttributeBO;
import io.github.pnoker.common.manager.entity.model.EventAttributeDO;
import io.github.pnoker.common.manager.entity.vo.EventAttributeVO;
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
 * MapStruct builder converting between event attribute BO, VO, and DO.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface EventAttributeBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    @Mapping(target = "tenantId", ignore = true)
    EventAttributeBO buildBOByVO(EventAttributeVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     */
    List<EventAttributeBO> buildBOListByVOList(List<EventAttributeVO> entityVOList);

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
    EventAttributeDO buildDOByBO(EventAttributeBO entityBO);

    @AfterMapping
    default void afterProcess(EventAttributeBO entityBO, @MappingTarget EventAttributeDO entityDO) {
        // Json Ext
        EventAttributeExt entityExt = entityBO.getAttributeExt();
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
    List<EventAttributeDO> buildDOListByBOList(List<EventAttributeBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    @Mapping(target = "attributeExt", ignore = true)
    @Mapping(target = "attributeTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    EventAttributeBO buildBOByDO(EventAttributeDO entityDO);

    @AfterMapping
    default void afterProcess(EventAttributeDO entityDO, @MappingTarget EventAttributeBO entityBO) {
        // Json Ext
        JsonExt entityExt = entityDO.getAttributeExt();
        if (Objects.nonNull(entityExt)) {
            EventAttributeExt ext = new EventAttributeExt();
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.parseObject(entityExt.getContent(), EventAttributeExt.Content.class));
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
    List<EventAttributeBO> buildBOListByDOList(List<EventAttributeDO> entityDOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    EventAttributeVO buildVOByBO(EventAttributeBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     */
    List<EventAttributeVO> buildVOListByBOList(List<EventAttributeBO> entityBOList);

    /**
     * DOPage to BOPage
     *
     * @param entityPageDO EntityDO Page
     * @return EntityBO Page
     */
    default Page<EventAttributeBO> buildBOPageByDOPage(Page<EventAttributeDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    default Page<EventAttributeVO> buildVOPageByBOPage(Page<EventAttributeBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

}
