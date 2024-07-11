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

import io.github.pnoker.api.center.manager.GrpcPageDriverQuery;
import io.github.pnoker.api.common.GrpcBase;
import io.github.pnoker.api.common.GrpcDriverDTO;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.ext.DriverExt;
import io.github.pnoker.common.enums.DriverTypeFlagEnum;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.query.DriverQuery;
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
 * GrpcDriver Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface GrpcDriverBuilder {

    /**
     * Grpc Query to Query
     *
     * @param entityQuery GrpcPageDriverQuery
     * @return DriverQuery
     */
    @Mapping(target = "page", ignore = true)
    @Mapping(target = "driverTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    DriverQuery buildQueryByGrpcQuery(GrpcPageDriverQuery entityQuery);

    @AfterMapping
    default void afterProcess(GrpcPageDriverQuery entityGrpc, @MappingTarget DriverQuery.DriverQueryBuilder entityQuery) {
        Pages pages = GrpcBuilderUtil.buildPagesByGrpcPage(entityGrpc.getPage());
        entityQuery.page(pages);

        Optional.ofNullable(DriverTypeFlagEnum.ofIndex((byte) entityGrpc.getDriverTypeFlag())).ifPresent(entityQuery::driverTypeFlag);
        EnableOptional.ofNullable(entityGrpc.getEnableFlag()).ifPresent(entityQuery::enableFlag);
    }

    /**
     * BO to Grpc DTO
     *
     * @param entityBO DriverBO
     * @return GrpcDriverDTO
     */
    @Mapping(target = "driverExt", ignore = true)
    @Mapping(target = "driverTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "driverNameBytes", ignore = true)
    @Mapping(target = "driverCodeBytes", ignore = true)
    @Mapping(target = "serviceNameBytes", ignore = true)
    @Mapping(target = "serviceHostBytes", ignore = true)
    @Mapping(target = "driverExtBytes", ignore = true)
    @Mapping(target = "signatureBytes", ignore = true)
    @Mapping(target = "mergeFrom", ignore = true)
    @Mapping(target = "clearField", ignore = true)
    @Mapping(target = "clearOneof", ignore = true)
    @Mapping(target = "base", ignore = true)
    @Mapping(target = "mergeBase", ignore = true)
    @Mapping(target = "unknownFields", ignore = true)
    @Mapping(target = "mergeUnknownFields", ignore = true)
    @Mapping(target = "allFields", ignore = true)
    GrpcDriverDTO buildGrpcDTOByBO(DriverBO entityBO);

    @AfterMapping
    default void afterProcess(DriverBO entityBO, @MappingTarget GrpcDriverDTO.Builder entityGrpc) {
        GrpcBase grpcBase = GrpcBuilderUtil.buildGrpcBaseByBO(entityBO);
        entityGrpc.setBase(grpcBase);

        Optional.ofNullable(entityBO.getDriverExt()).ifPresent(value -> entityGrpc.setDriverExt(JsonUtil.toJsonString(value)));
        Optional.ofNullable(entityBO.getDriverTypeFlag()).ifPresentOrElse(value -> entityGrpc.setDriverTypeFlag(value.getIndex()), () -> entityGrpc.setDriverTypeFlag(DefaultConstant.NULL_INT));
        Optional.ofNullable(entityBO.getEnableFlag()).ifPresentOrElse(value -> entityGrpc.setEnableFlag(value.getIndex()), () -> entityGrpc.setEnableFlag(DefaultConstant.NULL_INT));
    }

    /**
     * Grpc DTO to BO
     *
     * @param entityGrpc GrpcDriverDTO
     * @return DriverBO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "remark", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    @Mapping(target = "creatorName", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "operatorId", ignore = true)
    @Mapping(target = "operatorName", ignore = true)
    @Mapping(target = "operateTime", ignore = true)
    @Mapping(target = "driverExt", ignore = true)
    @Mapping(target = "driverTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    DriverBO buildBOByGrpcDTO(GrpcDriverDTO entityGrpc);

    @AfterMapping
    default void afterProcess(GrpcDriverDTO entityGrpc, @MappingTarget DriverBO entityBO) {
        GrpcBuilderUtil.buildBaseBOByGrpcBase(entityGrpc.getBase(), entityBO);

        JsonOptional.ofNullable(entityGrpc.getDriverExt()).ifPresent(value -> entityBO.setDriverExt(JsonUtil.parseObject(value, DriverExt.class)));
        Optional.ofNullable(DriverTypeFlagEnum.ofIndex((byte) entityGrpc.getDriverTypeFlag())).ifPresent(entityBO::setDriverTypeFlag);
        EnableOptional.ofNullable(entityGrpc.getEnableFlag()).ifPresent(entityBO::setEnableFlag);
    }
}
