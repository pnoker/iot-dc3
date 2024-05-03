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

package io.github.pnoker.center.manager.grpc.builder;

import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.api.center.manager.GrpcPageDriverQuery;
import io.github.pnoker.api.common.GrpcBase;
import io.github.pnoker.api.common.GrpcDriverDTO;
import io.github.pnoker.center.manager.entity.bo.DriverBO;
import io.github.pnoker.center.manager.entity.query.DriverQuery;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.DriverTypeFlagEnum;
import io.github.pnoker.common.optional.EnableOptional;
import io.github.pnoker.common.optional.IntegerOptional;
import io.github.pnoker.common.optional.LongOptional;
import io.github.pnoker.common.optional.StringOptional;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.github.pnoker.common.utils.JsonUtil;

import java.util.Optional;

/**
 * Point Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
public class GrpcDriverBuilder {

    private GrpcDriverBuilder() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Grpc Query to Query
     *
     * @param entityQuery GrpcPageDriverQuery
     * @return DriverQuery
     */
    public static DriverQuery buildQueryByGrpcQuery(GrpcPageDriverQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery)) {
            return null;
        }

        DriverQuery query = new DriverQuery();
        Pages pages = GrpcBuilderUtil.buildPagesByGrpcPage(entityQuery.getPage());
        query.setPage(pages);

        LongOptional.ofNullable(entityQuery.getTenantId()).ifPresent(query::setTenantId);
        StringOptional.ofNullable(entityQuery.getDriverName()).ifPresent(query::setDriverName);
        StringOptional.ofNullable(entityQuery.getDriverCode()).ifPresent(query::setDriverCode);
        StringOptional.ofNullable(entityQuery.getServiceName()).ifPresent(query::setServiceName);
        StringOptional.ofNullable(entityQuery.getServiceHost()).ifPresent(query::setServiceHost);
        query.setDriverTypeFlag(DriverTypeFlagEnum.ofIndex((byte) entityQuery.getDriverTypeFlag()));
        EnableOptional.ofNullable(entityQuery.getEnableFlag()).ifPresent(query::setEnableFlag);
        IntegerOptional.ofNullable(entityQuery.getVersion()).ifPresent(query::setVersion);

        return query;
    }

    /**
     * BO to Grpc DTO
     *
     * @param entityBO DriverBO
     * @return GrpcDriverDTO
     */
    public static GrpcDriverDTO buildGrpcDTOByBO(DriverBO entityBO) {
        if (ObjectUtil.isNull(entityBO)) {
            return null;
        }

        GrpcDriverDTO.Builder builder = GrpcDriverDTO.newBuilder();
        GrpcBase baseDTO = GrpcBuilderUtil.buildGrpcBaseByBO(entityBO);
        builder.setBase(baseDTO)
                .setDriverName(entityBO.getDriverName())
                .setDriverCode(entityBO.getDriverCode())
                .setServiceName(entityBO.getServiceName())
                .setServiceHost(entityBO.getServiceHost())
                .setDriverExt(JsonUtil.toJsonString(entityBO.getDriverExt()))
                .setSignature(entityBO.getSignature())
                .setVersion(entityBO.getVersion())
                .setTenantId(entityBO.getTenantId());

        Optional.ofNullable(entityBO.getDriverTypeFlag()).ifPresentOrElse(value -> builder.setDriverTypeFlag(value.getIndex()), () -> builder.setDriverTypeFlag(DefaultConstant.DEFAULT_NULL_INT_VALUE));
        Optional.ofNullable(entityBO.getEnableFlag()).ifPresentOrElse(value -> builder.setEnableFlag(value.getIndex()), () -> builder.setEnableFlag(DefaultConstant.DEFAULT_NULL_INT_VALUE));

        return builder.build();
    }
}
