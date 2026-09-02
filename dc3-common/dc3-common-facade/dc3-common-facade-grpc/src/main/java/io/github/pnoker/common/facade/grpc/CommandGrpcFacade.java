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
package io.github.pnoker.common.facade.grpc;

import io.github.pnoker.api.center.manager.CommandApiGrpc;
import io.github.pnoker.api.center.manager.GrpcCommandIdsQuery;
import io.github.pnoker.api.center.manager.GrpcCommandListDTO;
import io.github.pnoker.api.center.manager.GrpcCommandQuery;
import io.github.pnoker.api.center.manager.GrpcOffsetCommandQuery;
import io.github.pnoker.api.center.manager.GrpcOffsetPageCommandDTO;
import io.github.pnoker.api.common.GrpcCommandDTO;
import io.github.pnoker.common.facade.api.CommandFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandBO;
import io.github.pnoker.common.facade.entity.query.FacadeCommandOffsetQuery;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcCommandBuilder;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CommandGrpcFacade implements CommandFacade {

    private final CommandApiGrpc.CommandApiStub commandApiStub;
    private final FacadeGrpcCommandBuilder commandBuilder;
    private final GrpcFacadeSupport grpcFacadeSupport;

    @Override
    public Mono<FacadeCommandBO> getById(Long tenantId, Long id) {
        if (tenantId == null || id == null) {
            return Mono.empty();
        }
        GrpcCommandQuery request = GrpcCommandQuery.newBuilder()
                .setTenantId(tenantId)
                .setCommandId(id)
                .build();
        return ReactiveGrpcClientSupport.<GrpcCommandQuery, GrpcCommandDTO>unary(
                        "CommandFacade.getById",
                        observer ->
                                grpcFacadeSupport.withDeadline(commandApiStub).getById(request, observer))
                .map(commandBuilder::toFacadeBO);
    }

    @Override
    public Flux<FacadeCommandBO> listByIds(Long tenantId, Collection<Long> ids) {
        List<Long> values = ids == null
                ? List.of()
                : ids.stream().filter(Objects::nonNull).distinct().toList();
        if (tenantId == null || values.isEmpty()) {
            return Flux.empty();
        }
        GrpcCommandIdsQuery request = GrpcCommandIdsQuery.newBuilder()
                .setTenantId(tenantId)
                .addAllCommandIds(values)
                .build();
        return ReactiveGrpcClientSupport.<GrpcCommandIdsQuery, GrpcCommandListDTO>unary(
                        "CommandFacade.listByIds",
                        observer ->
                                grpcFacadeSupport.withDeadline(commandApiStub).listByIds(request, observer))
                .flatMapMany(response -> Flux.fromIterable(response.getItemsList()))
                .map(commandBuilder::toFacadeBO);
    }

    @Override
    public Mono<OffsetPage<FacadeCommandBO>> list(FacadeCommandOffsetQuery query) {
        var request = commandBuilder.toGrpcOffsetQuery(query);
        return ReactiveGrpcClientSupport.<GrpcOffsetCommandQuery, GrpcOffsetPageCommandDTO>unary(
                        "CommandFacade.list",
                        observer ->
                                grpcFacadeSupport.withDeadline(commandApiStub).list(request, observer))
                .map(response -> OffsetPage.of(
                        response.getItemsList().stream()
                                .map(commandBuilder::toFacadeBO)
                                .toList(),
                        response.getPage().getOffset(),
                        response.getPage().getLimit(),
                        response.getPage().getTotal()));
    }
}
