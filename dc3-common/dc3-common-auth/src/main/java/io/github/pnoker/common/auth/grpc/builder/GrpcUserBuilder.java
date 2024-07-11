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

package io.github.pnoker.common.auth.grpc.builder;

import io.github.pnoker.api.center.auth.GrpcUserDTO;
import io.github.pnoker.api.common.GrpcBase;
import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Optional;

/**
 * GrpcUser Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface GrpcUserBuilder {

    /**
     * BO to Grpc DTO
     *
     * @param entityBO UserBO
     * @return GrpcUserDTO
     */
    @Mapping(target = "socialExt", ignore = true)
    @Mapping(target = "identityExt", ignore = true)
    @Mapping(target = "nickNameBytes", ignore = true)
    @Mapping(target = "userNameBytes", ignore = true)
    @Mapping(target = "phoneBytes", ignore = true)
    @Mapping(target = "emailBytes", ignore = true)
    @Mapping(target = "socialExtBytes", ignore = true)
    @Mapping(target = "identityExtBytes", ignore = true)
    @Mapping(target = "mergeFrom", ignore = true)
    @Mapping(target = "clearField", ignore = true)
    @Mapping(target = "clearOneof", ignore = true)
    @Mapping(target = "base", ignore = true)
    @Mapping(target = "mergeBase", ignore = true)
    @Mapping(target = "unknownFields", ignore = true)
    @Mapping(target = "mergeUnknownFields", ignore = true)
    @Mapping(target = "allFields", ignore = true)
    GrpcUserDTO buildGrpcDTOByBO(UserBO entityBO);

    @AfterMapping
    default void afterProcess(UserBO entityBO, @MappingTarget GrpcUserDTO.Builder entityGrpc) {
        GrpcBase grpcBase = GrpcBuilderUtil.buildGrpcBaseByBO(entityBO);
        entityGrpc.setBase(grpcBase);

        Optional.ofNullable(entityBO.getSocialExt()).ifPresent(value -> entityGrpc.setSocialExt(JsonUtil.toJsonString(value)));
        Optional.ofNullable(entityBO.getIdentityExt()).ifPresent(value -> entityGrpc.setIdentityExt(JsonUtil.toJsonString(value)));
    }

}
