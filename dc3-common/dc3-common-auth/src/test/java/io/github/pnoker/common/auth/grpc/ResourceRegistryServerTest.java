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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.api.center.auth.GrpcSyncRequest;
import io.github.pnoker.api.center.auth.GrpcSyncResultDTO;
import io.github.pnoker.common.auth.biz.ReactiveResourceRegistrySyncService;
import io.github.pnoker.common.auth.entity.bo.ResourceRegistrySyncResult;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class ResourceRegistryServerTest {

    @Test
    void emitsDirectResultMessage() {
        ReactiveResourceRegistrySyncService service = mock(ReactiveResourceRegistrySyncService.class);
        when(service.sync(any()))
                .thenReturn(Mono.just(ResourceRegistrySyncResult.builder()
                        .inserted(2)
                        .updated(3)
                        .deleted(4)
                        .unchanged(5)
                        .build()));
        @SuppressWarnings("unchecked")
        StreamObserver<GrpcSyncResultDTO> observer = mock(StreamObserver.class);

        new ResourceRegistryServer(service)
                .sync(GrpcSyncRequest.newBuilder().setServiceName("svc").build(), observer);

        var captor = org.mockito.ArgumentCaptor.forClass(GrpcSyncResultDTO.class);
        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();
        assertThat(captor.getValue().getInserted()).isEqualTo(2);
        assertThat(captor.getValue().getUpdated()).isEqualTo(3);
        assertThat(captor.getValue().getDeleted()).isEqualTo(4);
        assertThat(captor.getValue().getUnchanged()).isEqualTo(5);
    }
}
