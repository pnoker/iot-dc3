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

package io.github.pnoker.common.dal.entity.builder;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.dal.entity.bo.LabelBindBO;
import io.github.pnoker.common.dal.entity.model.LabelBindDO;
import io.github.pnoker.common.dal.entity.vo.LabelBindVO;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.utils.MapStructUtil;
import io.github.pnoker.common.utils.PageUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Optional;

/**
 * MapStruct builder converting between label binding BO, VO, and DO.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface LabelBindBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    @Mapping(target = "tenantId", ignore = true)
    LabelBindBO buildBOByVO(LabelBindVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO collection
     * @return EntityBO collection
     */
    List<LabelBindBO> buildBOListByVOList(List<LabelBindVO> entityVOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    LabelBindVO buildVOByBO(LabelBindBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO collection
     * @return EntityVO collection
     */
    List<LabelBindVO> buildVOListByBOList(List<LabelBindBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    @Mapping(target = "entityTypeFlag", ignore = true)
    LabelBindBO buildBOByDO(LabelBindDO entityDO);

    @AfterMapping
    default void afterProcess(LabelBindDO entityDO, @MappingTarget LabelBindBO entityBO) {
        // EntityType Flag
        Byte entityTypeFlag = entityDO.getEntityTypeFlag();
        entityBO.setEntityTypeFlag(EntityTypeEnum.ofIndex(entityTypeFlag));
    }

    /**
     * DOList to BOList
     *
     * @param entityDOList EntityDO Array
     * @return EntityBO Array
     */
    List<LabelBindBO> buildBOListByDOList(List<LabelBindDO> entityDOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "entityTypeFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    LabelBindDO buildDOByBO(LabelBindBO entityBO);

    @AfterMapping
    default void afterProcess(LabelBindBO entityBO, @MappingTarget LabelBindDO entityDO) {
        // EntityType Flag
        EntityTypeEnum entityTypeFlag = entityBO.getEntityTypeFlag();
        Optional.ofNullable(entityTypeFlag).ifPresent(value -> entityDO.setEntityTypeFlag(value.getIndex()));
    }

    /**
     * BOList to DOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityDO Array
     */
    List<LabelBindDO> buildDOListByBOList(List<LabelBindBO> entityBOList);

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    default Page<LabelBindVO> buildVOPageByBOPage(Page<LabelBindBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

    /**
     * DOPage to BOPage
     *
     * @param entityPageDO EntityDO Page
     * @return EntityBO Page
     */
    default Page<LabelBindBO> buildBOPageByDOPage(Page<LabelBindDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

}
