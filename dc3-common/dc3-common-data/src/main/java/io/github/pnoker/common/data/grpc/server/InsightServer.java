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

import io.github.pnoker.api.center.data.GrpcInsightCall;
import io.github.pnoker.api.center.data.GrpcInsightResult;
import io.github.pnoker.api.center.data.InsightApiGrpc;
import io.github.pnoker.common.data.biz.insight.InsightDispatcher;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Non-blocking gRPC server exposing the agent-facing insight channel.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InsightServer extends InsightApiGrpc.InsightApiImplBase {

    private final InsightDispatcher insightDispatcher;

    private final ObjectMapper objectMapper;

    @Override
    public void invoke(GrpcInsightCall request, StreamObserver<GrpcInsightResult> responseObserver) {
        if (request.getTenantId() <= 0 || request.getOperation().isBlank()) {
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                    .withDescription("tenant_id and operation are required")
                    .asRuntimeException());
            return;
        }
        ReactiveGrpcServerSupport.subscribe(
                insightDispatcher
                        .invoke(request.getTenantId(), request.getOperation(), request.getRequestJson())
                        .map(payload -> GrpcInsightResult.newBuilder()
                                .setResponseJson(objectMapper.writeValueAsString(payload))
                                .build()),
                responseObserver);
    }
}
