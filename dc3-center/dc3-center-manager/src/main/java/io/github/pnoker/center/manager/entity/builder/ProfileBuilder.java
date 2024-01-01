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

package io.github.pnoker.center.manager.entity.builder;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.bo.ProfileBO;
import io.github.pnoker.center.manager.entity.model.ProfileDO;
import io.github.pnoker.center.manager.entity.vo.ProfileVO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.entity.ext.ProfileExt;
import io.github.pnoker.common.utils.JsonUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * Profile Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring")
public interface ProfileBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    @Mapping(target = "tenantId", ignore = true)
    ProfileBO buildBOByVO(ProfileVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     */
    List<ProfileBO> buildBOListByVOList(List<ProfileVO> entityVOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "profileExt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    ProfileDO buildDOByBO(ProfileBO entityBO);

    @AfterMapping
    default void afterProcess(ProfileBO entityBO, @MappingTarget ProfileDO entityDO) {
        ProfileExt entityExt = entityBO.getProfileExt();
        if (ObjectUtil.isNotNull(entityExt)) {
            JsonExt.JsonExtBuilder<?, ?> builder = JsonExt.builder();
            builder.type(entityExt.getType()).version(entityExt.getVersion()).remark(entityExt.getRemark());
            builder.content(JsonUtil.toJsonString(entityExt.getContent()));
            entityDO.setProfileExt(builder.build());
        }
    }

    /**
     * BOList to DOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityDO Array
     */
    List<ProfileDO> buildDOListByBOList(List<ProfileBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    @Mapping(target = "profileExt", ignore = true)
    ProfileBO buildBOByDO(ProfileDO entityDO);

    @AfterMapping
    default void afterProcess(ProfileDO entityDO, @MappingTarget ProfileBO entityBO) {
        JsonExt entityExt = entityDO.getProfileExt();
        if (ObjectUtil.isNotNull(entityExt)) {
            ProfileExt.ProfileExtBuilder<?, ?> builder = ProfileExt.builder();
            builder.type(entityExt.getType()).version(entityExt.getVersion()).remark(entityExt.getRemark());
            builder.content(JsonUtil.parseObject(entityExt.getContent(), ProfileExt.Content.class));
            entityBO.setProfileExt(builder.build());
        }
    }

    /**
     * DOList to BOList
     *
     * @param entityDOList EntityDO Array
     * @return EntityBO Array
     */
    List<ProfileBO> buildBOListByDOList(List<ProfileDO> entityDOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    ProfileVO buildVOByBO(ProfileBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     */
    List<ProfileVO> buildVOListByBOList(List<ProfileBO> entityBOList);

    /**
     * DOPage to BOPage
     *
     * @param entityPageDO EntityDO Page
     * @return EntityBO Page
     */
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<ProfileBO> buildBOPageByDOPage(Page<ProfileDO> entityPageDO);

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<ProfileVO> buildVOPageByBOPage(Page<ProfileBO> entityPageBO);
}