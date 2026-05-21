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

import io.github.pnoker.api.center.data.GrpcPointValueHistoryQuery;
import io.github.pnoker.api.center.data.GrpcPointValueQuery;
import io.github.pnoker.api.center.data.GrpcRPointValueDTO;
import io.github.pnoker.api.center.data.GrpcRPointValueStringList;
import io.github.pnoker.api.center.data.PointValueApiGrpc;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.PointValueFacade;
import io.github.pnoker.common.facade.entity.bo.FacadePointValueBO;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcPointValueBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * gRPC implementation: forwards each call to Data Center via
 * {@link PointValueApiGrpc.PointValueApiBlockingStub}.
 * <p>
 * Selected when {@code dc3.facade.mode=grpc} (or unset — grpc is the default in the
 * auto-configuration declaration).
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointValueGrpcFacade implements PointValueFacade {

    private final PointValueApiGrpc.PointValueApiBlockingStub pointValueApiBlockingStub;

    private final FacadeGrpcPointValueBuilder facadeGrpcPointValueBuilder;

    private final GrpcFacadeSupport grpcFacadeSupport;

    @Override
    public FacadePointValueBO lastValue(Long tenantId, Long deviceId, Long pointId) {
        GrpcPointValueQuery request = GrpcPointValueQuery.newBuilder()
                .setDeviceId(deviceId)
                .setPointId(pointId)
                .setTenantId(tenantId)
                .build();
        GrpcRPointValueDTO response = grpcFacadeSupport.call("PointValueFacade.lastValue", pointValueApiBlockingStub,
                stub -> stub.lastValue(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "lastValue");
            return null;
        }
        if (!response.hasData()) {
            return null;
        }
        return facadeGrpcPointValueBuilder.toFacadeBO(response.getData());
    }

    @Override
    public List<String> history(Long tenantId, Long deviceId, Long pointId, int count) {
        GrpcPointValueHistoryQuery request = GrpcPointValueHistoryQuery.newBuilder()
                .setDeviceId(deviceId)
                .setPointId(pointId)
                .setTenantId(tenantId)
                .setCount(count)
                .build();
        GrpcRPointValueStringList response = grpcFacadeSupport.call("PointValueFacade.history", pointValueApiBlockingStub,
                stub -> stub.historyValue(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "history");
            return Collections.emptyList();
        }
        return response.getDataList();
    }

    /**
     * NO_RESOURCE is a normal "not found" signal — swallow and let the caller see null /
     * empty. Any other non-OK code (server error, param error, etc.) escalates to an
     * exception.
     */
    private void guardOrThrow(GrpcR result, String op) {
        String code = result.getCode();
        if (ResponseEnum.NO_RESOURCE.getCode().equals(code)) {
            log.debug("PointValueGrpcFacade.{} => no resource", op);
            return;
        }
        throw new ServiceException("PointValueFacade." + op + " failed: [" + code + "] " + result.getMessage());
    }

}
