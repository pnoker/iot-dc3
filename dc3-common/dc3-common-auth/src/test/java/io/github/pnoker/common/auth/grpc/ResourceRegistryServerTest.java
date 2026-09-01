package io.github.pnoker.common.auth.grpc;

import io.github.pnoker.api.center.auth.GrpcSyncRequest;
import io.github.pnoker.api.center.auth.GrpcSyncResultDTO;
import io.github.pnoker.common.auth.biz.ReactiveResourceRegistrySyncService;
import io.github.pnoker.common.auth.entity.bo.ResourceRegistrySyncResult;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceRegistryServerTest {

    @Test
    void emitsDirectResultMessage() {
        ReactiveResourceRegistrySyncService service = mock(ReactiveResourceRegistrySyncService.class);
        when(service.sync(any())).thenReturn(Mono.just(ResourceRegistrySyncResult.builder()
                .inserted(2).updated(3).deleted(4).unchanged(5).build()));
        @SuppressWarnings("unchecked")
        StreamObserver<GrpcSyncResultDTO> observer = mock(StreamObserver.class);

        new ResourceRegistryServer(service).sync(GrpcSyncRequest.newBuilder().setServiceName("svc").build(), observer);

        var captor = org.mockito.ArgumentCaptor.forClass(GrpcSyncResultDTO.class);
        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();
        assertThat(captor.getValue().getInserted()).isEqualTo(2);
        assertThat(captor.getValue().getUpdated()).isEqualTo(3);
        assertThat(captor.getValue().getDeleted()).isEqualTo(4);
        assertThat(captor.getValue().getUnchanged()).isEqualTo(5);
    }
}
