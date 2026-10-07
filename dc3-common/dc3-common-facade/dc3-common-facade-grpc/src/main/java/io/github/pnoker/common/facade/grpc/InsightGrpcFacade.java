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

import io.github.pnoker.api.center.data.GrpcInsightCall;
import io.github.pnoker.api.center.data.GrpcInsightResult;
import io.github.pnoker.api.center.data.InsightApiGrpc;
import io.github.pnoker.common.facade.api.InsightFacade;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * gRPC InsightFacade: forwards agent-facing insight invocations to Data Center via
 * {@link InsightApiGrpc}.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InsightGrpcFacade implements InsightFacade {

    private final InsightApiGrpc.InsightApiStub insightApiStub;

    private final GrpcFacadeSupport grpcFacadeSupport;

    @Override
    public Mono<String> invokeReactive(Long tenantId, String operation, String requestJson) {
        if (Objects.isNull(tenantId) || tenantId <= 0 || Objects.isNull(operation) || operation.isBlank()) {
            return Mono.error(new IllegalArgumentException("tenantId and operation are required"));
        }
        GrpcInsightCall request = GrpcInsightCall.newBuilder()
                .setTenantId(tenantId)
                .setOperation(operation)
                .setRequestJson(Objects.requireNonNullElse(requestJson, ""))
                .build();
        return ReactiveGrpcClientSupport.<GrpcInsightCall, GrpcInsightResult>unary(
                        "InsightFacade.invoke",
                        observer ->
                                grpcFacadeSupport.withDeadline(insightApiStub).invoke(request, observer))
                .map(GrpcInsightResult::getResponseJson);
    }
}
