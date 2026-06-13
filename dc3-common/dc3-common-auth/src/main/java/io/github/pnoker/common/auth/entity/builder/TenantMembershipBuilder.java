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
import io.github.pnoker.common.auth.entity.bo.TenantMembershipBO;
import io.github.pnoker.common.auth.entity.model.TenantMembershipDO;
import io.github.pnoker.common.auth.entity.vo.TenantMembershipVO;
import io.github.pnoker.common.enums.MembershipStatusEnum;
import io.github.pnoker.common.enums.PrincipalTypeEnum;
import io.github.pnoker.common.utils.MapStructUtil;
import io.github.pnoker.common.utils.PageUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Objects;

/**
 * MapStruct builder for tenant memberships.
 *
 * @author pnoker
 * @version 2026.6.13
 * @since 2026.6.13
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface TenantMembershipBuilder {

    TenantMembershipBO buildBOByVO(TenantMembershipVO entityVO);

    TenantMembershipVO buildVOByBO(TenantMembershipBO entityBO);

    @Mapping(target = "principalType", ignore = true)
    @Mapping(target = "membershipStatus", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    TenantMembershipDO buildDOByBO(TenantMembershipBO entityBO);

    @AfterMapping
    default void afterProcess(TenantMembershipBO entityBO, @MappingTarget TenantMembershipDO entityDO) {
        if (Objects.nonNull(entityBO.getPrincipalType())) {
            entityDO.setPrincipalType(entityBO.getPrincipalType().getValue());
        }
        if (Objects.nonNull(entityBO.getMembershipStatus())) {
            entityDO.setMembershipStatus(entityBO.getMembershipStatus().getValue());
        }
    }

    @Mapping(target = "principalType", ignore = true)
    @Mapping(target = "membershipStatus", ignore = true)
    TenantMembershipBO buildBOByDO(TenantMembershipDO entityDO);

    @AfterMapping
    default void afterProcess(TenantMembershipDO entityDO, @MappingTarget TenantMembershipBO entityBO) {
        entityBO.setPrincipalType(PrincipalTypeEnum.ofValue(entityDO.getPrincipalType()));
        entityBO.setMembershipStatus(MembershipStatusEnum.ofValue(entityDO.getMembershipStatus()));
    }

    default Page<TenantMembershipBO> buildBOPageByDOPage(Page<TenantMembershipDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    default Page<TenantMembershipVO> buildVOPageByBOPage(Page<TenantMembershipBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }
}
