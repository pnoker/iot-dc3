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
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Optional;

/**
 * LabelBind Builder
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
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
     * @param entityVOList EntityVO 集合
     * @return EntityBO 集合
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
     * @param entityBOList EntityBO 集合
     * @return EntityVO 集合
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
        Byte groupTypeFlag = entityDO.getEntityTypeFlag();
        entityBO.setEntityTypeFlag(EntityTypeFlagEnum.ofIndex(groupTypeFlag));
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
        EntityTypeFlagEnum entityTypeFlag = entityBO.getEntityTypeFlag();
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
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "countId", ignore = true)
    @Mapping(target = "maxLimit", ignore = true)
    @Mapping(target = "searchCount", ignore = true)
    @Mapping(target = "optimizeCountSql", ignore = true)
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<LabelBindVO> buildVOPageByBOPage(Page<LabelBindBO> entityPageBO);

    /**
     * DOPage to BOPage
     *
     * @param entityPageDO EntityDO Page
     * @return EntityBO Page
     */
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "countId", ignore = true)
    @Mapping(target = "maxLimit", ignore = true)
    @Mapping(target = "searchCount", ignore = true)
    @Mapping(target = "optimizeCountSql", ignore = true)
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<LabelBindBO> buildBOPageByDOPage(Page<LabelBindDO> entityPageDO);
}