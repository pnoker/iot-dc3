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
package io.github.pnoker.common.auth.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.api.center.auth.GrpcLocalCredentialDTO;
import io.github.pnoker.api.center.auth.GrpcLoginNameQuery;
import io.github.pnoker.api.center.auth.LocalCredentialApiGrpc;
import io.github.pnoker.common.auth.entity.bo.LocalCredentialBO;
import io.github.pnoker.common.auth.grpc.builder.GrpcLocalCredentialBuilder;
import io.github.pnoker.common.auth.service.ReactiveLocalCredentialService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class LocalCredentialServerTest {

    private final ReactiveLocalCredentialService service = mock(ReactiveLocalCredentialService.class);
    private final GrpcLocalCredentialBuilder builder = mock(GrpcLocalCredentialBuilder.class);
    private Server server;
    private ManagedChannel channel;
    private LocalCredentialApiGrpc.LocalCredentialApiBlockingStub stub;

    @BeforeEach
    void setUp() throws Exception {
        String name = "local-credential-" + UUID.randomUUID();
        server = InProcessServerBuilder.forName(name)
                .directExecutor()
                .addService(new LocalCredentialServer(builder, service))
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        stub = LocalCredentialApiGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void getByLoginNameUsesTenantScopedReactiveService() {
        LocalCredentialBO credential = new LocalCredentialBO();
        when(service.getByLoginName(7L, "alice")).thenReturn(Mono.just(credential));
        when(builder.buildGrpcDTOByBO(credential))
                .thenReturn(io.github.pnoker.api.center.auth.GrpcLocalCredentialDTO.newBuilder()
                        .setLoginName("alice")
                        .build());

        GrpcLocalCredentialDTO response = stub.getByLoginName(GrpcLoginNameQuery.newBuilder()
                .setTenantId(7L)
                .setLoginName("alice")
                .build());

        assertThat(response.getLoginName()).isEqualTo("alice");
        verify(service).getByLoginName(7L, "alice");
    }

    @Test
    void getByLoginNameMapsEmptyToNotFoundStatus() {
        when(service.getByLoginName(7L, "missing")).thenReturn(Mono.empty());

        assertThatThrownBy(() -> stub.getByLoginName(GrpcLoginNameQuery.newBuilder()
                        .setTenantId(7L)
                        .setLoginName("missing")
                        .build()))
                .hasMessageContaining("NOT_FOUND");
    }

    @Test
    void getByLoginNameMapsServiceFailureToInternalStatus() {
        when(service.getByLoginName(7L, "alice")).thenReturn(Mono.error(new IllegalStateException("boom")));

        assertThatThrownBy(() -> stub.getByLoginName(GrpcLoginNameQuery.newBuilder()
                        .setTenantId(7L)
                        .setLoginName("alice")
                        .build()))
                .hasMessageContaining("INTERNAL");
    }
}
