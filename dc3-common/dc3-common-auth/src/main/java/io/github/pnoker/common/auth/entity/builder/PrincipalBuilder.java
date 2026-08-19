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
 * @since 2026.6.12
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface PrincipalBuilder {

    /**
     * Convert vo to bo.
     *
     * @param entityVO view object
     * @return converted value
     */
    PrincipalBO buildBOByVO(PrincipalVO entityVO);

    /**
     * Convert bo to vo.
     *
     * @param entityBO business object
     * @return converted value
     */
    PrincipalVO buildVOByBO(PrincipalBO entityBO);

    /**
     * Convert bo to do.
     *
     * @param entityBO business object
     * @return converted value
     */
    @Mapping(target = "principalType", ignore = true)
    @Mapping(target = "sourceType", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "lockedFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    PrincipalDO buildDOByBO(PrincipalBO entityBO);

    /**
     * After process.
     *
     * @param entityBO business object
     * @param entityDO persistence object
     */
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

    /**
     * Convert do to bo.
     *
     * @param entityDO persistence object
     * @return converted value
     */
    @Mapping(target = "principalType", ignore = true)
    @Mapping(target = "sourceType", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "lockedFlag", ignore = true)
    PrincipalBO buildBOByDO(PrincipalDO entityDO);

    /**
     * After process.
     *
     * @param entityDO persistence object
     * @param entityBO business object
     */
    @AfterMapping
    default void afterProcess(PrincipalDO entityDO, @MappingTarget PrincipalBO entityBO) {
        entityBO.setPrincipalType(PrincipalTypeEnum.ofValue(entityDO.getPrincipalType()));
        entityBO.setSourceType(PrincipalSourceTypeEnum.ofValue(entityDO.getSourceType()));
        entityBO.setEnableFlag(EnableFlagEnum.ofIndex(entityDO.getEnableFlag()));
        entityBO.setLockedFlag(EnableFlagEnum.ofIndex(entityDO.getLockedFlag()));
    }

    /**
     * Convert do list to bo list.
     *
     * @param entityDOList entity persistence object list
     * @return converted value
     */
    List<PrincipalBO> buildBOListByDOList(List<PrincipalDO> entityDOList);

    /**
     * Convert bo list to vo list.
     *
     * @param entityBOList entity business object list
     * @return converted value
     */
    List<PrincipalVO> buildVOListByBOList(List<PrincipalBO> entityBOList);

    /**
     * Convert do page to bo page.
     *
     * @param entityPageDO persistence object
     * @return converted value
     */
    default Page<PrincipalBO> buildBOPageByDOPage(Page<PrincipalDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    /**
     * Convert bo page to vo page.
     *
     * @param entityPageBO business object
     * @return converted value
     */
    default Page<PrincipalVO> buildVOPageByBOPage(Page<PrincipalBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

}
