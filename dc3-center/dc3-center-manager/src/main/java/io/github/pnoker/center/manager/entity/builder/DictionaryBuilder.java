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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.bo.*;
import io.github.pnoker.center.manager.entity.vo.DictionaryVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Dictionary Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring")
public interface DictionaryBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    DictionaryBO buildBOByVO(DictionaryVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     */
    List<DictionaryBO> buildBOListByVOList(List<DictionaryVO> entityVOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    DictionaryVO buildVOByBO(DictionaryBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     */
    List<DictionaryVO> buildVOListByBOList(List<DictionaryBO> entityBOList);

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<DictionaryVO> buildVOPageByBOPage(Page<DictionaryBO> entityPageBO);


    // 驱动相关

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    default DictionaryBO buildVOByDriverBO(DriverBO entityBO) {
        return DictionaryBO.builder().label(entityBO.getDriverName()).value(entityBO.getId()).build();
    }

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<DictionaryBO> buildVOPageByDriverBOPage(Page<DriverBO> entityPageBO);

    // 模板相关

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    default DictionaryBO buildVOByProfileBO(ProfileBO entityBO) {
        return DictionaryBO.builder().label(entityBO.getProfileName()).value(entityBO.getId()).build();
    }

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
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
        return DictionaryBO.builder().label(entityBO.getPointName()).value(entityBO.getId()).build();
    }

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
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
        return DictionaryBO.builder().label(entityBO.getDeviceName()).value(entityBO.getId()).build();
    }

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<DictionaryBO> buildVOPageByDeviceBOPage(Page<DeviceBO> entityPageBO);
}