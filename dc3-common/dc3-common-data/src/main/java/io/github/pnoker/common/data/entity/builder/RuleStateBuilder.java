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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.data.entity.bo.RuleStateBO;
import io.github.pnoker.common.data.entity.model.RuleStateDO;
import io.github.pnoker.common.data.entity.vo.RuleStateVO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.entity.ext.RuleStateExt;
import io.github.pnoker.common.enums.AlarmTargetTypeFlagEnum;
import io.github.pnoker.common.enums.RuleStateFlagEnum;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.MapStructUtil;
import io.github.pnoker.common.utils.PageUtil;
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
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface RuleStateBuilder {

    @Mapping(target = "tenantId", ignore = true)
    RuleStateBO buildBOByVO(RuleStateVO entityVO);

    List<RuleStateBO> buildBOListByVOList(List<RuleStateVO> entityVOList);

    @Mapping(target = "entityStateExt", ignore = true)
    @Mapping(target = "alarmTargetTypeFlag", ignore = true)
    @Mapping(target = "entityStateFlag", ignore = true)
    RuleStateDO buildDOByBO(RuleStateBO entityBO);

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

        AlarmTargetTypeFlagEnum alarmTargetTypeFlag = entityBO.getAlarmTargetTypeFlag();
        Optional.ofNullable(alarmTargetTypeFlag)
                .ifPresent(value -> entityDO.setAlarmTargetTypeFlag(value.getIndex()));

        RuleStateFlagEnum entityStateFlag = entityBO.getEntityStateFlag();
        Optional.ofNullable(entityStateFlag).ifPresent(value -> entityDO.setEntityStateFlag(value.getIndex()));
    }

    List<RuleStateDO> buildDOListByBOList(List<RuleStateBO> entityBOList);

    @Mapping(target = "entityStateExt", ignore = true)
    @Mapping(target = "alarmTargetTypeFlag", ignore = true)
    @Mapping(target = "entityStateFlag", ignore = true)
    RuleStateBO buildBOByDO(RuleStateDO entityDO);

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
        entityBO.setAlarmTargetTypeFlag(AlarmTargetTypeFlagEnum.ofIndex(alarmTargetTypeFlag));

        Byte entityStateFlag = entityDO.getEntityStateFlag();
        entityBO.setEntityStateFlag(RuleStateFlagEnum.ofIndex(entityStateFlag));
    }

    List<RuleStateBO> buildBOListByDOList(List<RuleStateDO> entityDOList);

    RuleStateVO buildVOByBO(RuleStateBO entityBO);

    List<RuleStateVO> buildVOListByBOList(List<RuleStateBO> entityBOList);

    default Page<RuleStateBO> buildBOPageByDOPage(Page<RuleStateDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    default Page<RuleStateVO> buildVOPageByBOPage(Page<RuleStateBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

}
