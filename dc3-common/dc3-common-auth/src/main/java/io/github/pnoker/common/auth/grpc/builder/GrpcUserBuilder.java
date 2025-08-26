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
 * @version 2025.6.0
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
