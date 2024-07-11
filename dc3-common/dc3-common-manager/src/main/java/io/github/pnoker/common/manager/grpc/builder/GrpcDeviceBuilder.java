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

import io.github.pnoker.api.center.manager.GrpcPageDeviceQuery;
import io.github.pnoker.api.common.GrpcBase;
import io.github.pnoker.api.common.GrpcDeviceDTO;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.query.DeviceQuery;
import io.github.pnoker.common.optional.CollectionOptional;
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
 * GrpcDevice Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface GrpcDeviceBuilder {

    /**
     * Grpc Query to Query
     *
     * @param entityQuery GrpcPageDeviceQuery
     * @return DeviceQuery
     */
    @Mapping(target = "page", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    DeviceQuery buildQueryByGrpcQuery(GrpcPageDeviceQuery entityQuery);

    @AfterMapping
    default void afterProcess(GrpcPageDeviceQuery entityGrpc, @MappingTarget DeviceQuery.DeviceQueryBuilder entityQuery) {
        Pages pages = GrpcBuilderUtil.buildPagesByGrpcPage(entityGrpc.getPage());
        entityQuery.page(pages);

        EnableOptional.ofNullable(entityGrpc.getEnableFlag()).ifPresent(entityQuery::enableFlag);
    }

    /**
     * Grpc Query to Query
     *
     * @param entityQuery GrpcPageDeviceQuery
     * @return DeviceQuery
     */
    @Mapping(target = "page", ignore = true)
    @Mapping(target = "deviceName", ignore = true)
    @Mapping(target = "deviceCode", ignore = true)
    @Mapping(target = "groupId", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "profileId", ignore = true)
    DeviceQuery buildQueryByGrpcQuery(io.github.pnoker.api.common.driver.GrpcPageDeviceQuery entityQuery);

    @AfterMapping
    default void afterProcess(io.github.pnoker.api.common.driver.GrpcPageDeviceQuery entityGrpc, @MappingTarget DeviceQuery.DeviceQueryBuilder entityQuery) {
        Pages pages = GrpcBuilderUtil.buildPagesByGrpcPage(entityGrpc.getPage());
        entityQuery.page(pages);
    }

    /**
     * BO to Grpc DTO
     *
     * @param entityBO DeviceBO
     * @return GrpcDeviceDTO
     */
    @Mapping(target = "deviceExt", ignore = true)
    @Mapping(target = "profileIdsList", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deviceNameBytes", ignore = true)
    @Mapping(target = "deviceCodeBytes", ignore = true)
    @Mapping(target = "deviceExtBytes", ignore = true)
    @Mapping(target = "signatureBytes", ignore = true)
    @Mapping(target = "mergeFrom", ignore = true)
    @Mapping(target = "clearField", ignore = true)
    @Mapping(target = "clearOneof", ignore = true)
    @Mapping(target = "base", ignore = true)
    @Mapping(target = "mergeBase", ignore = true)
    @Mapping(target = "unknownFields", ignore = true)
    @Mapping(target = "mergeUnknownFields", ignore = true)
    @Mapping(target = "allFields", ignore = true)
    GrpcDeviceDTO buildGrpcDTOByBO(DeviceBO entityBO);

    @AfterMapping
    default void afterProcess(DeviceBO entityBO, @MappingTarget GrpcDeviceDTO.Builder entityGrpc) {
        GrpcBase grpcBase = GrpcBuilderUtil.buildGrpcBaseByBO(entityBO);
        entityGrpc.setBase(grpcBase);

        CollectionOptional.ofNullable(entityBO.getProfileIds()).ifPresent(entityGrpc::addAllProfileIds);
        Optional.ofNullable(entityBO.getDeviceExt()).ifPresent(value -> entityGrpc.setDeviceExt(JsonUtil.toJsonString(value)));
        Optional.ofNullable(entityBO.getEnableFlag()).ifPresentOrElse(value -> entityGrpc.setEnableFlag(value.getIndex()), () -> entityGrpc.setEnableFlag(DefaultConstant.NULL_INT));
    }

}
