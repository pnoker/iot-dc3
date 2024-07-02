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

import io.github.pnoker.api.center.manager.GrpcPagePointQuery;
import io.github.pnoker.api.common.GrpcBase;
import io.github.pnoker.api.common.GrpcPointDTO;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.common.enums.RwFlagEnum;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.query.PointQuery;
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
 * GrpcPoint Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface GrpcPointBuilder {

    /**
     * Grpc Query to Query
     *
     * @param entityQuery GrpcPagePointQuery
     * @return PointQuery
     */
    @Mapping(target = "page", ignore = true)
    @Mapping(target = "pointTypeFlag", ignore = true)
    @Mapping(target = "rwFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    PointQuery buildQueryByGrpcQuery(GrpcPagePointQuery entityQuery);

    @AfterMapping
    default void afterProcess(GrpcPagePointQuery entityGrpc, @MappingTarget PointQuery.PointQueryBuilder entityQuery) {
        Pages pages = GrpcBuilderUtil.buildPagesByGrpcPage(entityGrpc.getPage());
        entityQuery.page(pages);

        Optional.ofNullable(PointTypeFlagEnum.ofIndex((byte) entityGrpc.getPointTypeFlag())).ifPresent(entityQuery::pointTypeFlag);
        Optional.ofNullable(RwFlagEnum.ofIndex((byte) entityGrpc.getRwFlag())).ifPresent(entityQuery::rwFlag);
        EnableOptional.ofNullable(entityGrpc.getEnableFlag()).ifPresent(entityQuery::enableFlag);
    }

    @Mapping(target = "page", ignore = true)
    @Mapping(target = "pointName", ignore = true)
    @Mapping(target = "pointCode", ignore = true)
    @Mapping(target = "groupId", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "profileId", ignore = true)
    @Mapping(target = "pointTypeFlag", ignore = true)
    @Mapping(target = "rwFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    PointQuery buildQueryByGrpcQuery(io.github.pnoker.api.common.driver.GrpcPagePointQuery entityQuery);

    @AfterMapping
    default void afterProcess(io.github.pnoker.api.common.driver.GrpcPagePointQuery entityGrpc, @MappingTarget PointQuery.PointQueryBuilder entityQuery) {
        Pages pages = GrpcBuilderUtil.buildPagesByGrpcPage(entityGrpc.getPage());
        entityQuery.page(pages);
    }

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

    @AfterMapping
    default void afterProcess(PointBO entityBO, @MappingTarget GrpcPointDTO.Builder entityGrpc) {
        GrpcBase grpcBase = GrpcBuilderUtil.buildGrpcBaseByBO(entityBO);
        entityGrpc.setBase(grpcBase);

        Optional.ofNullable(entityBO.getPointExt()).ifPresent(value -> entityGrpc.setPointExt(JsonUtil.toJsonString(value)));
        Optional.ofNullable(entityBO.getPointTypeFlag()).ifPresentOrElse(value -> entityGrpc.setPointTypeFlag(value.getIndex()), () -> entityGrpc.setPointTypeFlag(DefaultConstant.NULL_INT));
        Optional.ofNullable(entityBO.getRwFlag()).ifPresentOrElse(value -> entityGrpc.setRwFlag(value.getIndex()), () -> entityGrpc.setRwFlag(DefaultConstant.NULL_INT));
        Optional.ofNullable(entityBO.getEnableFlag()).ifPresentOrElse(value -> entityGrpc.setEnableFlag(value.getIndex()), () -> entityGrpc.setEnableFlag(DefaultConstant.NULL_INT));
    }
}
