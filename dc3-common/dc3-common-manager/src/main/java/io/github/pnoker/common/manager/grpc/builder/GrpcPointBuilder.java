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

package io.github.pnoker.common.manager.grpc.builder;

import io.github.pnoker.api.common.GrpcBase;
import io.github.pnoker.api.common.GrpcPointDTO;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Optional;


/**
 * MapStruct builder for point gRPC message conversion.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface GrpcPointBuilder {

    /**
     * BO to Grpc DTO
     *
     * @param entityBO PointBO
     * @return GrpcPointDTO
     */
    @Mapping(target = "pointExt", ignore = true)
    @Mapping(target = "pointTypeFlag", ignore = true)
    @Mapping(target = "rwFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "pointNameBytes", ignore = true)
    @Mapping(target = "pointCodeBytes", ignore = true)
    @Mapping(target = "unitBytes", ignore = true)
    @Mapping(target = "pointExtBytes", ignore = true)
    @Mapping(target = "signatureBytes", ignore = true)
    @Mapping(target = "mergeFrom", ignore = true)
    @Mapping(target = "clearField", ignore = true)
    @Mapping(target = "clearOneof", ignore = true)
    @Mapping(target = "base", ignore = true)
    @Mapping(target = "mergeBase", ignore = true)
    @Mapping(target = "unknownFields", ignore = true)
    @Mapping(target = "mergeUnknownFields", ignore = true)
    @Mapping(target = "allFields", ignore = true)
    GrpcPointDTO buildGrpcDTOByBO(PointBO entityBO);

    /**
     * After process.
     *
     * @param entityBO   business object
     * @param entityGrpc entity grpc
     */
    @AfterMapping
    default void afterProcess(PointBO entityBO, @MappingTarget GrpcPointDTO.Builder entityGrpc) {
        GrpcBase grpcBase = GrpcBuilderUtil.buildGrpcBaseByBO(entityBO);
        entityGrpc.setBase(grpcBase);

        Optional.ofNullable(entityBO.getPointExt())
                .ifPresent(value -> entityGrpc.setPointExt(JsonUtil.toJsonString(value)));
        Optional.ofNullable(entityBO.getPointTypeFlag())
                .ifPresentOrElse(value -> entityGrpc.setPointTypeFlag(value.getIndex()),
                        () -> entityGrpc.setPointTypeFlag(DefaultConstant.NULL_INT));
        Optional.ofNullable(entityBO.getRwFlag())
                .ifPresentOrElse(value -> entityGrpc.setRwFlag(value.getIndex()),
                        () -> entityGrpc.setRwFlag(DefaultConstant.NULL_INT));
        Optional.ofNullable(entityBO.getEnableFlag())
                .ifPresentOrElse(value -> entityGrpc.setEnableFlag(value.getIndex()),
                        () -> entityGrpc.setEnableFlag(DefaultConstant.DEFAULT_INT));
    }

}
