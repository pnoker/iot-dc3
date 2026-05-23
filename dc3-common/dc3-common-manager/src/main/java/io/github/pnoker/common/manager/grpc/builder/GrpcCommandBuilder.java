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

import io.github.pnoker.api.center.manager.GrpcPageCommandQuery;
import io.github.pnoker.api.common.GrpcBase;
import io.github.pnoker.api.common.GrpcCommandDTO;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.CallTypeFlagEnum;
import io.github.pnoker.common.enums.CommandTypeFlagEnum;
import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.common.manager.entity.query.CommandQuery;
import io.github.pnoker.common.optional.EnableOptional;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Optional;

/**
 * MapStruct builder for command gRPC message conversion.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface GrpcCommandBuilder {

    @Mapping(target = "page", ignore = true)
    @Mapping(target = "commandType", ignore = true)
    @Mapping(target = "callType", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    CommandQuery buildQueryByGrpcQuery(GrpcPageCommandQuery entityQuery);

    @AfterMapping
    default void afterProcess(GrpcPageCommandQuery entityGrpc, @MappingTarget CommandQuery.CommandQueryBuilder entityQuery) {
        Pages pages = GrpcBuilderUtil.buildPagesByGrpcPage(entityGrpc.getPage());
        entityQuery.page(pages);

        Optional.ofNullable(CommandTypeFlagEnum.ofIndex((byte) entityGrpc.getCommandTypeFlag()))
                .ifPresent(entityQuery::commandType);
        Optional.ofNullable(CallTypeFlagEnum.ofIndex((byte) entityGrpc.getCallTypeFlag()))
                .ifPresent(entityQuery::callType);
        EnableOptional.ofNullable(entityGrpc.getEnableFlag()).ifPresent(entityQuery::enableFlag);
    }

    @Mapping(target = "commandExt", ignore = true)
    @Mapping(target = "commandTypeFlag", ignore = true)
    @Mapping(target = "callTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "commandNameBytes", ignore = true)
    @Mapping(target = "commandCodeBytes", ignore = true)
    @Mapping(target = "commandExtBytes", ignore = true)
    @Mapping(target = "signatureBytes", ignore = true)
    @Mapping(target = "mergeFrom", ignore = true)
    @Mapping(target = "clearField", ignore = true)
    @Mapping(target = "clearOneof", ignore = true)
    @Mapping(target = "base", ignore = true)
    @Mapping(target = "mergeBase", ignore = true)
    @Mapping(target = "unknownFields", ignore = true)
    @Mapping(target = "mergeUnknownFields", ignore = true)
    @Mapping(target = "allFields", ignore = true)
    GrpcCommandDTO buildGrpcDTOByBO(CommandBO entityBO);

    @AfterMapping
    default void afterProcess(CommandBO entityBO, @MappingTarget GrpcCommandDTO.Builder entityGrpc) {
        GrpcBase grpcBase = GrpcBuilderUtil.buildGrpcBaseByBO(entityBO);
        entityGrpc.setBase(grpcBase);

        Optional.ofNullable(entityBO.getCommandExt())
                .ifPresent(value -> entityGrpc.setCommandExt(JsonUtil.toJsonString(value)));
        Optional.ofNullable(entityBO.getCommandTypeFlag())
                .ifPresentOrElse(value -> entityGrpc.setCommandTypeFlag(value.getIndex()),
                        () -> entityGrpc.setCommandTypeFlag(DefaultConstant.NULL_INT));
        Optional.ofNullable(entityBO.getCallTypeFlag())
                .ifPresentOrElse(value -> entityGrpc.setCallTypeFlag(value.getIndex()),
                        () -> entityGrpc.setCallTypeFlag(DefaultConstant.NULL_INT));
        Optional.ofNullable(entityBO.getEnableFlag())
                .ifPresentOrElse(value -> entityGrpc.setEnableFlag(value.getIndex()),
                        () -> entityGrpc.setEnableFlag(DefaultConstant.DEFAULT_INT));
    }

}
