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
import io.github.pnoker.center.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.center.manager.entity.model.DriverAttributeDO;
import io.github.pnoker.center.manager.entity.vo.DriverAttributeVO;
import io.github.pnoker.common.entity.dto.DriverAttributeDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * DriverAttribute Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring")
public interface DriverAttributeBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    @Mapping(target = "tenantId", ignore = true)
    DriverAttributeBO buildBOByVO(DriverAttributeVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     */
    List<DriverAttributeBO> buildBOListByVOList(List<DriverAttributeVO> entityVOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "attributeExt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    DriverAttributeDO buildDOByBO(DriverAttributeBO entityBO);

    /**
     * BOList to DOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityDO Array
     */
    List<DriverAttributeDO> buildDOListByBOList(List<DriverAttributeBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    DriverAttributeBO buildBOByDO(DriverAttributeDO entityDO);

    /**
     * DOList to BOList
     *
     * @param entityDOList EntityDO Array
     * @return EntityBO Array
     */
    List<DriverAttributeBO> buildBOListByDOList(List<DriverAttributeDO> entityDOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    DriverAttributeVO buildVOByBO(DriverAttributeBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     */
    List<DriverAttributeVO> buildVOListByBOList(List<DriverAttributeBO> entityBOList);

    /**
     * BO to DTO
     *
     * @param entityBO EntityBO
     * @return EntityDTO
     */
    DriverAttributeDTO buildDTOByBO(DriverAttributeBO entityBO);

    /**
     * BOList to DTOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityDTO Array
     */
    List<DriverAttributeDTO> buildDTOListByBOList(List<DriverAttributeBO> entityBOList);

    /**
     * DTO to BO
     *
     * @param entityDTO EntityDTO
     * @return EntityBO
     */
    @Mapping(target = "attributeExt", ignore = true)
    DriverAttributeBO buildBOByDTO(DriverAttributeDTO entityDTO);

    /**
     * DOPage to BOPage
     *
     * @param entityPageDO EntityDO Page
     * @return EntityBO Page
     */
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<DriverAttributeBO> buildBOPageByDOPage(Page<DriverAttributeDO> entityPageDO);

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<DriverAttributeVO> buildVOPageByBOPage(Page<DriverAttributeBO> entityPageBO);
}