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
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.manager.entity.bo.EventAttributeConfigBO;
import io.github.pnoker.common.manager.entity.model.EventAttributeConfigDO;
import io.github.pnoker.common.manager.entity.vo.EventAttributeConfigVO;
import io.github.pnoker.common.utils.MapStructUtil;
import io.github.pnoker.common.utils.PageUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Optional;

/**
 * EventAttributeConfig Builder
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface EventAttributeConfigBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    @Mapping(target = "tenantId", ignore = true)
    EventAttributeConfigBO buildBOByVO(EventAttributeConfigVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     */
    List<EventAttributeConfigBO> buildBOListByVOList(List<EventAttributeConfigVO> entityVOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    EventAttributeConfigDO buildDOByBO(EventAttributeConfigBO entityBO);

    @AfterMapping
    default void afterProcess(EventAttributeConfigBO entityBO, @MappingTarget EventAttributeConfigDO entityDO) {
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
    List<EventAttributeConfigDO> buildDOListByBOList(List<EventAttributeConfigBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    @Mapping(target = "enableFlag", ignore = true)
    EventAttributeConfigBO buildBOByDO(EventAttributeConfigDO entityDO);

    @AfterMapping
    default void afterProcess(EventAttributeConfigDO entityDO, @MappingTarget EventAttributeConfigBO entityBO) {
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
    List<EventAttributeConfigBO> buildBOListByDOList(List<EventAttributeConfigDO> entityDOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    EventAttributeConfigVO buildVOByBO(EventAttributeConfigBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     */
    List<EventAttributeConfigVO> buildVOListByBOList(List<EventAttributeConfigBO> entityBOList);

    /**
     * DOPage to BOPage
     *
     * @param entityPageDO EntityDO Page
     * @return EntityBO Page
     */
    default Page<EventAttributeConfigBO> buildBOPageByDOPage(Page<EventAttributeConfigDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    default Page<EventAttributeConfigVO> buildVOPageByBOPage(Page<EventAttributeConfigBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

}
