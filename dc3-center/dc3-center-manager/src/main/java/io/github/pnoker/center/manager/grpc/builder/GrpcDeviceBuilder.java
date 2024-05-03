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
import io.github.pnoker.api.center.manager.GrpcPageDeviceQuery;
import io.github.pnoker.api.common.GrpcBase;
import io.github.pnoker.api.common.GrpcDeviceDTO;
import io.github.pnoker.center.manager.entity.bo.DeviceBO;
import io.github.pnoker.center.manager.entity.query.DeviceQuery;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.entity.common.Pages;
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
public class GrpcDeviceBuilder {

    private GrpcDeviceBuilder() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Grpc Query to Query
     *
     * @param entityQuery GrpcPageDeviceQuery
     * @return DeviceQuery
     */
    public static DeviceQuery buildQueryByGrpcQuery(GrpcPageDeviceQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery)) {
            return null;
        }

        DeviceQuery query = new DeviceQuery();
        Pages pages = GrpcBuilderUtil.buildPagesByGrpcPage(entityQuery.getPage());
        query.setPage(pages);

        LongOptional.ofNullable(entityQuery.getTenantId()).ifPresent(query::setTenantId);
        StringOptional.ofNullable(entityQuery.getDeviceName()).ifPresent(query::setDeviceName);
        StringOptional.ofNullable(entityQuery.getDeviceCode()).ifPresent(query::setDeviceCode);
        LongOptional.ofNullable(entityQuery.getDriverId()).ifPresent(query::setDriverId);
        LongOptional.ofNullable(entityQuery.getGroupId()).ifPresent(query::setGroupId);
        EnableOptional.ofNullable(entityQuery.getEnableFlag()).ifPresent(query::setEnableFlag);
        IntegerOptional.ofNullable(entityQuery.getVersion()).ifPresent(query::setVersion);
        LongOptional.ofNullable(entityQuery.getProfileId()).ifPresent(query::setProfileId);

        return query;
    }

    /**
     * BO to Grpc DTO
     *
     * @param entityBO DeviceBO
     * @return GrpcDeviceDTO
     */
    public static GrpcDeviceDTO buildGrpcDTOByBO(DeviceBO entityBO) {
        if (ObjectUtil.isNull(entityBO)) {
            return null;
        }

        GrpcDeviceDTO.Builder builder = GrpcDeviceDTO.newBuilder();
        GrpcBase baseDTO = GrpcBuilderUtil.buildGrpcBaseByBO(entityBO);
        builder.setBase(baseDTO)
                .setDeviceName(entityBO.getDeviceName())
                .setDeviceCode(entityBO.getDeviceCode())
                .setDriverId(entityBO.getDriverId())
                .setGroupId(entityBO.getGroupId())
                .setDeviceExt(JsonUtil.toJsonString(entityBO.getDeviceExt()))
                .setSignature(entityBO.getSignature())
                .setVersion(entityBO.getVersion())
                .setTenantId(entityBO.getTenantId());

        Optional.ofNullable(entityBO.getEnableFlag()).ifPresentOrElse(value -> builder.setEnableFlag(value.getIndex()), () -> builder.setEnableFlag(DefaultConstant.DEFAULT_NULL_INT_VALUE));

        return builder.build();
    }
}
