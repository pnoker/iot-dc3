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
package io.github.pnoker.common.data.grpc.server;

import io.github.pnoker.api.center.data.GrpcPointValueCommandQuery;
import io.github.pnoker.api.center.data.GrpcPointValueDTO;
import io.github.pnoker.api.center.data.GrpcPointValueHistoryQuery;
import io.github.pnoker.api.center.data.GrpcPointValueQuery;
import io.github.pnoker.api.center.data.GrpcPointValueWriteCommand;
import io.github.pnoker.api.center.data.GrpcPointCommandAccepted;
import io.github.pnoker.api.center.data.GrpcPointValueCursorPage;
import io.github.pnoker.api.center.data.GrpcPointVolumeList;
import io.github.pnoker.api.center.data.PointValueApiGrpc;
import io.github.pnoker.common.data.biz.PointCommandService;
import io.github.pnoker.common.data.biz.PointValueService;
import io.github.pnoker.common.data.entity.bo.PointCommandReadBO;
import io.github.pnoker.common.data.entity.bo.PointCommandWriteBO;
import io.github.pnoker.common.enums.PointCommandSourceEnum;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * gRPC server implementation for the PointValue service. Delegates to
 * {@link PointValueService} and {@link PointCommandService}.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Service
@RequiredArgsConstructor
public class PointValueServer extends PointValueApiGrpc.PointValueApiImplBase {

    private final PointValueService pointValueService;

    private final PointCommandService pointCommandService;

    @Override
    public void getLastValue(GrpcPointValueQuery request, StreamObserver<GrpcPointValueDTO> responseObserver) {
        io.github.pnoker.common.entity.query.PointValueQuery query = new io.github.pnoker.common.entity.query.PointValueQuery();
        query.setDeviceId(request.getDeviceId());
        query.setPointId(request.getPointId());
        query.setTenantId(request.getTenantId());
        query.setOffset(0);
        query.setLimit(1);
        ReactiveGrpcServerSupport.subscribe(pointValueService.latest(query)
                .flatMap(page -> page.items().isEmpty()
                        ? reactor.core.publisher.Mono.error(new io.github.pnoker.common.exception.NotFoundException("Point value"))
                        : reactor.core.publisher.Mono.just(toGrpcDTO(page.items().getFirst()))), responseObserver);
    }

    @Override
    public void listHistoryValues(GrpcPointValueHistoryQuery request,
                                  StreamObserver<GrpcPointValueCursorPage> responseObserver) {
        ReactiveGrpcServerSupport.subscribe(pointValueService.history(request.getTenantId(), request.getDeviceId(), request.getPointId(),
                        request.getCursor(), request.getLimit()).map(history -> {
                    GrpcPointValueCursorPage.Builder response = GrpcPointValueCursorPage.newBuilder()
                            .setLimit(request.getLimit()).setHasNext(history.hasNext())
                            .addAllData(history.items().stream().map(this::toGrpcDTO).toList());
                    if (history.nextCursor() != null) response.setNextCursor(history.nextCursor());
                    return response.build();
                }), responseObserver);
    }

    @Override
    public void listSeriesVolumes(io.github.pnoker.api.center.data.GrpcPointVolumeQuery request,
                                  StreamObserver<GrpcPointVolumeList> responseObserver) {
        ReactiveGrpcServerSupport.subscribe(pointValueService.seriesVolumes(request.getTenantId(), java.time.Instant.ofEpochMilli(request.getFromTime()))
                .map(volumes -> {
                    return GrpcPointVolumeList.newBuilder().addAllData(volumes.stream().map(row ->
                                    io.github.pnoker.api.center.data.GrpcPointVolumeDTO.newBuilder()
                                            .setDeviceId(Objects.requireNonNullElse(row.deviceId(), 0L))
                                            .setPointId(Objects.requireNonNullElse(row.pointId(), 0L))
                                            .setCount(row.count()).build()).toList()).build();
                }), responseObserver);
    }

    private GrpcPointValueDTO toGrpcDTO(io.github.pnoker.common.entity.bo.PointValueBO bo) {
        return GrpcPointValueDTO.newBuilder()
                .setId(0)
                .setDeviceId(Objects.requireNonNullElse(bo.getDeviceId(), 0L))
                .setPointId(Objects.requireNonNullElse(bo.getPointId(), 0L))
                .setValue(Objects.requireNonNullElse(bo.getCalValue(), ""))
                .setRawValue(Objects.requireNonNullElse(bo.getRawValue(), ""))
                .setNumValue(Objects.requireNonNullElse(bo.getNumValue(), 0d))
                .setCreateTime(bo.getCreateTime() == null ? 0L : bo.getCreateTime().toEpochSecond(java.time.ZoneOffset.UTC))
                .build();
    }

    @Override
    public void readCommand(GrpcPointValueCommandQuery request, StreamObserver<GrpcPointCommandAccepted> responseObserver) {
        PointCommandReadBO entityBO = new PointCommandReadBO();
        entityBO.setDeviceId(request.getDeviceId());
        entityBO.setPointId(request.getPointId());
        entityBO.setSource(PointCommandSourceEnum.ofIndex((byte) request.getSource()));
        ReactiveGrpcServerSupport.subscribe(pointCommandService.read(request.getTenantId(), entityBO)
                .map(commandId -> GrpcPointCommandAccepted.newBuilder().setCommandId(commandId).build()), responseObserver);
    }

    @Override
    public void writeCommand(GrpcPointValueWriteCommand request, StreamObserver<GrpcPointCommandAccepted> responseObserver) {
        PointCommandWriteBO entityBO = new PointCommandWriteBO();
        entityBO.setDeviceId(request.getDeviceId());
        entityBO.setPointId(request.getPointId());
        entityBO.setValue(request.getValue());
        entityBO.setSource(PointCommandSourceEnum.ofIndex((byte) request.getSource()));
        ReactiveGrpcServerSupport.subscribe(pointCommandService.write(request.getTenantId(), entityBO)
                .map(commandId -> GrpcPointCommandAccepted.newBuilder().setCommandId(commandId).build()), responseObserver);
    }

}
