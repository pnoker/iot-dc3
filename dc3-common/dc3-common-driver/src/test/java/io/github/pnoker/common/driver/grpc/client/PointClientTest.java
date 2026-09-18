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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.pnoker.api.common.GrpcPointDTO;
import io.github.pnoker.api.common.OffsetPage;
import io.github.pnoker.api.common.driver.GrpcOffsetPagePointDTO;
import io.github.pnoker.api.common.driver.PointApiGrpc;
import io.github.pnoker.common.driver.entity.bo.DriverBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.entity.builder.PointBuilder;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class PointClientTest {

    @Test
    void listUsesOffsetPagination() {
        PointApiGrpc.PointApiStub stub = mock(PointApiGrpc.PointApiStub.class);
        PointBuilder pointBuilder = mock(PointBuilder.class);
        PointBO point = new PointBO();
        when(pointBuilder.buildDTOByGrpcDTO(any())).thenReturn(point);
        DriverMetadata metadata = new DriverMetadata();
        DriverBO driver = new DriverBO();
        driver.setId(2L);
        driver.setTenantId(1L);
        metadata.setDriver(driver);
        PointClient client = new PointClient(stub, metadata, pointBuilder);
        GrpcOffsetPagePointDTO response = GrpcOffsetPagePointDTO.newBuilder()
                .setPage(OffsetPage.newBuilder()
                        .setOffset(0)
                        .setLimit(200)
                        .setTotal(1)
                        .setHasNext(false))
                .addItems(GrpcPointDTO.getDefaultInstance())
                .build();
        doAnswer(invocation -> {
                    StreamObserver<GrpcOffsetPagePointDTO> observer = invocation.getArgument(1);
                    observer.onNext(response);
                    observer.onCompleted();
                    return null;
                })
                .when(stub)
                .list(any(), any());

        StepVerifier.create(client.list()).expectNext(point).verifyComplete();
    }
}
