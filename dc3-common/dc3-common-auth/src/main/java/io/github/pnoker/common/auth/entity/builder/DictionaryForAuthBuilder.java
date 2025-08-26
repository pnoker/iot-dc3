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
import io.github.pnoker.common.auth.entity.bo.TenantBO;
import io.github.pnoker.common.auth.entity.bo.UserLoginBO;
import io.github.pnoker.common.dal.entity.bo.DictionaryBO;
import io.github.pnoker.common.dal.entity.builder.DictionaryBuilder;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Dictionary For Auth Builder
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface DictionaryForAuthBuilder extends DictionaryBuilder {

    // 租户相关

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    default DictionaryBO buildVOByTenantBO(TenantBO entityBO) {
        return DictionaryBO.builder().label(entityBO.getTenantName()).value(entityBO.getId().toString()).build();
    }

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "countId", ignore = true)
    @Mapping(target = "maxLimit", ignore = true)
    @Mapping(target = "searchCount", ignore = true)
    @Mapping(target = "optimizeCountSql", ignore = true)
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<DictionaryBO> buildVOPageByTenantBOPage(Page<TenantBO> entityPageBO);

    // 用户登录相关

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    default DictionaryBO buildVOByUserLoginBO(UserLoginBO entityBO) {
        return DictionaryBO.builder().label(entityBO.getLoginName()).value(entityBO.getId().toString()).build();
    }

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "countId", ignore = true)
    @Mapping(target = "maxLimit", ignore = true)
    @Mapping(target = "searchCount", ignore = true)
    @Mapping(target = "optimizeCountSql", ignore = true)
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<DictionaryBO> buildVOPageByUserLoginBOPage(Page<UserLoginBO> entityPageBO);

}