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
import io.github.pnoker.common.entity.ext.PointExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.common.enums.RwFlagEnum;
import io.github.pnoker.common.manager.entity.bo.*;
import io.github.pnoker.common.manager.entity.model.PointDO;
import io.github.pnoker.common.manager.entity.vo.*;
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
 * Point Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface PointBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    @Mapping(target = "tenantId", ignore = true)
    PointBO buildBOByVO(PointVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     */
    List<PointBO> buildBOListByVOList(List<PointVO> entityVOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "pointExt", ignore = true)
    @Mapping(target = "pointTypeFlag", ignore = true)
    @Mapping(target = "rwFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    PointDO buildDOByBO(PointBO entityBO);

    @AfterMapping
    default void afterProcess(PointBO entityBO, @MappingTarget PointDO entityDO) {
        // Json Ext
        PointExt entityExt = entityBO.getPointExt();
        JsonExt ext = new JsonExt();
        if (Objects.nonNull(entityExt)) {
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.toJsonString(entityExt.getContent()));
        }
        entityDO.setPointExt(ext);

        // PointType Flag
        PointTypeFlagEnum pointTypeFlag = entityBO.getPointTypeFlag();
        Optional.ofNullable(pointTypeFlag).ifPresent(value -> entityDO.setPointTypeFlag(value.getIndex()));

        // Rw Flag
        RwFlagEnum rwFlag = entityBO.getRwFlag();
        Optional.ofNullable(rwFlag).ifPresent(value -> entityDO.setRwFlag(value.getIndex()));

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
    List<PointDO> buildDOListByBOList(List<PointBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    @Mapping(target = "pointExt", ignore = true)
    @Mapping(target = "pointTypeFlag", ignore = true)
    @Mapping(target = "rwFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    PointBO buildBOByDO(PointDO entityDO);

    @AfterMapping
    default void afterProcess(PointDO entityDO, @MappingTarget PointBO entityBO) {
        // Json Ext
        JsonExt entityExt = entityDO.getPointExt();
        if (Objects.nonNull(entityExt)) {
            PointExt ext = new PointExt();
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.parseObject(entityExt.getContent(), PointExt.Content.class));
            entityBO.setPointExt(ext);
        }

        // PointType Flag
        Byte pointTypeFlag = entityDO.getPointTypeFlag();
        entityBO.setPointTypeFlag(PointTypeFlagEnum.ofIndex(pointTypeFlag));

        // Rw Flag
        Byte rwFlag = entityDO.getRwFlag();
        entityBO.setRwFlag(RwFlagEnum.ofIndex(rwFlag));

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
    List<PointBO> buildBOListByDOList(List<PointDO> entityDOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    PointVO buildVOByBO(PointBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     */
    List<PointVO> buildVOListByBOList(List<PointBO> entityBOList);

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
    Page<PointBO> buildBOPageByDOPage(Page<PointDO> entityPageDO);

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
    Page<PointVO> buildVOPageByBOPage(Page<PointBO> entityPageBO);

    List<PointDataVolumeRunVO> buildVOPointDataByBO(List<PointDataVolumeRunBO> list);

    PointConfigByDeviceVO buildVODeviceByBO(PointConfigByDeviceBO pointConfigByDeviceBO);

    List<DeviceDataVolumeRunVO> buildVODeviceDataByBO(List<DeviceDataVolumeRunBO> list);

    PointDataStatisticsByDriverIdVO buildVOPointDataDriverByBO(PointDataStatisticsByDriverIdBO pointDataStatisticsByDriverIdBOList);
}