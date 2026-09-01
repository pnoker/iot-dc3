/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.pnoker.common.agentic.entity.builder;

import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.agentic.entity.vo.ModelProviderVO;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Objects;

/** MapStruct builder converting between model provider business and API objects. */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface ModelProviderBuilder {

    @Mapping(target = "apiKey", ignore = true)
    ModelProviderBO buildBOByVO(ModelProviderVO entityVO);

    List<ModelProviderBO> buildBOListByVOList(List<ModelProviderVO> entityVOList);

    @AfterMapping
    default void afterProcess(ModelProviderVO entityRequest, @MappingTarget ModelProviderBO entityBO) {
        if (Objects.nonNull(entityRequest)) entityBO.setProviderType(entityRequest.getProviderType());
    }

    ModelProviderVO buildVOByBO(ModelProviderBO entityBO);

    List<ModelProviderVO> buildVOListByBOList(List<ModelProviderBO> entityBOList);
}
