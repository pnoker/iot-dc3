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
import io.github.pnoker.common.auth.entity.bo.ResourceBO;
import io.github.pnoker.common.auth.entity.model.ResourceDO;
import io.github.pnoker.common.auth.entity.vo.ResourceVO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.entity.ext.ResourceExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ResourceScopeFlagEnum;
import io.github.pnoker.common.enums.ResourceTypeFlagEnum;
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
 * Resource Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface ResourceBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    @Mapping(target = "tenantId", ignore = true)
    ResourceBO buildBOByVO(ResourceVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     */
    List<ResourceBO> buildBOListByVOList(List<ResourceVO> entityVOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "resourceExt", ignore = true)
    @Mapping(target = "resourceTypeFlag", ignore = true)
    @Mapping(target = "resourceScopeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    ResourceDO buildDOByBO(ResourceBO entityBO);

    @AfterMapping
    default void afterProcess(ResourceBO entityBO, @MappingTarget ResourceDO entityDO) {
        // Json Ext
        ResourceExt entityExt = entityBO.getResourceExt();
        JsonExt ext = new JsonExt();
        if (Objects.nonNull(entityExt)) {
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.toJsonString(entityExt.getContent()));
        }
        entityDO.setResourceExt(ext);

        // ResourceType Flag
        ResourceTypeFlagEnum resourceTypeFlag = entityBO.getResourceTypeFlag();
        Optional.ofNullable(resourceTypeFlag).ifPresent(value -> entityDO.setResourceTypeFlag(value.getIndex()));

        // ResourceScope Flag
        ResourceScopeFlagEnum resourceScopeFlag = entityBO.getResourceScopeFlag();
        Optional.ofNullable(resourceScopeFlag).ifPresent(value -> entityDO.setResourceScopeFlag(value.getIndex()));

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
    List<ResourceDO> buildDOListByBOList(List<ResourceBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    @Mapping(target = "resourceExt", ignore = true)
    @Mapping(target = "resourceTypeFlag", ignore = true)
    @Mapping(target = "resourceScopeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    ResourceBO buildBOByDO(ResourceDO entityDO);

    @AfterMapping
    default void afterProcess(ResourceDO entityDO, @MappingTarget ResourceBO entityBO) {
        // Json Ext
        JsonExt entityExt = entityDO.getResourceExt();
        if (Objects.nonNull(entityExt)) {
            ResourceExt ext = new ResourceExt();
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.parseObject(entityExt.getContent(), ResourceExt.Content.class));
            entityBO.setResourceExt(ext);
        }

        // ResourceType Flag
        Byte resourceTypeFlag = entityDO.getResourceTypeFlag();
        entityBO.setResourceTypeFlag(ResourceTypeFlagEnum.ofIndex(resourceTypeFlag));

        // ResourceScope Flag
        Byte resourceScopeFlag = entityDO.getResourceScopeFlag();
        entityBO.setResourceScopeFlag(ResourceScopeFlagEnum.ofIndex(resourceScopeFlag));

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
    List<ResourceBO> buildBOListByDOList(List<ResourceDO> entityDOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    ResourceVO buildVOByBO(ResourceBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     */
    List<ResourceVO> buildVOListByBOList(List<ResourceBO> entityBOList);

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
    Page<ResourceBO> buildBOPageByDOPage(Page<ResourceDO> entityPageDO);

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
    Page<ResourceVO> buildVOPageByBOPage(Page<ResourceBO> entityPageBO);
}