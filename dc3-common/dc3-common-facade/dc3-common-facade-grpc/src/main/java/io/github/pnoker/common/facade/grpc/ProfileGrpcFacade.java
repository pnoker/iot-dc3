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
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.ProfileFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeProfileBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeProfileQuery;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcProfileBuilder;
import jakarta.annotation.Resource;
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
public class ProfileGrpcFacade implements ProfileFacade {

    @Resource
    private ProfileApiGrpc.ProfileApiBlockingStub profileApiBlockingStub;

    @Resource
    private FacadeGrpcProfileBuilder facadeGrpcProfileBuilder;

    @Resource
    private GrpcFacadeSupport grpcFacadeSupport;

    @Override
    public FacadeProfileBO selectById(Long id) {
        GrpcProfileQuery request = GrpcProfileQuery.newBuilder().setProfileId(id).build();
        GrpcRProfileDTO response = grpcFacadeSupport.call("ProfileFacade.selectById", profileApiBlockingStub,
                stub -> stub.selectByProfileId(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "selectById");
            return null;
        }
        return facadeGrpcProfileBuilder.toFacadeBO(response.getData());
    }

    @Override
    public List<FacadeProfileBO> selectByIds(Collection<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> profileIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (profileIds.isEmpty()) {
            return Collections.emptyList();
        }

        GrpcProfileIdsQuery request = GrpcProfileIdsQuery.newBuilder().addAllProfileIds(profileIds).build();
        GrpcRProfileListDTO response = grpcFacadeSupport.call("ProfileFacade.selectByIds", profileApiBlockingStub,
                stub -> stub.selectByProfileIds(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "selectByIds");
            return Collections.emptyList();
        }
        return response.getDataList().stream().map(facadeGrpcProfileBuilder::toFacadeBO).toList();
    }

    @Override
    public FacadePage<FacadeProfileBO> selectByPage(FacadeProfileQuery query) {
        GrpcRPageProfileDTO response = grpcFacadeSupport.call("ProfileFacade.selectByPage", profileApiBlockingStub,
                stub -> stub.selectByPage(facadeGrpcProfileBuilder.toGrpcPageQuery(query)));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "selectByPage");
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
    public List<FacadeProfileBO> selectByDeviceId(Long deviceId) {
        GrpcDeviceQuery request = GrpcDeviceQuery.newBuilder().setDeviceId(deviceId).build();
        GrpcRProfileListDTO response = grpcFacadeSupport.call("ProfileFacade.selectByDeviceId", profileApiBlockingStub,
                stub -> stub.selectByDeviceId(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "selectByDeviceId");
            return Collections.emptyList();
        }
        return response.getDataList().stream().map(facadeGrpcProfileBuilder::toFacadeBO).toList();
    }

    private void guardOrThrow(GrpcR result, String op) {
        String code = result.getCode();
        if (ResponseEnum.NO_RESOURCE.getCode().equals(code)) {
            log.debug("ProfileGrpcFacade.{} => no resource", op);
            return;
        }
        throw new ServiceException("ProfileFacade." + op + " failed: [" + code + "] " + result.getMessage());
    }

}
