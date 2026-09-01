/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */
package io.github.pnoker.common.agentic.entity.builder;

import io.github.pnoker.common.agentic.entity.bo.MessageBO;
import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import io.github.pnoker.common.agentic.entity.vo.MessageVO;
import io.github.pnoker.common.utils.MapStructUtil;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Objects;

/** Maps reactive message projections to API resources. */
@Mapper(componentModel = "spring", implementationName = "AgenticMessageBuilderImpl", uses = {MapStructUtil.class})
public interface MessageBuilder {

    @Mapping(target = "content", ignore = true)
    @Mapping(target = "contentExt", source = "content")
    MessageVO buildVOByBO(MessageBO entityBO);

    List<MessageVO> buildVOListByBOList(List<MessageBO> entityBOList);

    @AfterMapping
    default void afterProcess(MessageBO entityBO, @MappingTarget MessageVO entityVO) {
        AgenticMessageContent content = Objects.nonNull(entityBO.getContent())
                ? entityBO.getContent() : AgenticMessageContent.ofText("");
        entityVO.setContent(StringUtils.defaultString(content.getText()));
        entityVO.setContentExt(content);
    }
}
