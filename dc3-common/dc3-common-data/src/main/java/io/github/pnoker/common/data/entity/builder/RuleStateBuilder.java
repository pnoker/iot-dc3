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

package io.github.pnoker.common.data.entity.builder;

import io.github.pnoker.common.data.entity.bo.RuleStateBO;
import io.github.pnoker.common.data.entity.model.RuleStateDO;
import io.github.pnoker.common.data.entity.vo.RuleStateVO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.entity.ext.RuleStateExt;
import io.github.pnoker.common.enums.AlarmTargetTypeEnum;
import io.github.pnoker.common.enums.RuleStatusEnum;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * MapStruct builder converting between rule runtime state BO, VO, and DO.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface RuleStateBuilder {

    /**
     * Convert vo to bo.
     *
     * @param entityVO view object
     * @return converted value
     */
    @Mapping(target = "tenantId", ignore = true)
    RuleStateBO buildBOByVO(RuleStateVO entityVO);

    /**
     * Convert vo list to bo list.
     *
     * @param entityVOList entity view object list
     * @return converted value
     */
    List<RuleStateBO> buildBOListByVOList(List<RuleStateVO> entityVOList);

    /**
     * Convert bo to do.
     *
     * @param entityBO business object
     * @return converted value
     */
    @Mapping(target = "entityStateExt", ignore = true)
    @Mapping(target = "alarmTargetTypeFlag", ignore = true)
    @Mapping(target = "entityStateFlag", ignore = true)
    RuleStateDO buildDOByBO(RuleStateBO entityBO);

    /**
     * After process.
     *
     * @param entityBO business object
     * @param entityDO persistence object
     */
    @AfterMapping
    default void afterProcess(RuleStateBO entityBO, @MappingTarget RuleStateDO entityDO) {
        RuleStateExt entityExt = entityBO.getEntityStateExt();
        JsonExt ext = new JsonExt();
        if (Objects.nonNull(entityExt)) {
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.toJsonString(entityExt.getContent()));
        }
        entityDO.setEntityStateExt(ext);

        AlarmTargetTypeEnum alarmTargetTypeFlag = entityBO.getAlarmTargetTypeFlag();
        Optional.ofNullable(alarmTargetTypeFlag)
                .ifPresent(value -> entityDO.setAlarmTargetTypeFlag(value.getIndex()));

        RuleStatusEnum entityStateFlag = entityBO.getEntityStateFlag();
        Optional.ofNullable(entityStateFlag).ifPresent(value -> entityDO.setEntityStateFlag(value.getIndex()));
    }

    /**
     * Convert bo list to do list.
     *
     * @param entityBOList entity business object list
     * @return converted value
     */
    List<RuleStateDO> buildDOListByBOList(List<RuleStateBO> entityBOList);

    /**
     * Convert do to bo.
     *
     * @param entityDO persistence object
     * @return converted value
     */
    @Mapping(target = "entityStateExt", ignore = true)
    @Mapping(target = "alarmTargetTypeFlag", ignore = true)
    @Mapping(target = "entityStateFlag", ignore = true)
    RuleStateBO buildBOByDO(RuleStateDO entityDO);

    /**
     * After process.
     *
     * @param entityDO persistence object
     * @param entityBO business object
     */
    @AfterMapping
    default void afterProcess(RuleStateDO entityDO, @MappingTarget RuleStateBO entityBO) {
        JsonExt entityExt = entityDO.getEntityStateExt();
        if (Objects.nonNull(entityExt)) {
            RuleStateExt ext = new RuleStateExt();
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.parseObject(entityExt.getContent(), RuleStateExt.Content.class));
            entityBO.setEntityStateExt(ext);
        }

        Byte alarmTargetTypeFlag = entityDO.getAlarmTargetTypeFlag();
        entityBO.setAlarmTargetTypeFlag(AlarmTargetTypeEnum.ofIndex(alarmTargetTypeFlag));

        Byte entityStateFlag = entityDO.getEntityStateFlag();
        entityBO.setEntityStateFlag(RuleStatusEnum.ofIndex(entityStateFlag));
    }

    /**
     * Convert do list to bo list.
     *
     * @param entityDOList entity persistence object list
     * @return converted value
     */
    List<RuleStateBO> buildBOListByDOList(List<RuleStateDO> entityDOList);

    /**
     * Convert bo to vo.
     *
     * @param entityBO business object
     * @return converted value
     */
    RuleStateVO buildVOByBO(RuleStateBO entityBO);

    /**
     * Convert bo list to vo list.
     *
     * @param entityBOList entity business object list
     * @return converted value
     */
    List<RuleStateVO> buildVOListByBOList(List<RuleStateBO> entityBOList);

}
