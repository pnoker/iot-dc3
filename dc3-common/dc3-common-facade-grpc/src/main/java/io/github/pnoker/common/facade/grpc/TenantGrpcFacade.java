/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.facade.grpc;

import io.github.pnoker.api.center.auth.GrpcCodeQuery;
import io.github.pnoker.api.center.auth.GrpcRTenantDTO;
import io.github.pnoker.api.center.auth.TenantApiGrpc;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.TenantFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeTenantBO;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcTenantBuilder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

/**
 * gRPC {@link TenantFacade}.
 *
 * @author pnoker
 * @since 2026.4.19
 */
@Slf4j
@Component
public class TenantGrpcFacade implements TenantFacade {

    @GrpcClient(AuthConstant.SERVICE_NAME)
    private TenantApiGrpc.TenantApiBlockingStub tenantApiBlockingStub;

    @Resource
    private FacadeGrpcTenantBuilder facadeGrpcTenantBuilder;

    @Override
    public FacadeTenantBO selectByCode(String code) {
        GrpcRTenantDTO response = tenantApiBlockingStub.selectByCode(GrpcCodeQuery.newBuilder().setCode(code).build());
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "selectByCode");
            return null;
        }
        return facadeGrpcTenantBuilder.toFacadeBO(response.getData());
    }

    private void guardOrThrow(GrpcR result, String op) {
        String code = result.getCode();
        if (ResponseEnum.NO_RESOURCE.getCode().equals(code)) {
            log.debug("TenantGrpcFacade.{} => no resource", op);
            return;
        }
        throw new ServiceException("TenantFacade." + op + " failed: [" + code + "] " + result.getMessage());
    }
}
