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

import io.github.pnoker.api.center.auth.GrpcLoginQuery;
import io.github.pnoker.api.center.auth.GrpcRTokenDTO;
import io.github.pnoker.api.center.auth.TokenApiGrpc;
import io.github.pnoker.common.auth.biz.TokenService;
import io.github.pnoker.common.auth.entity.bean.TokenValid;
import io.github.pnoker.common.enums.ResponseEnum;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServerTest {

    @Mock
    private TokenService tokenService;

    private Server server;
    private ManagedChannel channel;
    private TokenApiGrpc.TokenApiBlockingStub stub;

    @BeforeEach
    void setUp() throws Exception {
        TokenServer tokenServer = new TokenServer(tokenService);

        String name = "dc3-token-" + UUID.randomUUID();
        server = InProcessServerBuilder.forName(name).directExecutor().addService(tokenServer).build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        stub = TokenApiGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.shutdownNow();
        }
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Test
    void checkValidReportsOkWithExpiryDataForValidToken() {
        TokenValid valid = new TokenValid(true, new Date(1_700_000_000_000L));
        when(tokenService.checkValid("alice", "salt", "token", "tenant-A")).thenReturn(valid);

        GrpcRTokenDTO response = stub.checkValid(GrpcLoginQuery.newBuilder()
                .setTenant("tenant-A")
                .setName("alice")
                .setSalt("salt")
                .setToken("token")
                .build());

        assertThat(response.getResult().getOk()).isTrue();
        assertThat(response.getResult().getCode()).isEqualTo(ResponseEnum.OK.getCode());
        assertThat(response.getData()).matches("\\d{4}-\\d{2}-\\d{2} .*");
    }

    @Test
    void checkValidReportsTokenInvalidEnvelopeForInvalidToken() {
        when(tokenService.checkValid("alice", "salt", "token", "tenant-A"))
                .thenReturn(new TokenValid(false, null));

        GrpcRTokenDTO response = stub.checkValid(GrpcLoginQuery.newBuilder()
                .setTenant("tenant-A")
                .setName("alice")
                .setSalt("salt")
                .setToken("token")
                .build());

        assertThat(response.getResult().getOk()).isFalse();
        assertThat(response.getResult().getCode()).isEqualTo(ResponseEnum.TOKEN_INVALID.getCode());
        assertThat(response.getData()).isEmpty();
    }

    @Test
    void checkValidReportsNoResourceWhenServiceReturnsNull() {
        when(tokenService.checkValid("alice", "salt", "token", "tenant-A")).thenReturn(null);

        GrpcRTokenDTO response = stub.checkValid(GrpcLoginQuery.newBuilder()
                .setTenant("tenant-A")
                .setName("alice")
                .setSalt("salt")
                .setToken("token")
                .build());

        assertThat(response.getResult().getOk()).isFalse();
        assertThat(response.getResult().getCode()).isEqualTo(ResponseEnum.NO_RESOURCE.getCode());
    }
}
