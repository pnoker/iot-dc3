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

package io.github.pnoker.common.manager.grpc.builder;

import io.github.pnoker.api.common.GrpcBase;
import io.github.pnoker.api.common.GrpcDriverAttributeDTO;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.entity.ext.DriverAttributeExt;
import io.github.pnoker.common.enums.AttributeTypeFlagEnum;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
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
 * GrpcDriverAttribute Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface GrpcDriverAttributeBuilder {

    /**
     * Grpc DTO to BO
     *
     * @param entityGrpc GrpcDriverAttributeDTO
     * @return DriverAttributeBO
     */
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
    DriverAttributeBO buildBOByGrpcDTO(GrpcDriverAttributeDTO entityGrpc);

    @AfterMapping
    default void afterProcess(GrpcDriverAttributeDTO entityGrpc, @MappingTarget DriverAttributeBO entityBO) {
        GrpcBuilderUtil.buildBaseBOByGrpcBase(entityGrpc.getBase(), entityBO);

        JsonOptional.ofNullable(entityGrpc.getAttributeExt()).ifPresent(value -> entityBO.setAttributeExt(JsonUtil.parseObject(value, DriverAttributeExt.class)));
        Optional.ofNullable(AttributeTypeFlagEnum.ofIndex((byte) entityGrpc.getAttributeTypeFlag())).ifPresent(entityBO::setAttributeTypeFlag);
        EnableOptional.ofNullable(entityGrpc.getEnableFlag()).ifPresent(entityBO::setEnableFlag);
    }

    /**
     * BO to Grpc DTO
     *
     * @param entityBO DriverAttributeBO
     * @return GrpcDriverAttributeDTO
     */
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
    GrpcDriverAttributeDTO buildGrpcDTOByBO(DriverAttributeBO entityBO);

    @AfterMapping
    default void afterProcess(DriverAttributeBO entityBO, @MappingTarget GrpcDriverAttributeDTO.Builder entityGrpc) {
        GrpcBase grpcBase = GrpcBuilderUtil.buildGrpcBaseByBO(entityBO);
        entityGrpc.setBase(grpcBase);

        Optional.ofNullable(entityBO.getAttributeExt()).ifPresent(value -> entityGrpc.setAttributeExt(JsonUtil.toJsonString(value)));
        Optional.ofNullable(entityBO.getAttributeTypeFlag()).ifPresentOrElse(value -> entityGrpc.setAttributeTypeFlag(value.getIndex()), () -> entityGrpc.setAttributeTypeFlag(DefaultConstant.NULL_INT));
        Optional.ofNullable(entityBO.getEnableFlag()).ifPresentOrElse(value -> entityGrpc.setEnableFlag(value.getIndex()), () -> entityGrpc.setEnableFlag(DefaultConstant.NULL_INT));
    }
}
