package io.github.pnoker.common.manager.grpc.server.manager;

import io.github.pnoker.api.center.manager.GrpcOffsetPageProfileDTO;
import io.github.pnoker.api.center.manager.GrpcOffsetProfileQuery;
import io.github.pnoker.api.common.GrpcProfileDTO;
import io.github.pnoker.api.common.PageRequest;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.grpc.builder.GrpcProfileBuilder;
import io.github.pnoker.common.manager.service.ReactiveProfileService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ManagerProfileServerTest {

    @Test
    void listOffsetUsesReactiveServiceAndReturnsCanonicalPage() {
        GrpcProfileBuilder builder = mock(GrpcProfileBuilder.class);
        ReactiveProfileService reactiveService = mock(ReactiveProfileService.class);
        ProfileBO profile = new ProfileBO();
        profile.setId(7L);
        GrpcProfileDTO dto = GrpcProfileDTO.newBuilder().build();
        when(builder.buildGrpcDTOByBO(profile)).thenReturn(dto);
        when(reactiveService.list(any())).thenReturn(reactor.core.publisher.Mono.just(
                OffsetPage.of(java.util.List.of(profile), 10, 20, 31)));
        ManagerProfileServer server = new ManagerProfileServer(builder, reactiveService);
        @SuppressWarnings("unchecked") StreamObserver<GrpcOffsetPageProfileDTO> observer = mock(StreamObserver.class);

        server.list(GrpcOffsetProfileQuery.newBuilder().setTenantId(3L)
                .setPage(PageRequest.newBuilder().setOffset(10).setLimit(20).build()).build(), observer);

        verify(reactiveService).list(any());
        verify(observer).onNext(any(GrpcOffsetPageProfileDTO.class));
        verify(observer).onCompleted();
        verify(observer, never()).onError(any());
        var response = getValue(observer);
        assertThat(response.getPage().getOffset()).isEqualTo(10);
        assertThat(response.getPage().getLimit()).isEqualTo(20);
        assertThat(response.getPage().getTotal()).isEqualTo(31);
        assertThat(response.getPage().getHasNext()).isTrue();
        assertThat(response.getItemsList()).hasSize(1);
    }

    @SuppressWarnings("unchecked")
    private GrpcOffsetPageProfileDTO getValue(StreamObserver<GrpcOffsetPageProfileDTO> observer) {
        var captor = org.mockito.ArgumentCaptor.forClass(GrpcOffsetPageProfileDTO.class);
        verify(observer, atLeastOnce()).onNext(captor.capture());
        return captor.getValue();
    }
}
