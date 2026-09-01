/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.pnoker.common.agentic.entity.builder;

import io.github.pnoker.common.agentic.entity.bo.ModelConfigBO;
import io.github.pnoker.common.agentic.entity.vo.ModelConfigVO;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.Mapper;

import java.util.List;

/** MapStruct builder converting between model configuration business and API objects. */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface ModelConfigBuilder {

    ModelConfigBO buildBOByVO(ModelConfigVO entityVO);

    List<ModelConfigBO> buildBOListByVOList(List<ModelConfigVO> entityVOList);

    ModelConfigVO buildVOByBO(ModelConfigBO entityBO);

    List<ModelConfigVO> buildVOListByBOList(List<ModelConfigBO> entityBOList);
}
