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
import io.github.pnoker.common.auth.entity.bo.RolePrincipalBindBO;
import io.github.pnoker.common.auth.entity.model.RolePrincipalBindDO;
import io.github.pnoker.common.auth.entity.vo.RolePrincipalBindVO;
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
 * MapStruct builder for role-principal bindings.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface RolePrincipalBindBuilder {

    RolePrincipalBindBO buildBOByVO(RolePrincipalBindVO entityVO);

    RolePrincipalBindVO buildVOByBO(RolePrincipalBindBO entityBO);

    @Mapping(target = "principalType", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    RolePrincipalBindDO buildDOByBO(RolePrincipalBindBO entityBO);

    @AfterMapping
    default void afterProcess(RolePrincipalBindBO entityBO, @MappingTarget RolePrincipalBindDO entityDO) {
        if (Objects.nonNull(entityBO.getPrincipalType())) {
            entityDO.setPrincipalType(entityBO.getPrincipalType().getValue());
        }
    }

    @Mapping(target = "principalType", ignore = true)
    RolePrincipalBindBO buildBOByDO(RolePrincipalBindDO entityDO);

    @AfterMapping
    default void afterProcess(RolePrincipalBindDO entityDO, @MappingTarget RolePrincipalBindBO entityBO) {
        entityBO.setPrincipalType(PrincipalTypeEnum.ofValue(entityDO.getPrincipalType()));
    }

    List<RolePrincipalBindBO> buildBOListByDOList(List<RolePrincipalBindDO> entityDOList);

    default Page<RolePrincipalBindBO> buildBOPageByDOPage(Page<RolePrincipalBindDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    default Page<RolePrincipalBindVO> buildVOPageByBOPage(Page<RolePrincipalBindBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

}
