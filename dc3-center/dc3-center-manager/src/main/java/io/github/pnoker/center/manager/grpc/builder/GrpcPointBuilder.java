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
import io.github.pnoker.api.center.manager.GrpcPagePointQuery;
import io.github.pnoker.api.common.GrpcPointDTO;
import io.github.pnoker.center.manager.entity.bo.PointBO;
import io.github.pnoker.center.manager.entity.query.PointQuery;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.common.enums.RwFlagEnum;
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
public class GrpcPointBuilder {

    private GrpcPointBuilder() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Grpc Query to Query
     *
     * @param entityQuery GrpcPagePointQuery
     * @return PointQuery
     */
    public static PointQuery buildQueryByGrpcQuery(GrpcPagePointQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery)) {
            return null;
        }

        PointQuery query = new PointQuery();
        Pages pages = GrpcBuilderUtil.buildPagesByGrpcPage(entityQuery.getPage());
        query.setPage(pages);

        LongOptional.ofNullable(entityQuery.getTenantId()).ifPresent(query::setTenantId);
        StringOptional.ofNullable(entityQuery.getPointName()).ifPresent(query::setPointName);
        StringOptional.ofNullable(entityQuery.getPointCode()).ifPresent(query::setPointCode);
        query.setPointTypeFlag(PointTypeFlagEnum.ofIndex((byte) entityQuery.getPointTypeFlag()));
        query.setRwFlag(RwFlagEnum.ofIndex((byte) entityQuery.getRwFlag()));
        LongOptional.ofNullable(entityQuery.getProfileId()).ifPresent(query::setProfileId);
        LongOptional.ofNullable(entityQuery.getGroupId()).ifPresent(query::setGroupId);
        EnableOptional.ofNullable(entityQuery.getEnableFlag()).ifPresent(query::setEnableFlag);
        IntegerOptional.ofNullable(entityQuery.getVersion()).ifPresent(query::setVersion);
        LongOptional.ofNullable(entityQuery.getDeviceId()).ifPresent(query::setDeviceId);

        return query;
    }

    /**
     * BO to Grpc DTO
     *
     * @param entityBO PointBO
     * @return GrpcPointDTO
     */
    public static GrpcPointDTO buildGrpcDTOByBO(PointBO entityBO) {
        if (ObjectUtil.isNull(entityBO)) {
            return null;
        }

        GrpcPointDTO.Builder builder = GrpcPointDTO.newBuilder();
        builder.setBase(GrpcBuilderUtil.buildGrpcBaseByBO(entityBO))
                .setPointName(entityBO.getPointName())
                .setPointCode(entityBO.getPointCode())
                .setBaseValue(entityBO.getBaseValue().doubleValue())
                .setMultiple(entityBO.getMultiple().doubleValue())
                .setValueDecimal(entityBO.getValueDecimal())
                .setUnit(entityBO.getUnit())
                .setProfileId(entityBO.getProfileId())
                .setAlarmNotifyProfileId(entityBO.getAlarmNotifyProfileId())
                .setAlarmMessageProfileId(entityBO.getAlarmMessageProfileId())
                .setGroupId(entityBO.getGroupId())
                .setPointExt(JsonUtil.toJsonString(entityBO.getPointExt()))
                .setSignature(entityBO.getSignature())
                .setVersion(entityBO.getVersion())
                .setTenantId(entityBO.getTenantId());

        Optional.ofNullable(entityBO.getPointTypeFlag()).ifPresentOrElse(value -> builder.setPointTypeFlag(value.getIndex()), () -> builder.setPointTypeFlag(DefaultConstant.DEFAULT_NULL_INT_VALUE));
        Optional.ofNullable(entityBO.getRwFlag()).ifPresentOrElse(value -> builder.setRwFlag(value.getIndex()), () -> builder.setRwFlag(DefaultConstant.DEFAULT_NULL_INT_VALUE));
        Optional.ofNullable(entityBO.getEnableFlag()).ifPresentOrElse(value -> builder.setEnableFlag(value.getIndex()), () -> builder.setEnableFlag(DefaultConstant.DEFAULT_NULL_INT_VALUE));

        return builder.build();
    }
}
