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

package io.github.pnoker.common.entity.builder;

import io.github.pnoker.common.entity.base.BaseBO;
import io.github.pnoker.common.entity.base.BaseDTO;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Base Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface BaseBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    BaseBO buildBOByVO(BaseVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO 集合
     * @return EntityBO 集合
     */
    List<BaseBO> buildBOListByVOList(List<BaseVO> entityVOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    BaseVO buildVOByBO(BaseBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO 集合
     * @return EntityVO 集合
     */
    List<BaseVO> buildVOListByBOList(List<BaseBO> entityBOList);

    /**
     * DTO to BO
     *
     * @param entityDTO EntityDTO
     * @return EntityBO
     */
    BaseBO buildBOByDTO(BaseDTO entityDTO);

    /**
     * DTOList to BOList
     *
     * @param entityDTOList EntityDTO 集合
     * @return EntityBO 集合
     */
    List<BaseBO> buildBOListByDTOList(List<BaseDTO> entityDTOList);

    /**
     * BO to DTO
     *
     * @param entityBO EntityBO
     * @return EntityDTO
     */
    BaseDTO buildDTOByBO(BaseBO entityBO);

    /**
     * BOList to DTOList
     *
     * @param entityBOList EntityBO 集合
     * @return EntityDTO 集合
     */
    List<BaseDTO> buildDTOListByBOList(List<BaseBO> entityBOList);
}