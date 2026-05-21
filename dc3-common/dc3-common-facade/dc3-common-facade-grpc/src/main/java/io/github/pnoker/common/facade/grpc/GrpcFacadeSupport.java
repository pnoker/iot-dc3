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

import lombok.RequiredArgsConstructor;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.grpc.config.GrpcFacadeProperties;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.AbstractStub;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Shared guardrails for blocking gRPC facade calls.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.9
 */
@Component
@RequiredArgsConstructor
public class GrpcFacadeSupport {

    private final GrpcFacadeProperties properties;

    public <S extends AbstractStub<S>, T> T call(String operation, S stub, Function<S, T> invocation) {
        try {
            return invocation.apply(withDeadline(stub));
        } catch (StatusRuntimeException e) {
            Status status = e.getStatus();
            String description = Objects.requireNonNullElse(status.getDescription(), e.getMessage());
            throw new ServiceException("{} transport failed: [{}] {}", operation, status.getCode(), description);
        }
    }

    private <S extends AbstractStub<S>> S withDeadline(S stub) {
        long deadlineMs = properties.getDeadlineMs();
        if (deadlineMs <= 0) {
            return stub;
        }
        return stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS);
    }

}
