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
import io.github.pnoker.common.entity.ext.DeviceExt;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.DeviceByPointBO;
import io.github.pnoker.common.manager.entity.model.DeviceDO;
import io.github.pnoker.common.manager.entity.vo.DeviceByPointVO;
import io.github.pnoker.common.manager.entity.vo.DeviceVO;
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
 * 设备 Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface DeviceBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    @Mapping(target = "tenantId", ignore = true)
    DeviceBO buildBOByVO(DeviceVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     */
    List<DeviceBO> buildBOListByVOList(List<DeviceVO> entityVOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "deviceExt", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    DeviceDO buildDOByBO(DeviceBO entityBO);

    @AfterMapping
    default void afterProcess(DeviceBO entityBO, @MappingTarget DeviceDO entityDO) {
        // Json Ext
        DeviceExt entityExt = entityBO.getDeviceExt();
        JsonExt ext = new JsonExt();
        if (Objects.nonNull(entityExt)) {
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.toJsonString(entityExt.getContent()));
        }
        entityDO.setDeviceExt(ext);

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
    List<DeviceDO> buildDOListByBOList(List<DeviceBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    @Mapping(target = "deviceExt", ignore = true)
    @Mapping(target = "profileIds", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    DeviceBO buildBOByDO(DeviceDO entityDO);

    @AfterMapping
    default void afterProcess(DeviceDO entityDO, @MappingTarget DeviceBO entityBO) {
        // Json Ext
        JsonExt entityExt = entityDO.getDeviceExt();
        if (Objects.nonNull(entityExt)) {
            DeviceExt ext = new DeviceExt();
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.parseObject(entityExt.getContent(), DeviceExt.Content.class));
            entityBO.setDeviceExt(ext);
        }

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
    List<DeviceBO> buildBOListByDOList(List<DeviceDO> entityDOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    DeviceVO buildVOByBO(DeviceBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     */
    List<DeviceVO> buildVOListByBOList(List<DeviceBO> entityBOList);

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
    Page<DeviceBO> buildBOPageByDOPage(Page<DeviceDO> entityPageDO);

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
    Page<DeviceVO> buildVOPageByBOPage(Page<DeviceBO> entityPageBO);


    DeviceByPointVO buildVOPointByBO(DeviceByPointBO deviceByPointBO);
}