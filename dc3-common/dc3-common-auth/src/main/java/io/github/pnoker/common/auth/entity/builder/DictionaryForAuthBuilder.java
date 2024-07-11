/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.auth.entity.builder;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.entity.bo.LimitedIpBO;
import io.github.pnoker.common.auth.entity.bo.TenantBO;
import io.github.pnoker.common.auth.entity.bo.UserLoginBO;
import io.github.pnoker.common.entity.bo.DictionaryBO;
import io.github.pnoker.common.entity.builder.DictionaryBuilder;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Dictionary For Auth Builder
 *
 * @author pnoker
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

    // 租户相关

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    default DictionaryBO buildVOByLimitedIpBO(LimitedIpBO entityBO) {
        return DictionaryBO.builder().label(entityBO.getIp()).value(entityBO.getId().toString()).build();
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
    Page<DictionaryBO> buildVOPageByLimitedIpBOPage(Page<LimitedIpBO> entityPageBO);

}