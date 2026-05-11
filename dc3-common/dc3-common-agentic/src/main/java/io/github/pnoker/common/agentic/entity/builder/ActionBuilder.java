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
 * Agentic action builder.
 *
 * @author pnoker
 * @version 2026.5.11
 * @since 2026.5.11
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface ActionBuilder {

    ActionBO buildBOByVO(ActionVO entityVO);

    List<ActionBO> buildBOListByVOList(List<ActionVO> entityVOList);

    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    ActionDO buildDOByBO(ActionBO entityBO);

    @AfterMapping
    default void afterProcess(ActionBO entityBO, @MappingTarget ActionDO entityDO) {
        AgenticActionStatusEnum status = entityBO.getStatus();
        Optional.ofNullable(status).ifPresent(value -> entityDO.setStatus(value.getIndex()));
    }

    List<ActionDO> buildDOListByBOList(List<ActionBO> entityBOList);

    @Mapping(target = "status", ignore = true)
    ActionBO buildBOByDO(ActionDO entityDO);

    @AfterMapping
    default void afterProcess(ActionDO entityDO, @MappingTarget ActionBO entityBO) {
        Byte status = entityDO.getStatus();
        entityBO.setStatus(AgenticActionStatusEnum.ofIndex(status));
    }

    List<ActionBO> buildBOListByDOList(List<ActionDO> entityDOList);

    ActionVO buildVOByBO(ActionBO entityBO);

    List<ActionVO> buildVOListByBOList(List<ActionBO> entityBOList);

    default Page<ActionBO> buildBOPageByDOPage(Page<ActionDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    default Page<ActionVO> buildVOPageByBOPage(Page<ActionBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

}
