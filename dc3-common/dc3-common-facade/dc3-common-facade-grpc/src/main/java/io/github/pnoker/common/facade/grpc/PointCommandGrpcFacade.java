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

import io.github.pnoker.api.center.data.GrpcPointCommandAccepted;
import io.github.pnoker.api.center.data.GrpcPointValueCommandQuery;
import io.github.pnoker.api.center.data.GrpcPointValueWriteCommand;
import io.github.pnoker.api.center.data.PointValueApiGrpc;
import io.github.pnoker.common.enums.PointCommandSourceEnum;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.PointCommandFacade;
import io.github.pnoker.common.facade.grpc.config.GrpcFacadeProperties;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Reactive gRPC facade for asynchronous point command submission. */
@Component
@RequiredArgsConstructor
public class PointCommandGrpcFacade implements PointCommandFacade {

    private final PointValueApiGrpc.PointValueApiStub pointValueApiStub;
    private final GrpcFacadeProperties properties;

    @Override
    public Mono<String> submitRead(Long tenantId, Long deviceId, Long pointId) {
        return submitRead(tenantId, deviceId, pointId, PointCommandSourceEnum.HTTP);
    }

    @Override
    public Mono<String> submitRead(Long tenantId, Long deviceId, Long pointId, PointCommandSourceEnum source) {
        GrpcPointValueCommandQuery request = GrpcPointValueCommandQuery.newBuilder()
                .setTenantId(Objects.requireNonNull(tenantId, "tenantId"))
                .setDeviceId(Objects.requireNonNull(deviceId, "deviceId"))
                .setPointId(Objects.requireNonNull(pointId, "pointId"))
                .setSource(source == null ? PointCommandSourceEnum.HTTP.getIndex() : source.getIndex())
                .build();
        return invoke("PointCommandFacade.submitRead", request, true);
    }

    @Override
    public Mono<String> submitWrite(Long tenantId, Long deviceId, Long pointId, String value) {
        return submitWrite(tenantId, deviceId, pointId, value, PointCommandSourceEnum.HTTP);
    }

    @Override
    public Mono<String> submitWrite(
            Long tenantId, Long deviceId, Long pointId, String value, PointCommandSourceEnum source) {
        GrpcPointValueWriteCommand request = GrpcPointValueWriteCommand.newBuilder()
                .setTenantId(Objects.requireNonNull(tenantId, "tenantId"))
                .setDeviceId(Objects.requireNonNull(deviceId, "deviceId"))
                .setPointId(Objects.requireNonNull(pointId, "pointId"))
                .setValue(Objects.requireNonNull(value, "value"))
                .setSource(source == null ? PointCommandSourceEnum.HTTP.getIndex() : source.getIndex())
                .build();
        return invoke("PointCommandFacade.submitWrite", request, false);
    }

    private Mono<String> invoke(String operation, Object request, boolean read) {
        return Mono.create(sink -> {
            StreamObserver<GrpcPointCommandAccepted> observer = new StreamObserver<>() {
                @Override
                public void onNext(GrpcPointCommandAccepted response) {
                    if (!response.getCommandId().isBlank()) sink.success(response.getCommandId());
                    else sink.error(new ServiceException(operation + " returned an empty command id"));
                }

                @Override
                public void onError(Throwable error) {
                    sink.error(Status.fromThrowable(error)
                            .withDescription(operation + " transport failed")
                            .asRuntimeException());
                }

                @Override
                public void onCompleted() {}
            };
            PointValueApiGrpc.PointValueApiStub stub = properties.getDeadlineMs() > 0
                    ? pointValueApiStub.withDeadlineAfter(properties.getDeadlineMs(), TimeUnit.MILLISECONDS)
                    : pointValueApiStub;
            if (read) stub.readCommand((GrpcPointValueCommandQuery) request, observer);
            else stub.writeCommand((GrpcPointValueWriteCommand) request, observer);
        });
    }
}
