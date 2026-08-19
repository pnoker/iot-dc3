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
import io.github.pnoker.common.agentic.entity.bo.AttachmentBO;
import io.github.pnoker.common.agentic.entity.model.AttachmentDO;
import io.github.pnoker.common.agentic.entity.vo.AttachmentVO;
import io.github.pnoker.common.utils.MapStructUtil;
import io.github.pnoker.common.utils.PageUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct builder converting between attachment BO, VO, and DO.
 *
 * @author pnoker
 * @since 2026.5.11
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface AttachmentBuilder {

    /**
     * Convert vo to bo.
     *
     * @param entityVO view object
     * @return converted value
     */
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    AttachmentBO buildBOByVO(AttachmentVO entityVO);

    /**
     * Convert vo list to bo list.
     *
     * @param entityVOList entity view object list
     * @return converted value
     */
    List<AttachmentBO> buildBOListByVOList(List<AttachmentVO> entityVOList);

    /**
     * Convert bo to do.
     *
     * @param entityBO business object
     * @return converted value
     */
    @Mapping(target = "deleted", ignore = true)
    AttachmentDO buildDOByBO(AttachmentBO entityBO);

    /**
     * Convert bo list to do list.
     *
     * @param entityBOList entity business object list
     * @return converted value
     */
    List<AttachmentDO> buildDOListByBOList(List<AttachmentBO> entityBOList);

    /**
     * Convert do to bo.
     *
     * @param entityDO persistence object
     * @return converted value
     */
    AttachmentBO buildBOByDO(AttachmentDO entityDO);

    /**
     * Convert do list to bo list.
     *
     * @param entityDOList entity persistence object list
     * @return converted value
     */
    List<AttachmentBO> buildBOListByDOList(List<AttachmentDO> entityDOList);

    /**
     * Convert bo to vo.
     *
     * @param entityBO business object
     * @return converted value
     */
    AttachmentVO buildVOByBO(AttachmentBO entityBO);

    /**
     * Convert bo list to vo list.
     *
     * @param entityBOList entity business object list
     * @return converted value
     */
    List<AttachmentVO> buildVOListByBOList(List<AttachmentBO> entityBOList);

    /**
     * Convert do page to bo page.
     *
     * @param entityPageDO persistence object
     * @return converted value
     */
    default Page<AttachmentBO> buildBOPageByDOPage(Page<AttachmentDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    /**
     * Convert bo page to vo page.
     *
     * @param entityPageBO business object
     * @return converted value
     */
    default Page<AttachmentVO> buildVOPageByBOPage(Page<AttachmentBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

}
