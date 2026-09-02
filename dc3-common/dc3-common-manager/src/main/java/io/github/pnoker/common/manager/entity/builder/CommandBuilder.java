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

import io.github.pnoker.common.entity.ext.CommandExt;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.CallTypeEnum;
import io.github.pnoker.common.enums.CommandTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.common.manager.entity.model.CommandDO;
import io.github.pnoker.common.manager.entity.vo.CommandVO;
import io.github.pnoker.common.utils.CodeUtil;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.MapStructUtil;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct builder converting between command BO, VO, and DO.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Mapper(
        componentModel = "spring",
        uses = {MapStructUtil.class})
public interface CommandBuilder {

    /**
     * Convert vo to bo.
     *
     * @param entityVO view object
     * @return converted value
     */
    @Mapping(target = "tenantId", ignore = true)
    CommandBO buildBOByVO(CommandVO entityVO);

    /**
     * Convert vo list to bo list.
     *
     * @param entityVOList entity view object list
     * @return converted value
     */
    List<CommandBO> buildBOListByVOList(List<CommandVO> entityVOList);

    /**
     * Convert bo to do.
     *
     * @param entityBO business object
     * @return converted value
     */
    @Mapping(target = "commandExt", ignore = true)
    @Mapping(target = "commandTypeFlag", ignore = true)
    @Mapping(target = "callTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    CommandDO buildDOByBO(CommandBO entityBO);

    /**
     * After build persistence object.
     *
     * @param entityBO business object
     * @param entityDO persistence object
     */
    @AfterMapping
    default void afterBuildDO(CommandBO entityBO, @MappingTarget CommandDO entityDO) {
        if (StringUtils.isEmpty(entityBO.getCommandCode())) {
            entityDO.setCommandCode(CodeUtil.getCode());
        }

        CommandExt entityExt = entityBO.getCommandExt();
        JsonExt ext = new JsonExt();
        if (Objects.nonNull(entityExt)) {
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.toJsonString(entityExt.getContent()));
        }
        entityDO.setCommandExt(ext);

        Optional.ofNullable(entityBO.getCommandTypeFlag())
                .ifPresent(value -> entityDO.setCommandTypeFlag(value.getIndex()));
        Optional.ofNullable(entityBO.getCallTypeFlag()).ifPresent(value -> entityDO.setCallTypeFlag(value.getIndex()));
        Optional.ofNullable(entityBO.getEnableFlag()).ifPresent(value -> entityDO.setEnableFlag(value.getIndex()));
    }

    /**
     * Convert bo list to do list.
     *
     * @param entityBOList entity business object list
     * @return converted value
     */
    List<CommandDO> buildDOListByBOList(List<CommandBO> entityBOList);

    /**
     * Convert do to bo.
     *
     * @param entityDO persistence object
     * @return converted value
     */
    @Mapping(target = "commandExt", ignore = true)
    @Mapping(target = "commandTypeFlag", ignore = true)
    @Mapping(target = "callTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    CommandBO buildBOByDO(CommandDO entityDO);

    /**
     * After build business object.
     *
     * @param entityDO persistence object
     * @param entityBO business object
     */
    @AfterMapping
    default void afterBuildBO(CommandDO entityDO, @MappingTarget CommandBO entityBO) {
        JsonExt entityExt = entityDO.getCommandExt();
        if (Objects.nonNull(entityExt)) {
            CommandExt ext = new CommandExt();
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.parseObject(entityExt.getContent(), CommandExt.Content.class));
            entityBO.setCommandExt(ext);
        }

        entityBO.setCommandTypeFlag(CommandTypeEnum.ofIndex(entityDO.getCommandTypeFlag()));
        entityBO.setCallTypeFlag(CallTypeEnum.ofIndex(entityDO.getCallTypeFlag()));
        entityBO.setEnableFlag(EnableFlagEnum.ofIndex(entityDO.getEnableFlag()));
    }

    /**
     * Convert do list to bo list.
     *
     * @param entityDOList entity persistence object list
     * @return converted value
     */
    List<CommandBO> buildBOListByDOList(List<CommandDO> entityDOList);

    /**
     * Convert bo to vo.
     *
     * @param entityBO business object
     * @return converted value
     */
    CommandVO buildVOByBO(CommandBO entityBO);

    /**
     * Convert bo list to vo list.
     *
     * @param entityBOList entity business object list
     * @return converted value
     */
    List<CommandVO> buildVOListByBOList(List<CommandBO> entityBOList);
}
