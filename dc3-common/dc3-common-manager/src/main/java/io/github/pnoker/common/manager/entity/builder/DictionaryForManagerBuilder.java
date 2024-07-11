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
import io.github.pnoker.common.entity.bo.DictionaryBO;
import io.github.pnoker.common.entity.builder.DictionaryBuilder;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Dictionary For Manager Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface DictionaryForManagerBuilder extends DictionaryBuilder {

    // 驱动相关

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    default DictionaryBO buildVOByDriverBO(DriverBO entityBO) {
        return DictionaryBO.builder().label(entityBO.getDriverName()).value(entityBO.getId().toString()).build();
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
    Page<DictionaryBO> buildVOPageByDriverBOPage(Page<DriverBO> entityPageBO);

    // 模版相关

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    default DictionaryBO buildVOByProfileBO(ProfileBO entityBO) {
        return DictionaryBO.builder().label(entityBO.getProfileName()).value(entityBO.getId().toString()).build();
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
    Page<DictionaryBO> buildVOPageByProfileBOPage(Page<ProfileBO> entityPageBO);

    // 位号相关

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    default DictionaryBO buildVOByPointBO(PointBO entityBO) {
        return DictionaryBO.builder().label(entityBO.getPointName()).value(entityBO.getId().toString()).build();
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
    Page<DictionaryBO> buildVOPageByPointBOPage(Page<PointBO> entityPageBO);

    // 设备相关

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    default DictionaryBO buildVOByDeviceBO(DeviceBO entityBO) {
        return DictionaryBO.builder().label(entityBO.getDeviceName()).value(entityBO.getId().toString()).build();
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
    Page<DictionaryBO> buildVOPageByDeviceBOPage(Page<DeviceBO> entityPageBO);
}