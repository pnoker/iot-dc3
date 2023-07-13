/*
 * Copyright 2016-present the original author or authors.
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
import io.github.pnoker.api.common.*;
import io.github.pnoker.center.manager.entity.query.PointPageQuery;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.enums.RwFlagEnum;
import io.github.pnoker.common.model.Point;
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
    public void list(PagePointQueryDTO request, StreamObserver<RPagePointDTO> responseObserver) {
        RPagePointDTO.Builder builder = RPagePointDTO.newBuilder();
        RDTO.Builder rBuilder = RDTO.newBuilder();

        PointPageQuery pageQuery = buildPageQuery(request);

        Page<Point> pointPage = pointService.list(pageQuery);
        if (ObjectUtil.isNull(pointPage)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getMessage());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getMessage());

            PagePointDTO.Builder pagePointBuilder = PagePointDTO.newBuilder();
            PageDTO.Builder pageBuilder = PageDTO.newBuilder();
            pageBuilder.setCurrent(pointPage.getCurrent());
            pageBuilder.setSize(pointPage.getSize());
            pageBuilder.setPages(pointPage.getPages());
            pageBuilder.setTotal(pointPage.getTotal());
            pagePointBuilder.setPage(pageBuilder);
            List<PointDTO> collect = pointPage.getRecords().stream().map(this::buildDTOByDO).collect(Collectors.toList());
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
    private PointPageQuery buildPageQuery(PagePointQueryDTO request) {
        PointPageQuery pageQuery = new PointPageQuery();
        Pages pages = new Pages();
        pages.setCurrent(request.getPage().getCurrent());
        pages.setSize(request.getPage().getSize());
        pageQuery.setPage(pages);

        PointDTO point = request.getPoint();
        pageQuery.setDeviceId(request.getDeviceId());
        pageQuery.setPointName(point.getPointName());
        pageQuery.setProfileId(point.getProfileId());
        pageQuery.setTenantId(point.getTenantId());
        pageQuery.setPointTypeFlag(PointTypeFlagEnum.ofName(point.getPointTypeFlag().name()));
        pageQuery.setRwFlag(RwFlagEnum.ofName(point.getRwFlag().name()));
        pageQuery.setEnableFlag(EnableFlagEnum.ofName(point.getEnableFlag().name()));

        return pageQuery;
    }

    /**
     * DO to DTO
     *
     * @param entityDO Point
     * @return PointDTO
     */
    private PointDTO buildDTOByDO(Point entityDO) {
        PointDTO.Builder builder = PointDTO.newBuilder();
        BaseDTO baseDTO = BuilderUtil.buildBaseDTOByDO(entityDO);
        builder.setBase(baseDTO);
        builder.setPointName(entityDO.getPointName());
        builder.setPointCode(entityDO.getPointCode());
        builder.setPointTypeFlag(PointTypeFlagDTOEnum.valueOf(entityDO.getPointTypeFlag().name()));
        builder.setRwFlag(RwFlagDTOEnum.valueOf(entityDO.getRwFlag().name()));
        builder.setBaseValue(entityDO.getBaseValue().doubleValue());
        builder.setMultiple(entityDO.getMultiple().doubleValue());
        builder.setValueDecimal(entityDO.getValueDecimal());
        builder.setUnit(entityDO.getUnit());
        builder.setProfileId(entityDO.getProfileId());
        builder.setGroupId(entityDO.getGroupId());
        builder.setEnableFlag(EnableFlagDTOEnum.valueOf(entityDO.getEnableFlag().name()));
        builder.setTenantId(entityDO.getTenantId());
        return builder.build();
    }

}
