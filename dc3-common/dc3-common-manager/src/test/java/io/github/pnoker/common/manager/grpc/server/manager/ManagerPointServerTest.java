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
package io.github.pnoker.common.manager.grpc.server.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.pnoker.api.center.manager.GrpcOffsetPagePointDTO;
import io.github.pnoker.api.center.manager.GrpcOffsetPointQuery;
import io.github.pnoker.api.common.GrpcPointDTO;
import io.github.pnoker.api.common.PageRequest;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.grpc.builder.GrpcPointBuilder;
import io.github.pnoker.common.manager.service.ReactivePointService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

class ManagerPointServerTest {

    @Test
    void listOffsetUsesReactiveServiceAndReturnsCanonicalPage() {
        GrpcPointBuilder builder = mock(GrpcPointBuilder.class);
        ReactivePointService reactiveService = mock(ReactivePointService.class);
        PointBO point = new PointBO();
        point.setId(7L);
        GrpcPointDTO dto = GrpcPointDTO.newBuilder().build();
        when(builder.buildGrpcDTOByBO(point)).thenReturn(dto);
        when(reactiveService.list(any()))
                .thenReturn(reactor.core.publisher.Mono.just(OffsetPage.of(java.util.List.of(point), 10, 20, 31)));
        ManagerPointServer server = new ManagerPointServer(builder, reactiveService);
        @SuppressWarnings("unchecked")
        StreamObserver<GrpcOffsetPagePointDTO> observer = mock(StreamObserver.class);

        server.list(
                GrpcOffsetPointQuery.newBuilder()
                        .setTenantId(3L)
                        .setPage(PageRequest.newBuilder()
                                .setOffset(10)
                                .setLimit(20)
                                .build())
                        .build(),
                observer);

        verify(reactiveService).list(any());
        verify(observer).onNext(any(GrpcOffsetPagePointDTO.class));
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
    private GrpcOffsetPagePointDTO getValue(StreamObserver<GrpcOffsetPagePointDTO> observer) {
        var captor = org.mockito.ArgumentCaptor.forClass(GrpcOffsetPagePointDTO.class);
        verify(observer, atLeastOnce()).onNext(captor.capture());
        return captor.getValue();
    }
}
