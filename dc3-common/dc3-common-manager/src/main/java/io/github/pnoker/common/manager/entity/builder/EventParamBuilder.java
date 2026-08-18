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
import io.github.pnoker.common.entity.ext.EventParamExt;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.manager.entity.bo.EventParamBO;
import io.github.pnoker.common.manager.entity.model.EventParamDO;
import io.github.pnoker.common.manager.entity.vo.EventParamVO;
import io.github.pnoker.common.utils.CodeUtil;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.MapStructUtil;
import io.github.pnoker.common.utils.PageUtil;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * MapStruct builder converting between event param BO, VO, and DO.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface EventParamBuilder {

    /**
     * Convert vo to bo.
     *
     * @param entityVO view object
     * @return converted value
     */
    @Mapping(target = "tenantId", ignore = true)
    EventParamBO buildBOByVO(EventParamVO entityVO);

    /**
     * Convert vo list to bo list.
     *
     * @param entityVOList entity view object list
     * @return converted value
     */
    List<EventParamBO> buildBOListByVOList(List<EventParamVO> entityVOList);

    /**
     * Convert bo to do.
     *
     * @param entityBO business object
     * @return converted value
     */
    @Mapping(target = "paramExt", ignore = true)
    @Mapping(target = "paramTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    EventParamDO buildDOByBO(EventParamBO entityBO);

    /**
     * After build persistence object.
     *
     * @param entityBO business object
     * @param entityDO persistence object
     */
    @AfterMapping
    default void afterBuildDO(EventParamBO entityBO, @MappingTarget EventParamDO entityDO) {
        if (StringUtils.isEmpty(entityBO.getParamCode())) {
            entityDO.setParamCode(CodeUtil.getCode());
        }

        EventParamExt entityExt = entityBO.getParamExt();
        JsonExt ext = new JsonExt();
        if (Objects.nonNull(entityExt)) {
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.toJsonString(entityExt.getContent()));
        }
        entityDO.setParamExt(ext);

        Optional.ofNullable(entityBO.getParamTypeFlag()).ifPresent(value -> entityDO.setParamTypeFlag(value.getIndex()));
        Optional.ofNullable(entityBO.getEnableFlag()).ifPresent(value -> entityDO.setEnableFlag(value.getIndex()));
    }

    /**
     * Convert bo list to do list.
     *
     * @param entityBOList entity business object list
     * @return converted value
     */
    List<EventParamDO> buildDOListByBOList(List<EventParamBO> entityBOList);

    /**
     * Convert do to bo.
     *
     * @param entityDO persistence object
     * @return converted value
     */
    @Mapping(target = "paramExt", ignore = true)
    @Mapping(target = "paramTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    EventParamBO buildBOByDO(EventParamDO entityDO);

    /**
     * After build business object.
     *
     * @param entityDO persistence object
     * @param entityBO business object
     */
    @AfterMapping
    default void afterBuildBO(EventParamDO entityDO, @MappingTarget EventParamBO entityBO) {
        JsonExt entityExt = entityDO.getParamExt();
        if (Objects.nonNull(entityExt)) {
            EventParamExt ext = new EventParamExt();
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.parseObject(entityExt.getContent(), EventParamExt.Content.class));
            entityBO.setParamExt(ext);
        }

        entityBO.setParamTypeFlag(PointTypeEnum.ofIndex(entityDO.getParamTypeFlag()));
        entityBO.setEnableFlag(EnableFlagEnum.ofIndex(entityDO.getEnableFlag()));
    }

    /**
     * Convert do list to bo list.
     *
     * @param entityDOList entity persistence object list
     * @return converted value
     */
    List<EventParamBO> buildBOListByDOList(List<EventParamDO> entityDOList);

    /**
     * Convert bo to vo.
     *
     * @param entityBO business object
     * @return converted value
     */
    EventParamVO buildVOByBO(EventParamBO entityBO);

    /**
     * Convert bo list to vo list.
     *
     * @param entityBOList entity business object list
     * @return converted value
     */
    List<EventParamVO> buildVOListByBOList(List<EventParamBO> entityBOList);

    /**
     * Convert do page to bo page.
     *
     * @param entityPageDO persistence object
     * @return converted value
     */
    default Page<EventParamBO> buildBOPageByDOPage(Page<EventParamDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    /**
     * Convert bo page to vo page.
     *
     * @param entityPageBO business object
     * @return converted value
     */
    default Page<EventParamVO> buildVOPageByBOPage(Page<EventParamBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

}
