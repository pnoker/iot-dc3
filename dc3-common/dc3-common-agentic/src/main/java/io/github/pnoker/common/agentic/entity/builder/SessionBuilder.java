/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */
package io.github.pnoker.common.agentic.entity.builder;

import io.github.pnoker.common.agentic.entity.bo.SessionBO;
import io.github.pnoker.common.agentic.entity.vo.SessionVO;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.Mapper;

import java.util.List;

/** Maps reactive session projections to API resources. */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface SessionBuilder {

    SessionVO buildVOByBO(SessionBO entityBO);

    List<SessionVO> buildVOListByBOList(List<SessionBO> entityBOList);
}
