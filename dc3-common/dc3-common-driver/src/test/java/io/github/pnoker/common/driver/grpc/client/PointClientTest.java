package io.github.pnoker.common.driver.grpc.client;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
                .setPage(OffsetPage.newBuilder().setOffset(0).setLimit(200).setTotal(1).setHasNext(false))
                .addItems(GrpcPointDTO.getDefaultInstance())
                .build();
        doAnswer(invocation -> {
            StreamObserver<GrpcOffsetPagePointDTO> observer = invocation.getArgument(1);
            observer.onNext(response);
            observer.onCompleted();
            return null;
        }).when(stub).list(any(), any());

        StepVerifier.create(client.list())
                .expectNext(point)
                .verifyComplete();
    }
}
