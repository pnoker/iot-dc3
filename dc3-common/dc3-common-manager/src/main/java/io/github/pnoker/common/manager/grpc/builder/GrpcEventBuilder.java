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
import io.github.pnoker.api.common.GrpcEventDTO;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.MapStructUtil;
import java.util.Optional;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct builder for event gRPC message conversion.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Mapper(
        componentModel = "spring",
        uses = {MapStructUtil.class})
public interface GrpcEventBuilder {

    /**
     * Convert bo to grpc transfer object.
     *
     * @param entityBO business object
     * @return converted value
     */
    @Mapping(target = "eventExt", ignore = true)
    @Mapping(target = "eventTypeFlag", ignore = true)
    @Mapping(target = "eventLevelFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "eventNameBytes", ignore = true)
    @Mapping(target = "eventCodeBytes", ignore = true)
    @Mapping(target = "eventExtBytes", ignore = true)
    @Mapping(target = "signatureBytes", ignore = true)
    @Mapping(target = "mergeFrom", ignore = true)
    @Mapping(target = "clearField", ignore = true)
    @Mapping(target = "clearOneof", ignore = true)
    @Mapping(target = "base", ignore = true)
    @Mapping(target = "mergeBase", ignore = true)
    @Mapping(target = "unknownFields", ignore = true)
    @Mapping(target = "mergeUnknownFields", ignore = true)
    @Mapping(target = "allFields", ignore = true)
    GrpcEventDTO buildGrpcDTOByBO(EventBO entityBO);

    /**
     * After process.
     *
     * @param entityBO   business object
     * @param entityGrpc entity grpc
     */
    @AfterMapping
    default void afterProcess(EventBO entityBO, @MappingTarget GrpcEventDTO.Builder entityGrpc) {
        GrpcBase grpcBase = GrpcBuilderUtil.buildGrpcBaseByBO(entityBO);
        entityGrpc.setBase(grpcBase);

        Optional.ofNullable(entityBO.getEventExt())
                .ifPresent(value -> entityGrpc.setEventExt(JsonUtil.toJsonString(value)));
        Optional.ofNullable(entityBO.getEventTypeFlag())
                .ifPresentOrElse(
                        value -> entityGrpc.setEventTypeFlag(value.getIndex()),
                        () -> entityGrpc.setEventTypeFlag(DefaultConstant.NULL_INT));
        Optional.ofNullable(entityBO.getEventLevelFlag())
                .ifPresentOrElse(
                        value -> entityGrpc.setEventLevelFlag(value.getIndex()),
                        () -> entityGrpc.setEventLevelFlag(DefaultConstant.NULL_INT));
        Optional.ofNullable(entityBO.getEnableFlag())
                .ifPresentOrElse(
                        value -> entityGrpc.setEnableFlag(value.getIndex()),
                        () -> entityGrpc.setEnableFlag(DefaultConstant.DEFAULT_INT));
    }
}
