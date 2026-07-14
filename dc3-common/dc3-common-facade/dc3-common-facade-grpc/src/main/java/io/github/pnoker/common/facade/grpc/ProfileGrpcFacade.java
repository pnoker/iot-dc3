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

import io.github.pnoker.api.center.manager.GrpcDeviceQuery;
import io.github.pnoker.api.center.manager.GrpcPageProfileDTO;
import io.github.pnoker.api.center.manager.GrpcProfileIdsQuery;
import io.github.pnoker.api.center.manager.GrpcProfileQuery;
import io.github.pnoker.api.center.manager.GrpcRPageProfileDTO;
import io.github.pnoker.api.center.manager.GrpcRProfileDTO;
import io.github.pnoker.api.center.manager.GrpcRProfileListDTO;
import io.github.pnoker.api.center.manager.ProfileApiGrpc;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.enums.ErrorCode;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.ProfileFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeProfileBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeProfileQuery;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcProfileBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * gRPC ProfileFacade: forwards to Manager Center via {@link ProfileApiGrpc}.
 *
 * @author pnoker
 * @version 2026.5.14
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileGrpcFacade implements ProfileFacade {

    private final ProfileApiGrpc.ProfileApiBlockingStub profileApiBlockingStub;

    private final FacadeGrpcProfileBuilder facadeGrpcProfileBuilder;

    private final GrpcFacadeSupport grpcFacadeSupport;

    @Override
    public FacadeProfileBO getById(Long tenantId, Long id) {
        GrpcProfileQuery request = GrpcProfileQuery.newBuilder().setProfileId(id).setTenantId(tenantId).build();
        GrpcRProfileDTO response = grpcFacadeSupport.call("ProfileFacade.getById", profileApiBlockingStub,
                stub -> stub.getByProfileId(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "getById");
            return null;
        }
        return facadeGrpcProfileBuilder.toFacadeBO(response.getData());
    }

    @Override
    public List<FacadeProfileBO> listByIds(Long tenantId, Collection<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> profileIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (profileIds.isEmpty()) {
            return Collections.emptyList();
        }

        GrpcProfileIdsQuery request = GrpcProfileIdsQuery.newBuilder().addAllProfileIds(profileIds).setTenantId(tenantId).build();
        GrpcRProfileListDTO response = grpcFacadeSupport.call("ProfileFacade.listByIds", profileApiBlockingStub,
                stub -> stub.listByProfileIds(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "listByIds");
            return Collections.emptyList();
        }
        return response.getDataList().stream().map(facadeGrpcProfileBuilder::toFacadeBO).toList();
    }

    @Override
    public FacadePage<FacadeProfileBO> listByPage(FacadeProfileQuery query) {
        GrpcRPageProfileDTO response = grpcFacadeSupport.call("ProfileFacade.listByPage", profileApiBlockingStub,
                stub -> stub.listByPage(facadeGrpcProfileBuilder.toGrpcPageQuery(query)));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "listByPage");
            return FacadePage.empty();
        }
        GrpcPageProfileDTO pageDTO = response.getData();
        List<FacadeProfileBO> records = pageDTO.getDataList().stream()
                .map(facadeGrpcProfileBuilder::toFacadeBO)
                .toList();
        return new FacadePage<>(pageDTO.getPage().getCurrent(), pageDTO.getPage().getSize(),
                pageDTO.getPage().getTotal(), pageDTO.getPage().getPages(), records);
    }

    @Override
    public List<FacadeProfileBO> listByDeviceId(Long tenantId, Long deviceId) {
        GrpcDeviceQuery request = GrpcDeviceQuery.newBuilder().setDeviceId(deviceId).setTenantId(tenantId).build();
        GrpcRProfileListDTO response = grpcFacadeSupport.call("ProfileFacade.listByDeviceId", profileApiBlockingStub,
                stub -> stub.listByDeviceId(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "listByDeviceId");
            return Collections.emptyList();
        }
        return response.getDataList().stream().map(facadeGrpcProfileBuilder::toFacadeBO).toList();
    }

    /**
     * Guard a gRPC result: NOT_FOUND is treated as a normal empty outcome, any other
     * error code throws a service exception.
     *
     * @param result the gRPC result envelope
     * @param op     the operation name, for error messages
     */
    private void guardOrThrow(GrpcR result, String op) {
        String code = result.getCode();
        if (ErrorCode.NOT_FOUND.getCode().equals(code)) {
            log.debug("ProfileGrpcFacade.{} => no resource", op);
            return;
        }
        throw new ServiceException("ProfileFacade." + op + " failed: [" + code + "] " + result.getMessage());
    }

}
