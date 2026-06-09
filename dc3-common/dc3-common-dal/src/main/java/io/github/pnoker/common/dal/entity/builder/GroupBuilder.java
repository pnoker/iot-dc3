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
import io.github.pnoker.common.dal.entity.bo.GroupBO;
import io.github.pnoker.common.dal.entity.model.GroupDO;
import io.github.pnoker.common.dal.entity.vo.GroupVO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.utils.CodeUtil;
import io.github.pnoker.common.utils.MapStructUtil;
import io.github.pnoker.common.utils.PageUtil;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Optional;

/**
 * MapStruct builder converting between group BO, VO, and DO.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface GroupBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    @Mapping(target = "tenantId", ignore = true)
    GroupBO buildBOByVO(GroupVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO collection
     * @return EntityBO collection
     */
    List<GroupBO> buildBOListByVOList(List<GroupVO> entityVOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    GroupVO buildVOByBO(GroupBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO collection
     * @return EntityVO collection
     */
    List<GroupVO> buildVOListByBOList(List<GroupBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    @Mapping(target = "groupTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    GroupBO buildBOByDO(GroupDO entityDO);

    @AfterMapping
    default void afterProcess(GroupDO entityDO, @MappingTarget GroupBO entityBO) {
        // GroupType Flag
        Byte groupTypeFlag = entityDO.getGroupTypeFlag();
        entityBO.setGroupTypeFlag(EntityTypeEnum.ofIndex(groupTypeFlag));

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
    List<GroupBO> buildBOListByDOList(List<GroupDO> entityDOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "groupTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    GroupDO buildDOByBO(GroupBO entityBO);

    @AfterMapping
    default void afterProcess(GroupBO entityBO, @MappingTarget GroupDO entityDO) {
        // Code
        if (StringUtils.isEmpty(entityBO.getGroupCode())) {
            entityDO.setGroupCode(CodeUtil.getCode());
        }

        // GroupType Flag
        EntityTypeEnum groupTypeFlag = entityBO.getGroupTypeFlag();
        Optional.ofNullable(groupTypeFlag).ifPresent(value -> entityDO.setGroupTypeFlag(value.getIndex()));

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
    List<GroupDO> buildDOListByBOList(List<GroupBO> entityBOList);

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    default Page<GroupVO> buildVOPageByBOPage(Page<GroupBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

    /**
     * DOPage to BOPage
     *
     * @param entityPageDO EntityDO Page
     * @return EntityBO Page
     */
    default Page<GroupBO> buildBOPageByDOPage(Page<GroupDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

}
