package io.github.pnoker.common.driver.grpc.client;

import io.grpc.stub.ClientCallStreamObserver;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReactiveGrpcClientSupportTest {

    @Test
    void cancellingStreamCancelsUnderlyingGrpcCall() {
        ClientCallStreamObserver<String> call = mock(ClientCallStreamObserver.class);
        var stream = ReactiveGrpcClientSupport.<String, Integer>stream("test stream", observer -> {
            observer.beforeStart(call);
        });

        StepVerifier.create(stream)
                .thenCancel()
                .verify();

        verify(call).cancel("test stream cancelled", null);
    }

    @Test
    void streamMapsTransportFailureToServiceException() {
        var stream = ReactiveGrpcClientSupport.<String, Integer>stream("test stream", observer ->
                observer.onError(io.grpc.Status.UNAVAILABLE.withDescription("manager down")
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
