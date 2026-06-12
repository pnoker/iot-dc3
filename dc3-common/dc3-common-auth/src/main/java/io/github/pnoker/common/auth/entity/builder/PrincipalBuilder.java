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

package io.github.pnoker.common.auth.entity.builder;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.entity.bo.PrincipalBO;
import io.github.pnoker.common.auth.entity.model.PrincipalDO;
import io.github.pnoker.common.auth.entity.vo.PrincipalVO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PrincipalSourceTypeEnum;
import io.github.pnoker.common.enums.PrincipalTypeEnum;
import io.github.pnoker.common.utils.MapStructUtil;
import io.github.pnoker.common.utils.PageUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Objects;

/**
 * MapStruct builder for principals.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface PrincipalBuilder {

    PrincipalBO buildBOByVO(PrincipalVO entityVO);

    PrincipalVO buildVOByBO(PrincipalBO entityBO);

    @Mapping(target = "principalType", ignore = true)
    @Mapping(target = "sourceType", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "lockedFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    PrincipalDO buildDOByBO(PrincipalBO entityBO);

    @AfterMapping
    default void afterProcess(PrincipalBO entityBO, @MappingTarget PrincipalDO entityDO) {
        if (Objects.nonNull(entityBO.getPrincipalType())) {
            entityDO.setPrincipalType(entityBO.getPrincipalType().getValue());
        }
        if (Objects.nonNull(entityBO.getSourceType())) {
            entityDO.setSourceType(entityBO.getSourceType().getValue());
        }
        if (Objects.nonNull(entityBO.getEnableFlag())) {
            entityDO.setEnableFlag(entityBO.getEnableFlag().getIndex());
        }
        if (Objects.nonNull(entityBO.getLockedFlag())) {
            entityDO.setLockedFlag(entityBO.getLockedFlag().getIndex());
        }
    }

    @Mapping(target = "principalType", ignore = true)
    @Mapping(target = "sourceType", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "lockedFlag", ignore = true)
    PrincipalBO buildBOByDO(PrincipalDO entityDO);

    @AfterMapping
    default void afterProcess(PrincipalDO entityDO, @MappingTarget PrincipalBO entityBO) {
        entityBO.setPrincipalType(PrincipalTypeEnum.ofValue(entityDO.getPrincipalType()));
        entityBO.setSourceType(PrincipalSourceTypeEnum.ofValue(entityDO.getSourceType()));
        entityBO.setEnableFlag(EnableFlagEnum.ofIndex(entityDO.getEnableFlag()));
        entityBO.setLockedFlag(EnableFlagEnum.ofIndex(entityDO.getLockedFlag()));
    }

    List<PrincipalBO> buildBOListByDOList(List<PrincipalDO> entityDOList);

    default Page<PrincipalBO> buildBOPageByDOPage(Page<PrincipalDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    default Page<PrincipalVO> buildVOPageByBOPage(Page<PrincipalBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

}
