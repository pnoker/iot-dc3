/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * @version 2025.2.4
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