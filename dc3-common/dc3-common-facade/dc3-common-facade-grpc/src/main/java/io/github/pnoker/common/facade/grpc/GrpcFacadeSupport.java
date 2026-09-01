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

import io.github.pnoker.common.facade.grpc.config.GrpcFacadeProperties;
import io.grpc.stub.AbstractStub;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Shared guardrails for blocking gRPC facade calls.
 *
 * @author pnoker
 * @since 2026.5.9
 */
@Component
@RequiredArgsConstructor
public class GrpcFacadeSupport {

    private final GrpcFacadeProperties properties;

    /**
     * Apply the configured deadline to a stub, returning it unchanged when no deadline
     * is configured.
     *
     * @param stub the gRPC stub
     * @param <S>  stub type
     * @return the stub with a deadline applied, or the original stub
     */
    public <S extends AbstractStub<S>> S withDeadline(S stub) {
        long deadlineMs = properties.getDeadlineMs();
        if (deadlineMs <= 0) {
            return stub;
        }
        return stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS);
    }

}
