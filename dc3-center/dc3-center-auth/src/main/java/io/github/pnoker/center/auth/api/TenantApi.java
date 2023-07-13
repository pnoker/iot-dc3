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

package io.github.pnoker.center.auth.api;


import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.api.center.auth.CodeQuery;
import io.github.pnoker.api.center.auth.RTenantDTO;
import io.github.pnoker.api.center.auth.TenantApiGrpc;
import io.github.pnoker.api.center.auth.TenantDTO;
import io.github.pnoker.api.common.BaseDTO;
import io.github.pnoker.api.common.EnableFlagDTOEnum;
import io.github.pnoker.api.common.RDTO;
import io.github.pnoker.center.auth.service.TenantService;
import io.github.pnoker.common.utils.BuilderUtil;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.model.Tenant;
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
    public void selectByCode(CodeQuery request, StreamObserver<RTenantDTO> responseObserver) {
        RTenantDTO.Builder builder = RTenantDTO.newBuilder();
        RDTO.Builder rBuilder = RDTO.newBuilder();
        Tenant select = tenantService.selectByCode(request.getCode());
        if (ObjectUtil.isNull(select)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getMessage());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getMessage());
            TenantDTO tenant = buildDTOByDO(select);
            builder.setData(tenant);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }


    /**
     * DO to DTO
     *
     * @param entityDO Tenant
     * @return TenantDTO
     */
    private TenantDTO buildDTOByDO(Tenant entityDO) {
        TenantDTO.Builder builder = TenantDTO.newBuilder();
        BaseDTO baseDTO = BuilderUtil.buildBaseDTOByDO(entityDO);
        builder.setBase(baseDTO);
        builder.setTenantName(entityDO.getTenantName());
        builder.setTenantCode(entityDO.getTenantCode());
        builder.setEnableFlag(EnableFlagDTOEnum.valueOf(entityDO.getEnableFlag().name()));
        return builder.build();
    }
}
