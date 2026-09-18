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
package io.github.pnoker.common.driver.grpc.client;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.grpc.stub.ClientCallStreamObserver;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class ReactiveGrpcClientSupportTest {

    @Test
    void cancellingStreamCancelsUnderlyingGrpcCall() {
        ClientCallStreamObserver<String> call = mock(ClientCallStreamObserver.class);
        var stream = ReactiveGrpcClientSupport.<String, Integer>stream("test stream", observer -> {
            observer.beforeStart(call);
        });

        StepVerifier.create(stream).thenCancel().verify();

        verify(call).cancel("test stream cancelled", null);
    }

    @Test
    void streamMapsTransportFailureToServiceException() {
        var stream = ReactiveGrpcClientSupport.<String, Integer>stream(
                "test stream",
                observer -> observer.onError(io.grpc.Status.UNAVAILABLE
                        .withDescription("manager down")
                        .asRuntimeException()));

        StepVerifier.create(stream)
                .expectErrorSatisfies(error -> {
                    org.assertj.core.api.Assertions.assertThat(error)
                            .isInstanceOf(io.github.pnoker.common.exception.ServiceException.class)
                            .hasMessageContaining("UNAVAILABLE")
                            .hasMessageContaining("manager down");
                })
                .verify();
    }
}
