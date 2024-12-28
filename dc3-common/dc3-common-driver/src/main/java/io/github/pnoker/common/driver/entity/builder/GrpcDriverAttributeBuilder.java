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

package io.github.pnoker.common.driver.entity.builder;

import io.github.pnoker.api.common.GrpcBase;
import io.github.pnoker.api.common.GrpcDriverAttributeDTO;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.driver.entity.dto.DriverAttributeDTO;
import io.github.pnoker.common.entity.ext.DriverAttributeExt;
import io.github.pnoker.common.enums.AttributeTypeFlagEnum;
import io.github.pnoker.common.optional.EnableOptional;
import io.github.pnoker.common.optional.JsonOptional;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Optional;

/**
 * DriverAttribute Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface GrpcDriverAttributeBuilder {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "remark", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    @Mapping(target = "creatorName", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "operatorId", ignore = true)
    @Mapping(target = "operatorName", ignore = true)
    @Mapping(target = "operateTime", ignore = true)
    @Mapping(target = "attributeExt", ignore = true)
    @Mapping(target = "attributeTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    DriverAttributeDTO buildDTOByGrpcDTO(GrpcDriverAttributeDTO entityGrpc);

    @AfterMapping
    default void afterProcess(GrpcDriverAttributeDTO entityGrpc, @MappingTarget DriverAttributeDTO entityDTO) {
        GrpcBuilderUtil.buildBaseDTOByGrpcBase(entityGrpc.getBase(), entityDTO);

        JsonOptional.ofNullable(entityGrpc.getAttributeExt()).ifPresent(value -> entityDTO.setAttributeExt(JsonUtil.parseObject(value, DriverAttributeExt.class)));
        Optional.ofNullable(AttributeTypeFlagEnum.ofIndex((byte) entityGrpc.getAttributeTypeFlag())).ifPresent(entityDTO::setAttributeTypeFlag);
        EnableOptional.ofNullable(entityGrpc.getEnableFlag()).ifPresent(entityDTO::setEnableFlag);
    }


    @Mapping(target = "attributeExt", ignore = true)
    @Mapping(target = "attributeTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "displayNameBytes", ignore = true)
    @Mapping(target = "attributeNameBytes", ignore = true)
    @Mapping(target = "defaultValueBytes", ignore = true)
    @Mapping(target = "attributeExtBytes", ignore = true)
    @Mapping(target = "signatureBytes", ignore = true)
    @Mapping(target = "mergeFrom", ignore = true)
    @Mapping(target = "clearField", ignore = true)
    @Mapping(target = "clearOneof", ignore = true)
    @Mapping(target = "base", ignore = true)
    @Mapping(target = "mergeBase", ignore = true)
    @Mapping(target = "unknownFields", ignore = true)
    @Mapping(target = "mergeUnknownFields", ignore = true)
    @Mapping(target = "allFields", ignore = true)
    GrpcDriverAttributeDTO buildGrpcDTOByDTO(DriverAttributeDTO entityDTO);

    @AfterMapping
    default void afterProcess(DriverAttributeDTO entityDTO, @MappingTarget GrpcDriverAttributeDTO.Builder entityGrpc) {
        GrpcBase grpcBase = GrpcBuilderUtil.buildGrpcBaseByDTO(entityDTO);
        entityGrpc.setBase(grpcBase);

        Optional.ofNullable(entityDTO.getAttributeExt()).ifPresent(value -> entityGrpc.setAttributeExt(JsonUtil.toJsonString(value)));
        Optional.ofNullable(entityDTO.getAttributeTypeFlag()).ifPresentOrElse(value -> entityGrpc.setAttributeTypeFlag(value.getIndex()), () -> entityGrpc.setAttributeTypeFlag(DefaultConstant.NULL_INT));
        Optional.ofNullable(entityDTO.getEnableFlag()).ifPresentOrElse(value -> entityGrpc.setEnableFlag(value.getIndex()), () -> entityGrpc.setEnableFlag(DefaultConstant.NULL_INT));
    }
}