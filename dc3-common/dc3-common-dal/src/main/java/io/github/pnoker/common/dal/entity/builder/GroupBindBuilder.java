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
import io.github.pnoker.common.dal.entity.bo.GroupBindBO;
import io.github.pnoker.common.dal.entity.model.GroupBindDO;
import io.github.pnoker.common.dal.entity.vo.GroupBindVO;
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
 * MapStruct builder converting between group binding BO, VO, and DO.
 *
 * @author pnoker
 * @version 2026.5.11
 * @since 2026.5.11
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface GroupBindBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    @Mapping(target = "tenantId", ignore = true)
    GroupBindBO buildBOByVO(GroupBindVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO collection
     * @return EntityBO collection
     */
    List<GroupBindBO> buildBOListByVOList(List<GroupBindVO> entityVOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    GroupBindVO buildVOByBO(GroupBindBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO collection
     * @return EntityVO collection
     */
    List<GroupBindVO> buildVOListByBOList(List<GroupBindBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    @Mapping(target = "entityTypeFlag", ignore = true)
    GroupBindBO buildBOByDO(GroupBindDO entityDO);

    @AfterMapping
    default void afterProcess(GroupBindDO entityDO, @MappingTarget GroupBindBO entityBO) {
        Byte entityTypeFlag = entityDO.getEntityTypeFlag();
        entityBO.setEntityTypeFlag(EntityTypeEnum.ofIndex(entityTypeFlag));
    }

    /**
     * DOList to BOList
     *
     * @param entityDOList EntityDO Array
     * @return EntityBO Array
     */
    List<GroupBindBO> buildBOListByDOList(List<GroupBindDO> entityDOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "entityTypeFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    GroupBindDO buildDOByBO(GroupBindBO entityBO);

    @AfterMapping
    default void afterProcess(GroupBindBO entityBO, @MappingTarget GroupBindDO entityDO) {
        EntityTypeEnum entityTypeFlag = entityBO.getEntityTypeFlag();
        Optional.ofNullable(entityTypeFlag).ifPresent(value -> entityDO.setEntityTypeFlag(value.getIndex()));
    }

    /**
     * BOList to DOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityDO Array
     */
    List<GroupBindDO> buildDOListByBOList(List<GroupBindBO> entityBOList);

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    default Page<GroupBindVO> buildVOPageByBOPage(Page<GroupBindBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

    /**
     * DOPage to BOPage
     *
     * @param entityPageDO EntityDO Page
     * @return EntityBO Page
     */
    default Page<GroupBindBO> buildBOPageByDOPage(Page<GroupBindDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

}
