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
import io.github.pnoker.center.manager.entity.bo.DriverAttributeConfigBO;
import io.github.pnoker.center.manager.entity.model.DriverAttributeConfigDO;
import io.github.pnoker.center.manager.entity.vo.DriverAttributeConfigVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * DriverAttributeConfig Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring")
public interface DriverAttributeConfigBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    DriverAttributeConfigBO buildBOByVO(DriverAttributeConfigVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     */
    List<DriverAttributeConfigBO> buildBOListByVOList(List<DriverAttributeConfigVO> entityVOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "deleted", ignore = true)
    DriverAttributeConfigDO buildDOByBO(DriverAttributeConfigBO entityBO);

    /**
     * BOList to DOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityDO Array
     */
    List<DriverAttributeConfigDO> buildDOListByBOList(List<DriverAttributeConfigBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    DriverAttributeConfigBO buildBOByDO(DriverAttributeConfigDO entityDO);

    /**
     * DOList to BOList
     *
     * @param entityDOList EntityDO Array
     * @return EntityBO Array
     */
    List<DriverAttributeConfigBO> buildBOListByDOList(List<DriverAttributeConfigDO> entityDOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    DriverAttributeConfigVO buildVOByBO(DriverAttributeConfigBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     */
    List<DriverAttributeConfigVO> buildVOListByBOList(List<DriverAttributeConfigBO> entityBOList);

    /**
     * DOPage to BOPage
     *
     * @param entityPageDO EntityDO Page
     * @return EntityBO Page
     */
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<DriverAttributeConfigBO> buildBOPageByDOPage(Page<DriverAttributeConfigDO> entityPageDO);

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<DriverAttributeConfigVO> buildVOPageByBOPage(Page<DriverAttributeConfigBO> entityPageBO);
}