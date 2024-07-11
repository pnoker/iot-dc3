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
import io.github.pnoker.common.auth.entity.bo.ApiBO;
import io.github.pnoker.common.auth.entity.model.ApiDO;
import io.github.pnoker.common.auth.entity.vo.ApiVO;
import io.github.pnoker.common.entity.ext.ApiExt;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.ApiTypeFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
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
 * Api Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface ApiBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    @Mapping(target = "tenantId", ignore = true)
    ApiBO buildBOByVO(ApiVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     */
    List<ApiBO> buildBOListByVOList(List<ApiVO> entityVOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "apiExt", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "apiTypeFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    ApiDO buildDOByBO(ApiBO entityBO);

    @AfterMapping
    default void afterProcess(ApiBO entityBO, @MappingTarget ApiDO entityDO) {
        // Json Ext
        ApiExt entityExt = entityBO.getApiExt();
        JsonExt ext = new JsonExt();
        if (Objects.nonNull(entityExt)) {
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.toJsonString(entityExt.getContent()));
        }
        entityDO.setApiExt(ext);

        // ApiType Flag
        ApiTypeFlagEnum apiTypeFlag = entityBO.getApiTypeFlag();
        entityDO.setApiTypeFlag(apiTypeFlag.getIndex());
        Optional.ofNullable(apiTypeFlag).ifPresent(value -> entityDO.setApiTypeFlag(value.getIndex()));

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
    List<ApiDO> buildDOListByBOList(List<ApiBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    @Mapping(target = "apiExt", ignore = true)
    @Mapping(target = "apiTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    ApiBO buildBOByDO(ApiDO entityDO);

    @AfterMapping
    default void afterProcess(ApiDO entityDO, @MappingTarget ApiBO entityBO) {
        // Json Ext
        JsonExt entityExt = entityDO.getApiExt();
        if (Objects.nonNull(entityExt)) {
            ApiExt ext = new ApiExt();
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.parseObject(entityExt.getContent(), ApiExt.Content.class));
            entityBO.setApiExt(ext);
        }

        // ApiType Flag
        Byte apiTypeFlag = entityDO.getApiTypeFlag();
        entityBO.setApiTypeFlag(ApiTypeFlagEnum.ofIndex(apiTypeFlag));

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
    List<ApiBO> buildBOListByDOList(List<ApiDO> entityDOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    ApiVO buildVOByBO(ApiBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     */
    List<ApiVO> buildVOListByBOList(List<ApiBO> entityBOList);

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
    Page<ApiBO> buildBOPageByDOPage(Page<ApiDO> entityPageDO);

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
    Page<ApiVO> buildVOPageByBOPage(Page<ApiBO> entityPageBO);
}