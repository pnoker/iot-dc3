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

import io.github.pnoker.api.common.PageRequest;
import io.github.pnoker.api.common.GrpcPointDTO;
import io.github.pnoker.api.common.driver.GrpcOffsetPagePointDTO;
import io.github.pnoker.api.common.driver.GrpcOffsetPointQuery;
import io.github.pnoker.api.common.driver.GrpcPointQuery;
import io.github.pnoker.api.common.driver.PointApiGrpc;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.entity.builder.PointBuilder;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * gRPC client used to query point metadata associated with the current driver.
 *
 * @author pnoker
 * @since 2016.10.1
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class PointClient {

    private final PointApiGrpc.PointApiStub pointApiStub;

    private final DriverMetadata driverMetadata;

    private final PointBuilder pointBuilder;

    /**
     * Fetch all points via paginated gRPC calls, looping through every page and
     * accumulating the results.
     *
     * @return all points
     */
    public Flux<PointBO> list() {
        return loadPage(0, 200)
                .expand(page -> page.hasNext()
                        ? loadPage(page.offset() + Math.max(page.limit(), 1), page.limit())
                        : Mono.empty())
                .concatMapIterable(PointPage::data)
                .map(pointBuilder::buildDTOByGrpcDTO);
    }

    /**
     * Performs a gRPC getById lookup scoped to the current tenant and driver,
     * returning null after logging when the response is not OK, otherwise
     * returning a {@link PointBO} built from the gRPC payload.
     *
     * @param id Point ID
     * @return PointBO
     */
    public Mono<PointBO> getById(Long id) {
        return Mono.defer(() -> {
            GrpcPointQuery query = GrpcPointQuery.newBuilder()
                    .setTenantId(driverMetadata.getDriver().getTenantId())
                    .setDriverId(driverMetadata.getDriver().getId())
                    .setPointId(id)
                    .build();
                    return ReactiveGrpcClientSupport.<GrpcPointQuery, GrpcPointDTO>unary("get point metadata",
                            observer -> pointApiStub.getById(query, observer))
                    .map(pointBuilder::buildDTOByGrpcDTO);
        });
    }

    private Mono<PointPage> loadPage(long offset, int limit) {
        GrpcOffsetPointQuery query = GrpcOffsetPointQuery.newBuilder()
                .setTenantId(driverMetadata.getDriver().getTenantId())
                .setDriverId(driverMetadata.getDriver().getId())
                .setPage(PageRequest.newBuilder().setOffset(offset).setLimit(limit).build())
                .build();
        return ReactiveGrpcClientSupport.<GrpcOffsetPointQuery, GrpcOffsetPagePointDTO>unary(
                        "list point metadata", observer -> pointApiStub.list(query, observer))
                .map(response -> new PointPage(response.getPage().getOffset(), response.getPage().getLimit(),
                        response.getPage().getHasNext(), response.getItemsList()));
    }

    private record PointPage(long offset, int limit, boolean hasNext, List<GrpcPointDTO> data) {
    }

}
