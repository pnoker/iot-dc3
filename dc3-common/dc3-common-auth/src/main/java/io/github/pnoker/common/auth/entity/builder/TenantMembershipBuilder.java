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
 * @since 2026.6.13
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface TenantMembershipBuilder {

    /**
     * Convert vo to bo.
     *
     * @param entityVO view object
     * @return converted value
     */
    TenantMembershipBO buildBOByVO(TenantMembershipVO entityVO);

    /**
     * Convert bo to vo.
     *
     * @param entityBO business object
     * @return converted value
     */
    TenantMembershipVO buildVOByBO(TenantMembershipBO entityBO);

    /**
     * Convert bo to do.
     *
     * @param entityBO business object
     * @return converted value
     */
    @Mapping(target = "principalType", ignore = true)
    @Mapping(target = "membershipStatus", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    TenantMembershipDO buildDOByBO(TenantMembershipBO entityBO);

    /**
     * After process.
     *
     * @param entityBO business object
     * @param entityDO persistence object
     */
    @AfterMapping
    default void afterProcess(TenantMembershipBO entityBO, @MappingTarget TenantMembershipDO entityDO) {
        if (Objects.nonNull(entityBO.getPrincipalType())) {
            entityDO.setPrincipalType(entityBO.getPrincipalType().getValue());
        }
        if (Objects.nonNull(entityBO.getMembershipStatus())) {
            entityDO.setMembershipStatus(entityBO.getMembershipStatus().getValue());
        }
    }

    /**
     * Convert do to bo.
     *
     * @param entityDO persistence object
     * @return converted value
     */
    @Mapping(target = "principalType", ignore = true)
    @Mapping(target = "membershipStatus", ignore = true)
    TenantMembershipBO buildBOByDO(TenantMembershipDO entityDO);

    /**
     * After process.
     *
     * @param entityDO persistence object
     * @param entityBO business object
     */
    @AfterMapping
    default void afterProcess(TenantMembershipDO entityDO, @MappingTarget TenantMembershipBO entityBO) {
        entityBO.setPrincipalType(PrincipalTypeEnum.ofValue(entityDO.getPrincipalType()));
        entityBO.setMembershipStatus(MembershipStatusEnum.ofValue(entityDO.getMembershipStatus()));
    }

    /**
     * Convert do page to bo page.
     *
     * @param entityPageDO persistence object
     * @return converted value
     */
    default Page<TenantMembershipBO> buildBOPageByDOPage(Page<TenantMembershipDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    /**
     * Convert bo page to vo page.
     *
     * @param entityPageBO business object
     * @return converted value
     */
    default Page<TenantMembershipVO> buildVOPageByBOPage(Page<TenantMembershipBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }
}
