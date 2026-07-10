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
 * Base MapStruct builder providing shared conversion methods.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface BaseBuilder {

    /**
     * Convert a base VO to a base BO.
     *
     * @param entityVO {@link BaseVO} source
     * @return {@link BaseBO} converted business object
     */
    BaseBO buildBOByVO(BaseVO entityVO);

    /**
     * Convert a list of base VOs to a list of base BOs.
     *
     * @param entityVOList {@link BaseVO} source collection
     * @return {@link BaseBO} converted collection
     */
    List<BaseBO> buildBOListByVOList(List<BaseVO> entityVOList);

    /**
     * Convert a base BO to a base VO.
     *
     * @param entityBO {@link BaseBO} source
     * @return {@link BaseVO} converted view object
     */
    BaseVO buildVOByBO(BaseBO entityBO);

    /**
     * Convert a list of base BOs to a list of base VOs.
     *
     * @param entityBOList {@link BaseBO} source collection
     * @return {@link BaseVO} converted collection
     */
    List<BaseVO> buildVOListByBOList(List<BaseBO> entityBOList);

    /**
     * Convert a base DTO to a base BO.
     *
     * @param entityDTO {@link BaseDTO} source
     * @return {@link BaseBO} converted business object
     */
    BaseBO buildBOByDTO(BaseDTO entityDTO);

    /**
     * Convert a list of base DTOs to a list of base BOs.
     *
     * @param entityDTOList {@link BaseDTO} source collection
     * @return {@link BaseBO} converted collection
     */
    List<BaseBO> buildBOListByDTOList(List<BaseDTO> entityDTOList);

    /**
     * Convert a base BO to a base DTO.
     *
     * @param entityBO {@link BaseBO} source
     * @return {@link BaseDTO} converted transfer object
     */
    BaseDTO buildDTOByBO(BaseBO entityBO);

    /**
     * Convert a list of base BOs to a list of base DTOs.
     *
     * @param entityBOList {@link BaseBO} source collection
     * @return {@link BaseDTO} converted collection
     */
    List<BaseDTO> buildDTOListByBOList(List<BaseBO> entityBOList);

}
