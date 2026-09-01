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
import io.github.pnoker.api.center.data.GrpcPointVolumeQuery;
import io.github.pnoker.api.center.data.GrpcPointValueDTO;
import io.github.pnoker.api.center.data.GrpcPointValueCursorPage;
import io.github.pnoker.api.center.data.GrpcPointVolumeList;
import io.github.pnoker.api.center.data.PointValueApiGrpc;
import io.github.pnoker.common.facade.api.PointValueFacade;
import io.github.pnoker.common.facade.entity.bo.FacadePointValueBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointVolumeBO;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcPointValueBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import io.github.pnoker.db.r2dbc.core.page.CursorPage;

import java.util.concurrent.TimeUnit;

import java.util.List;

/**
 * gRPC implementation: forwards each call to Data Center via
 * <p>
 * Selected when {@code dc3.facade.data.mode=grpc} (or unset — grpc is the default in the
 * auto-configuration declaration).
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointValueGrpcFacade implements PointValueFacade {

    private final PointValueApiGrpc.PointValueApiStub pointValueApiStub;

    private final io.github.pnoker.common.facade.grpc.config.GrpcFacadeProperties properties;

    private final FacadeGrpcPointValueBuilder facadeGrpcPointValueBuilder;

    @Override
    public Mono<FacadePointValueBO> lastValue(Long tenantId, Long deviceId, Long pointId) {
        GrpcPointValueQuery request = GrpcPointValueQuery.newBuilder().setDeviceId(deviceId).setPointId(pointId)
                .setTenantId(tenantId).build();
        return ReactiveGrpcClientSupport.<GrpcPointValueQuery, GrpcPointValueDTO>
                unary("getLastValue", observer -> deadlineStub().getLastValue(request, observer))
                .map(facadeGrpcPointValueBuilder::toFacadeBO);
    }

    @Override
    public Mono<CursorPage<FacadePointValueBO>> history(Long tenantId, Long deviceId, Long pointId,
                                                        String cursor, int limit) {
        GrpcPointValueHistoryQuery request = GrpcPointValueHistoryQuery.newBuilder().setDeviceId(deviceId)
                .setPointId(pointId).setTenantId(tenantId).setCursor(cursor == null ? "" : cursor)
                .setLimit(limit).build();
        return ReactiveGrpcClientSupport.<GrpcPointValueHistoryQuery, GrpcPointValueCursorPage>
                unary("listHistoryValues", observer -> deadlineStub().listHistoryValues(request, observer))
                .map(response -> CursorPage.of(response.getDataList().stream()
                            .map(facadeGrpcPointValueBuilder::toFacadeBO).toList(),
                            response.getHasNext() ? response.getNextCursor() : null));
    }

    @Override
    public Mono<List<FacadePointVolumeBO>> pointVolumes(Long tenantId, long fromEpochMillis) {
        GrpcPointVolumeQuery request = GrpcPointVolumeQuery.newBuilder().setTenantId(tenantId)
                .setFromTime(fromEpochMillis).build();
        return ReactiveGrpcClientSupport.<GrpcPointVolumeQuery, GrpcPointVolumeList>
                unary("listSeriesVolumes", observer -> deadlineStub().listSeriesVolumes(request, observer))
                .map(response -> response.getDataList().stream()
                            .map(row -> new FacadePointVolumeBO(row.getDeviceId(), row.getPointId(), row.getCount()))
                            .toList());
    }

    private PointValueApiGrpc.PointValueApiStub deadlineStub() {
        return properties.getDeadlineMs() > 0
                ? pointValueApiStub.withDeadlineAfter(properties.getDeadlineMs(), TimeUnit.MILLISECONDS)
                : pointValueApiStub;
    }

}
