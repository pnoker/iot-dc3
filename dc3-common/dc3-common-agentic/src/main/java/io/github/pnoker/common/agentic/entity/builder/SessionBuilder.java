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
import io.github.pnoker.common.agentic.entity.bo.SessionBO;
import io.github.pnoker.common.agentic.entity.model.SessionDO;
import io.github.pnoker.common.agentic.entity.vo.SessionVO;
import io.github.pnoker.common.utils.MapStructUtil;
import io.github.pnoker.common.utils.PageUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct builder converting between session BO, VO, and DO representations.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface SessionBuilder {

    /**
     * Convert vo to bo.
     *
     * @param entityVO view object
     * @return converted value
     */
    SessionBO buildBOByVO(SessionVO entityVO);

    /**
     * Convert vo list to bo list.
     *
     * @param entityVOList entity view object list
     * @return converted value
     */
    List<SessionBO> buildBOListByVOList(List<SessionVO> entityVOList);

    /**
     * Convert bo to vo.
     *
     * @param entityBO business object
     * @return converted value
     */
    SessionVO buildVOByBO(SessionBO entityBO);

    /**
     * Convert bo list to vo list.
     *
     * @param entityBOList entity business object list
     * @return converted value
     */
    List<SessionVO> buildVOListByBOList(List<SessionBO> entityBOList);

    /**
     * Convert do to bo.
     *
     * @param entityDO persistence object
     * @return converted value
     */
    SessionBO buildBOByDO(SessionDO entityDO);

    /**
     * Convert do list to bo list.
     *
     * @param entityDOList entity persistence object list
     * @return converted value
     */
    List<SessionBO> buildBOListByDOList(List<SessionDO> entityDOList);

    /**
     * Convert bo to do.
     *
     * @param entityBO business object
     * @return converted value
     */
    @Mapping(target = "deleted", ignore = true)
    SessionDO buildDOByBO(SessionBO entityBO);

    /**
     * Convert bo list to do list.
     *
     * @param entityBOList entity business object list
     * @return converted value
     */
    List<SessionDO> buildDOListByBOList(List<SessionBO> entityBOList);

    /**
     * Convert bo page to vo page.
     *
     * @param entityPageBO business object
     * @return converted value
     */
    default Page<SessionVO> buildVOPageByBOPage(Page<SessionBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

    /**
     * Convert do page to bo page.
     *
     * @param entityPageDO persistence object
     * @return converted value
     */
    default Page<SessionBO> buildBOPageByDOPage(Page<SessionDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

}
