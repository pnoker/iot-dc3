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
import io.github.pnoker.center.manager.entity.bo.DriverBO;
import io.github.pnoker.center.manager.entity.model.DriverDO;
import io.github.pnoker.center.manager.entity.vo.DriverVO;
import io.github.pnoker.common.entity.dto.DriverDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Driver Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring")
public interface DriverBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    DriverBO buildBOByVO(DriverVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     */
    List<DriverBO> buildBOListByVOList(List<DriverVO> entityVOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "driverExt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    DriverDO buildDOByBO(DriverBO entityBO);

    /**
     * BOList to DOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityDO Array
     */
    List<DriverDO> buildDOListByBOList(List<DriverBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    DriverBO buildBOByDO(DriverDO entityDO);

    /**
     * DOList to BOList
     *
     * @param entityDOList EntityDO Array
     * @return EntityBO Array
     */
    List<DriverBO> buildBOListByDOList(List<DriverDO> entityDOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    DriverVO buildVOByBO(DriverBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     */
    List<DriverVO> buildVOListByBOList(List<DriverBO> entityBOList);

    /**
     * BO to DTO
     *
     * @param entityBO EntityBO
     * @return EntityDTO
     */
    DriverDTO buildDTOByBO(DriverBO entityBO);

    /**
     * BOList to DTOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityDTO Array
     */
    List<DriverDTO> buildDTOListByBOList(List<DriverBO> entityBOList);

    /**
     * DTO to BO
     *
     * @param entityDTO EntityDTO
     * @return EntityBO
     */
    DriverBO buildBOByDTO(DriverDTO entityDTO);

    /**
     * DOPage to BOPage
     *
     * @param entityPageDO EntityDO Page
     * @return EntityBO Page
     */
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<DriverBO> buildBOPageByDOPage(Page<DriverDO> entityPageDO);

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<DriverVO> buildVOPageByBOPage(Page<DriverBO> entityPageBO);
}