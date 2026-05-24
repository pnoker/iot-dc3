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
import io.github.pnoker.common.manager.entity.bo.CommandAttributeConfigBO;
import io.github.pnoker.common.manager.entity.model.CommandAttributeConfigDO;
import io.github.pnoker.common.manager.entity.vo.CommandAttributeConfigVO;
import io.github.pnoker.common.utils.MapStructUtil;
import io.github.pnoker.common.utils.PageUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Optional;

/**
 * CommandAttributeConfig Builder
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface CommandAttributeConfigBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    @Mapping(target = "tenantId", ignore = true)
    CommandAttributeConfigBO buildBOByVO(CommandAttributeConfigVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     */
    List<CommandAttributeConfigBO> buildBOListByVOList(List<CommandAttributeConfigVO> entityVOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    CommandAttributeConfigDO buildDOByBO(CommandAttributeConfigBO entityBO);

    @AfterMapping
    default void afterProcess(CommandAttributeConfigBO entityBO, @MappingTarget CommandAttributeConfigDO entityDO) {
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
    List<CommandAttributeConfigDO> buildDOListByBOList(List<CommandAttributeConfigBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    @Mapping(target = "enableFlag", ignore = true)
    CommandAttributeConfigBO buildBOByDO(CommandAttributeConfigDO entityDO);

    @AfterMapping
    default void afterProcess(CommandAttributeConfigDO entityDO, @MappingTarget CommandAttributeConfigBO entityBO) {
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
    List<CommandAttributeConfigBO> buildBOListByDOList(List<CommandAttributeConfigDO> entityDOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    CommandAttributeConfigVO buildVOByBO(CommandAttributeConfigBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     */
    List<CommandAttributeConfigVO> buildVOListByBOList(List<CommandAttributeConfigBO> entityBOList);

    /**
     * DOPage to BOPage
     *
     * @param entityPageDO EntityDO Page
     * @return EntityBO Page
     */
    default Page<CommandAttributeConfigBO> buildBOPageByDOPage(Page<CommandAttributeConfigDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    default Page<CommandAttributeConfigVO> buildVOPageByBOPage(Page<CommandAttributeConfigBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

}
