/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
 * @version 2025.6.0
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