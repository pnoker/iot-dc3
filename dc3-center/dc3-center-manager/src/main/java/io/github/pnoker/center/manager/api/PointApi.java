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

package io.github.pnoker.center.manager.api;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.*;
import io.github.pnoker.api.common.GrpcBase;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.center.manager.entity.bo.PointBO;
import io.github.pnoker.center.manager.entity.query.PointQuery;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.enums.RwFlagEnum;
import io.github.pnoker.common.optional.LongOptional;
import io.github.pnoker.common.optional.StringOptional;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Point Api
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@GrpcService
public class PointApi extends PointApiGrpc.PointApiImplBase {

    @Resource
    private PointService pointService;

    @Override
    public void list(GrpcPagePointQuery request, StreamObserver<GrpcRPagePointDTO> responseObserver) {
        GrpcRPagePointDTO.Builder builder = GrpcRPagePointDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        PointQuery pageQuery = buildQueryByGrpcQuery(request);

        Page<PointBO> pointPage = pointService.selectByPage(pageQuery);
        if (ObjectUtil.isNull(pointPage)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            GrpcPagePointDTO.Builder pagePointBuilder = GrpcPagePointDTO.newBuilder();
            GrpcPage.Builder pageBuilder = GrpcPage.newBuilder();
            pageBuilder.setCurrent(pointPage.getCurrent());
            pageBuilder.setSize(pointPage.getSize());
            pageBuilder.setPages(pointPage.getPages());
            pageBuilder.setTotal(pointPage.getTotal());
            pagePointBuilder.setPage(pageBuilder);
            List<GrpcPointDTO> collect = pointPage.getRecords().stream().map(this::buildGrpcDTOByBO).collect(Collectors.toList());
            pagePointBuilder.addAllData(collect);

            builder.setData(pagePointBuilder);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    /**
     * Grpc Query to Query
     *
     * @param entityQuery GrpcPagePointQuery
     * @return PointQuery
     */
    private PointQuery buildQueryByGrpcQuery(GrpcPagePointQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery)) {
            return null;
        }

        PointQuery query = new PointQuery();
        Pages pages = GrpcBuilderUtil.buildPagesByGrpcPage(entityQuery.getPage());
        query.setPage(pages);

        LongOptional.of(entityQuery.getDeviceId()).ifPresent(query::setDeviceId);
        StringOptional.of(entityQuery.getPointName()).ifPresent(query::setPointName);
        LongOptional.of(entityQuery.getProfileId()).ifPresent(query::setProfileId);
        query.setPointTypeFlag(PointTypeFlagEnum.ofIndex((byte) entityQuery.getPointTypeFlag()));
        query.setRwFlag(RwFlagEnum.ofIndex((byte) entityQuery.getRwFlag()));
        query.setEnableFlag(EnableFlagEnum.ofIndex((byte) entityQuery.getEnableFlag()));
        LongOptional.of(entityQuery.getTenantId()).ifPresent(query::setTenantId);

        return query;
    }

    /**
     * BO to Grpc DTO
     *
     * @param entityBO PointBO
     * @return GrpcPointDTO
     */
    private GrpcPointDTO buildGrpcDTOByBO(PointBO entityBO) {
        if (ObjectUtil.isNull(entityBO)) {
            return null;
        }

        GrpcPointDTO.Builder builder = GrpcPointDTO.newBuilder();
        GrpcBase baseDTO = GrpcBuilderUtil.buildGrpcBaseByBO(entityBO);
        builder.setBase(baseDTO);

        builder.setPointName(entityBO.getPointName());
        builder.setPointCode(entityBO.getPointCode());
        builder.setPointTypeFlag(entityBO.getPointTypeFlag().getIndex());
        builder.setRwFlag(entityBO.getRwFlag().getIndex());
        builder.setBaseValue(entityBO.getBaseValue().doubleValue());
        builder.setMultiple(entityBO.getMultiple().doubleValue());
        builder.setValueDecimal(entityBO.getValueDecimal());
        builder.setUnit(entityBO.getUnit());
        builder.setProfileId(entityBO.getProfileId());
        builder.setGroupId(entityBO.getGroupId());
        builder.setEnableFlag(entityBO.getEnableFlag().getIndex());
        builder.setTenantId(entityBO.getTenantId());
        return builder.build();
    }

}
