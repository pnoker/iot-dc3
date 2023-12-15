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

package io.github.pnoker.center.auth.api;


import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.api.center.auth.GrpcCodeQuery;
import io.github.pnoker.api.center.auth.GrpcRTenantDTO;
import io.github.pnoker.api.center.auth.GrpcTenantDTO;
import io.github.pnoker.api.center.auth.TenantApiGrpc;
import io.github.pnoker.api.common.GrpcBaseDTO;
import io.github.pnoker.api.common.GrpcRDTO;
import io.github.pnoker.center.auth.entity.bo.TenantBO;
import io.github.pnoker.center.auth.service.TenantService;
import io.github.pnoker.common.constant.enums.ResponseEnum;
import io.github.pnoker.common.utils.BuilderUtil;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import javax.annotation.Resource;

/**
 * Tenant Api
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@GrpcService
public class TenantApi extends TenantApiGrpc.TenantApiImplBase {

    @Resource
    private TenantService tenantService;

    @Override
    public void selectByCode(GrpcCodeQuery request, StreamObserver<GrpcRTenantDTO> responseObserver) {
        GrpcRTenantDTO.Builder builder = GrpcRTenantDTO.newBuilder();
        GrpcRDTO.Builder rBuilder = GrpcRDTO.newBuilder();
        TenantBO select = tenantService.selectByCode(request.getCode());
        if (ObjectUtil.isNull(select)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getMessage());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getMessage());
            GrpcTenantDTO tenant = buildGrpcDTOByBO(select);
            builder.setData(tenant);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }


    /**
     * DO to DTO
     *
     * @param entityBO Tenant
     * @return TenantDTO
     */
    private GrpcTenantDTO buildGrpcDTOByBO(TenantBO entityBO) {
        GrpcTenantDTO.Builder builder = GrpcTenantDTO.newBuilder();
        GrpcBaseDTO baseDTO = BuilderUtil.buildBaseDTOByDO(entityBO);
        builder.setBase(baseDTO);
        builder.setTenantName(entityBO.getTenantName());
        builder.setTenantCode(entityBO.getTenantCode());
        builder.setEnableFlag(entityBO.getEnableFlag().getIndex());
        return builder.build();
    }
}
