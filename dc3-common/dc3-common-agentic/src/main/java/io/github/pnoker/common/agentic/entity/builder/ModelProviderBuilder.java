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
package io.github.pnoker.common.agentic.entity.builder;

import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.agentic.entity.vo.ModelProviderVO;
import io.github.pnoker.common.utils.MapStructUtil;
import java.util.List;
import java.util.Objects;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/** MapStruct builder converting between model provider business and API objects. */
@Mapper(
        componentModel = "spring",
        uses = {MapStructUtil.class})
public interface ModelProviderBuilder {

    /** Convert the value object into its business-object form. */
    @Mapping(target = "apiKey", ignore = true)
    ModelProviderBO buildBOByVO(ModelProviderVO entityVO);

    /** Convert the value objects into their business-object forms. */
    List<ModelProviderBO> buildBOListByVOList(List<ModelProviderVO> entityVOList);

    /** Post-process the mapped target after MapStruct copies the fields. */
    @AfterMapping
    default void afterProcess(ModelProviderVO entityRequest, @MappingTarget ModelProviderBO entityBO) {
        if (Objects.nonNull(entityRequest)) entityBO.setProviderType(entityRequest.getProviderType());
    }

    /** Convert the business object into its value-object form. */
    ModelProviderVO buildVOByBO(ModelProviderBO entityBO);

    /** Convert the business objects into their value-object forms. */
    List<ModelProviderVO> buildVOListByBOList(List<ModelProviderBO> entityBOList);
}
