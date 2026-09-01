package io.github.pnoker.common.auth.grpc;

import io.github.pnoker.api.center.auth.GrpcIdQuery;
import io.github.pnoker.api.center.auth.GrpcUserDTO;
import io.github.pnoker.api.center.auth.UserApiGrpc;
import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.grpc.builder.GrpcUserBuilder;
import io.github.pnoker.common.auth.service.ReactiveUserService;
import io.github.pnoker.common.exception.NotFoundException;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServerTest {

    private final ReactiveUserService service = mock(ReactiveUserService.class);
    private final GrpcUserBuilder builder = mock(GrpcUserBuilder.class);
    private Server server;
    private ManagedChannel channel;
    private UserApiGrpc.UserApiBlockingStub stub;

    @BeforeEach
    void setUp() throws Exception {
        String name = "user-" + UUID.randomUUID();
        server = InProcessServerBuilder.forName(name).directExecutor()
                .addService(new UserServer(builder, service)).build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        stub = UserApiGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void getByIdUsesTenantScopedReactiveService() {
        UserBO user = new UserBO();
        when(service.getById(7L, 11L)).thenReturn(Mono.just(user));
        when(builder.buildGrpcDTOByBO(user)).thenReturn(
                io.github.pnoker.api.center.auth.GrpcUserDTO.newBuilder().setUserName("alice").build());

        GrpcUserDTO response = stub.getById(GrpcIdQuery.newBuilder().setTenantId(7L).setId(11L).build());

        assertThat(response.getUserName()).isEqualTo("alice");
        verify(service).getById(7L, 11L);
    }

    @Test
    void getByPrincipalIdMapsNotFoundToStatus() {
        when(service.getByPrincipalId(7L, 22L)).thenReturn(Mono.error(new NotFoundException("User")));

        assertThatThrownBy(() -> stub.getByPrincipalId(GrpcIdQuery.newBuilder()
                .setTenantId(7L).setId(22L).build()))
                .hasMessageContaining("NOT_FOUND");
    }

    @Test
    void getByIdMapsServiceFailureToInternalStatus() {
        when(service.getById(7L, 11L)).thenReturn(Mono.error(new IllegalStateException("boom")));

        assertThatThrownBy(() -> stub.getById(GrpcIdQuery.newBuilder().setTenantId(7L).setId(11L).build()))
                .hasMessageContaining("INTERNAL");
    }
}
