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

package io.github.pnoker.center.auth.entity.builder;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.auth.entity.bo.TenantBO;
import io.github.pnoker.center.auth.entity.model.TenantDO;
import io.github.pnoker.center.auth.entity.vo.TenantVO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.entity.ext.TenantExt;
import io.github.pnoker.common.utils.JsonUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * Tenant Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring")
public interface TenantBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    TenantBO buildBOByVO(TenantVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     */
    List<TenantBO> buildBOListByVOList(List<TenantVO> entityVOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "tenantExt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    TenantDO buildDOByBO(TenantBO entityBO);

    @AfterMapping
    default void afterProcess(TenantBO entityBO, @MappingTarget TenantDO entityDO) {
        TenantExt entityExt = entityBO.getTenantExt();
        if (ObjectUtil.isNotNull(entityExt)) {
            JsonExt.JsonExtBuilder<?, ?> builder = JsonExt.builder();
            builder.type(entityExt.getType()).version(entityExt.getVersion()).remark(entityExt.getRemark());
            builder.content(JsonUtil.toJsonString(entityExt.getContent()));
            entityDO.setTenantExt(builder.build());
        }
    }

    /**
     * BOList to DOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityDO Array
     */
    List<TenantDO> buildDOListByBOList(List<TenantBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    @Mapping(target = "tenantExt", ignore = true)
    TenantBO buildBOByDO(TenantDO entityDO);

    @AfterMapping
    default void afterProcess(TenantDO entityDO, @MappingTarget TenantBO entityBO) {
        JsonExt entityExt = entityDO.getTenantExt();
        if (ObjectUtil.isNotNull(entityExt)) {
            TenantExt.TenantExtBuilder<?, ?> builder = TenantExt.builder();
            builder.type(entityExt.getType()).version(entityExt.getVersion()).remark(entityExt.getRemark());
            builder.content(JsonUtil.parseObject(entityExt.getContent(), TenantExt.Content.class));
            entityBO.setTenantExt(builder.build());
        }
    }

    /**
     * DOList to BOList
     *
     * @param entityDOList EntityDO Array
     * @return EntityBO Array
     */
    List<TenantBO> buildBOListByDOList(List<TenantDO> entityDOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    TenantVO buildVOByBO(TenantBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     */
    List<TenantVO> buildVOListByBOList(List<TenantBO> entityBOList);

    /**
     * DOPage to BOPage
     *
     * @param entityPageDO EntityDO Page
     * @return EntityBO Page
     */
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<TenantBO> buildBOPageByDOPage(Page<TenantDO> entityPageDO);

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<TenantVO> buildVOPageByBOPage(Page<TenantBO> entityPageBO);
}