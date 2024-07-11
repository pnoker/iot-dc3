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

package io.github.pnoker.common.manager.entity.builder;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.entity.ext.ProfileExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ProfileShareFlagEnum;
import io.github.pnoker.common.enums.ProfileTypeFlagEnum;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.entity.model.ProfileDO;
import io.github.pnoker.common.manager.entity.vo.ProfileVO;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Profile Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
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
    @Mapping(target = "profileShareFlag", ignore = true)
    @Mapping(target = "profileTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    ProfileDO buildDOByBO(ProfileBO entityBO);

    @AfterMapping
    default void afterProcess(ProfileBO entityBO, @MappingTarget ProfileDO entityDO) {
        // Json Ext
        ProfileExt entityExt = entityBO.getProfileExt();
        JsonExt ext = new JsonExt();
        if (Objects.nonNull(entityExt)) {
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.toJsonString(entityExt.getContent()));
        }
        entityDO.setProfileExt(ext);

        // ProfileShare Flag
        ProfileShareFlagEnum profileShareFlag = entityBO.getProfileShareFlag();
        Optional.ofNullable(profileShareFlag).ifPresent(value -> entityDO.setProfileShareFlag(value.getIndex()));

        // ProfileType Flag
        ProfileTypeFlagEnum profileTypeFlag = entityBO.getProfileTypeFlag();
        Optional.ofNullable(profileTypeFlag).ifPresent(value -> entityDO.setProfileTypeFlag(value.getIndex()));

        // Enable Flag
        EnableFlagEnum enableFlag = entityBO.getEnableFlag();
        Optional.ofNullable(enableFlag).ifPresent(value -> entityDO.setEnableFlag(value.getIndex()));
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
    @Mapping(target = "profileShareFlag", ignore = true)
    @Mapping(target = "profileTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    ProfileBO buildBOByDO(ProfileDO entityDO);

    @AfterMapping
    default void afterProcess(ProfileDO entityDO, @MappingTarget ProfileBO entityBO) {
        // Json Ext
        JsonExt entityExt = entityDO.getProfileExt();
        if (Objects.nonNull(entityExt)) {
            ProfileExt ext = new ProfileExt();
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.parseObject(entityExt.getContent(), ProfileExt.Content.class));
            entityBO.setProfileExt(ext);
        }

        // ProfileShare Flag
        Byte profileShareFlag = entityDO.getProfileShareFlag();
        entityBO.setProfileShareFlag(ProfileShareFlagEnum.ofIndex(profileShareFlag));

        // ProfileType Flag
        Byte profileTypeFlag = entityDO.getProfileTypeFlag();
        entityBO.setProfileTypeFlag(ProfileTypeFlagEnum.ofIndex(profileTypeFlag));

        // Enable Flag
        Byte enableFlag = entityDO.getEnableFlag();
        entityBO.setEnableFlag(EnableFlagEnum.ofIndex(enableFlag));
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
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "countId", ignore = true)
    @Mapping(target = "maxLimit", ignore = true)
    @Mapping(target = "searchCount", ignore = true)
    @Mapping(target = "optimizeCountSql", ignore = true)
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<ProfileBO> buildBOPageByDOPage(Page<ProfileDO> entityPageDO);

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
    Page<ProfileVO> buildVOPageByBOPage(Page<ProfileBO> entityPageBO);
}