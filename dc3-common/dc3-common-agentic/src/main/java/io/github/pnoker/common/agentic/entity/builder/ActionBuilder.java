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
import io.github.pnoker.common.agentic.entity.bo.ActionBO;
import io.github.pnoker.common.agentic.entity.model.ActionDO;
import io.github.pnoker.common.agentic.entity.vo.ActionVO;
import io.github.pnoker.common.enums.AgenticActionStatusEnum;
import io.github.pnoker.common.utils.MapStructUtil;
import io.github.pnoker.common.utils.PageUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Optional;

/**
 * MapStruct builder converting between action BO, VO, and DO.
 *
 * @author pnoker
 * @since 2026.5.11
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface ActionBuilder {

    /**
     * Convert vo to bo.
     *
     * @param entityVO view object
     * @return converted value
     */
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    ActionBO buildBOByVO(ActionVO entityVO);

    /**
     * Convert vo list to bo list.
     *
     * @param entityVOList entity view object list
     * @return converted value
     */
    List<ActionBO> buildBOListByVOList(List<ActionVO> entityVOList);

    /**
     * Convert bo to do.
     *
     * @param entityBO business object
     * @return converted value
     */
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    ActionDO buildDOByBO(ActionBO entityBO);

    /**
     * After process.
     *
     * @param entityBO business object
     * @param entityDO persistence object
     */
    @AfterMapping
    default void afterProcess(ActionBO entityBO, @MappingTarget ActionDO entityDO) {
        AgenticActionStatusEnum status = entityBO.getStatus();
        Optional.ofNullable(status).ifPresent(value -> entityDO.setStatus(value.getIndex()));
    }

    /**
     * Convert bo list to do list.
     *
     * @param entityBOList entity business object list
     * @return converted value
     */
    List<ActionDO> buildDOListByBOList(List<ActionBO> entityBOList);

    /**
     * Convert do to bo.
     *
     * @param entityDO persistence object
     * @return converted value
     */
    @Mapping(target = "status", ignore = true)
    ActionBO buildBOByDO(ActionDO entityDO);

    /**
     * After process.
     *
     * @param entityDO persistence object
     * @param entityBO business object
     */
    @AfterMapping
    default void afterProcess(ActionDO entityDO, @MappingTarget ActionBO entityBO) {
        Byte status = entityDO.getStatus();
        entityBO.setStatus(AgenticActionStatusEnum.ofIndex(status));
    }

    /**
     * Convert do list to bo list.
     *
     * @param entityDOList entity persistence object list
     * @return converted value
     */
    List<ActionBO> buildBOListByDOList(List<ActionDO> entityDOList);

    /**
     * Convert bo to vo.
     *
     * @param entityBO business object
     * @return converted value
     */
    ActionVO buildVOByBO(ActionBO entityBO);

    /**
     * Convert bo list to vo list.
     *
     * @param entityBOList entity business object list
     * @return converted value
     */
    List<ActionVO> buildVOListByBOList(List<ActionBO> entityBOList);

    /**
     * Convert do page to bo page.
     *
     * @param entityPageDO persistence object
     * @return converted value
     */
    default Page<ActionBO> buildBOPageByDOPage(Page<ActionDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    /**
     * Convert bo page to vo page.
     *
     * @param entityPageBO business object
     * @return converted value
     */
    default Page<ActionVO> buildVOPageByBOPage(Page<ActionBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

}
