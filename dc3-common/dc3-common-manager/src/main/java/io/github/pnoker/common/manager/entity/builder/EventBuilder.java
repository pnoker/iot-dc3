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

import io.github.pnoker.common.entity.ext.EventExt;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EventLevelEnum;
import io.github.pnoker.common.enums.EventTypeFlagEnum;
import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.common.manager.entity.model.EventDO;
import io.github.pnoker.common.manager.entity.vo.EventVO;
import io.github.pnoker.common.utils.CodeUtil;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.MapStructUtil;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * MapStruct builder converting between event BO, VO, and DO.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface EventBuilder {

    /**
     * Convert vo to bo.
     *
     * @param entityVO view object
     * @return converted value
     */
    @Mapping(target = "tenantId", ignore = true)
    EventBO buildBOByVO(EventVO entityVO);

    /**
     * Convert vo list to bo list.
     *
     * @param entityVOList entity view object list
     * @return converted value
     */
    List<EventBO> buildBOListByVOList(List<EventVO> entityVOList);

    /**
     * Convert bo to do.
     *
     * @param entityBO business object
     * @return converted value
     */
    @Mapping(target = "eventExt", ignore = true)
    @Mapping(target = "eventTypeFlag", ignore = true)
    @Mapping(target = "eventLevelFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    EventDO buildDOByBO(EventBO entityBO);

    /**
     * After build persistence object.
     *
     * @param entityBO business object
     * @param entityDO persistence object
     */
    @AfterMapping
    default void afterBuildDO(EventBO entityBO, @MappingTarget EventDO entityDO) {
        if (StringUtils.isEmpty(entityBO.getEventCode())) {
            entityDO.setEventCode(CodeUtil.getCode());
        }

        EventExt entityExt = entityBO.getEventExt();
        JsonExt ext = new JsonExt();
        if (Objects.nonNull(entityExt)) {
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.toJsonString(entityExt.getContent()));
        }
        entityDO.setEventExt(ext);

        Optional.ofNullable(entityBO.getEventTypeFlag()).ifPresent(value -> entityDO.setEventTypeFlag(value.getIndex()));
        Optional.ofNullable(entityBO.getEventLevelFlag()).ifPresent(value -> entityDO.setEventLevelFlag(value.getIndex()));
        Optional.ofNullable(entityBO.getEnableFlag()).ifPresent(value -> entityDO.setEnableFlag(value.getIndex()));
    }

    /**
     * Convert bo list to do list.
     *
     * @param entityBOList entity business object list
     * @return converted value
     */
    List<EventDO> buildDOListByBOList(List<EventBO> entityBOList);

    /**
     * Convert do to bo.
     *
     * @param entityDO persistence object
     * @return converted value
     */
    @Mapping(target = "eventExt", ignore = true)
    @Mapping(target = "eventTypeFlag", ignore = true)
    @Mapping(target = "eventLevelFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    EventBO buildBOByDO(EventDO entityDO);

    /**
     * After build business object.
     *
     * @param entityDO persistence object
     * @param entityBO business object
     */
    @AfterMapping
    default void afterBuildBO(EventDO entityDO, @MappingTarget EventBO entityBO) {
        JsonExt entityExt = entityDO.getEventExt();
        if (Objects.nonNull(entityExt)) {
            EventExt ext = new EventExt();
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.parseObject(entityExt.getContent(), EventExt.Content.class));
            entityBO.setEventExt(ext);
        }

        entityBO.setEventTypeFlag(EventTypeFlagEnum.ofIndex(entityDO.getEventTypeFlag()));
        entityBO.setEventLevelFlag(EventLevelEnum.ofIndex(entityDO.getEventLevelFlag()));
        entityBO.setEnableFlag(EnableFlagEnum.ofIndex(entityDO.getEnableFlag()));
    }

    /**
     * Convert do list to bo list.
     *
     * @param entityDOList entity persistence object list
     * @return converted value
     */
    List<EventBO> buildBOListByDOList(List<EventDO> entityDOList);

    /**
     * Convert bo to vo.
     *
     * @param entityBO business object
     * @return converted value
     */
    EventVO buildVOByBO(EventBO entityBO);

    /**
     * Convert bo list to vo list.
     *
     * @param entityBOList entity business object list
     * @return converted value
     */
    List<EventVO> buildVOListByBOList(List<EventBO> entityBOList);


}
