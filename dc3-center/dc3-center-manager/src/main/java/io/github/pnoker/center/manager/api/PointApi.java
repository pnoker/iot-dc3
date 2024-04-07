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
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.enums.RwFlagEnum;
import io.github.pnoker.common.utils.BuilderUtil;
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

        PointQuery pageQuery = buildPageQuery(request);

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
            List<GrpcPointDTO> collect = pointPage.getRecords().stream().map(this::buildDTOByDO).collect(Collectors.toList());
            pagePointBuilder.addAllData(collect);

            builder.setData(pagePointBuilder);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    /**
     * DTO to Query
     *
     * @param request PagePointQueryDTO
     * @return PointPageQuery
     */
    private PointQuery buildPageQuery(GrpcPagePointQuery request) {
        PointQuery pageQuery = new PointQuery();
        Pages pages = new Pages();
        pages.setCurrent(request.getPage().getCurrent());
        pages.setSize(request.getPage().getSize());
        pageQuery.setPage(pages);

        pageQuery.setDeviceId(request.getDeviceId() > DefaultConstant.DEFAULT_INT ? request.getDeviceId() : null);
        pageQuery.setPointName(request.getPointName());
        pageQuery.setProfileId(request.getProfileId() > DefaultConstant.DEFAULT_INT ? request.getProfileId() : null);
        pageQuery.setPointTypeFlag(PointTypeFlagEnum.ofIndex((byte) request.getPointTypeFlag()));
        pageQuery.setRwFlag(RwFlagEnum.ofIndex((byte) request.getRwFlag()));
        pageQuery.setEnableFlag(EnableFlagEnum.ofIndex((byte) request.getEnableFlag()));
        pageQuery.setTenantId(request.getTenantId());

        return pageQuery;
    }

    /**
     * DO to DTO
     *
     * @param entityDO Point
     * @return PointDTO
     */
    private GrpcPointDTO buildDTOByDO(PointBO entityDO) {
        GrpcPointDTO.Builder builder = GrpcPointDTO.newBuilder();
        GrpcBase baseDTO = BuilderUtil.buildBaseDTOByDO(entityDO);
        builder.setBase(baseDTO);
        builder.setPointName(entityDO.getPointName());
        builder.setPointCode(entityDO.getPointCode());
        builder.setPointTypeFlag(entityDO.getPointTypeFlag().getIndex());
        builder.setRwFlag(entityDO.getRwFlag().getIndex());
        builder.setBaseValue(entityDO.getBaseValue().doubleValue());
        builder.setMultiple(entityDO.getMultiple().doubleValue());
        builder.setValueDecimal(entityDO.getValueDecimal());
        builder.setUnit(entityDO.getUnit());
        builder.setProfileId(entityDO.getProfileId());
        builder.setGroupId(entityDO.getGroupId());
        builder.setEnableFlag(entityDO.getEnableFlag().getIndex());
        builder.setTenantId(entityDO.getTenantId());
        return builder.build();
    }

}
