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

import io.github.pnoker.api.center.manager.CommandApiGrpc;
import io.github.pnoker.api.center.manager.GrpcCommandIdsQuery;
import io.github.pnoker.api.center.manager.GrpcCommandListDTO;
import io.github.pnoker.api.center.manager.GrpcCommandQuery;
import io.github.pnoker.api.center.manager.GrpcOffsetCommandQuery;
import io.github.pnoker.api.center.manager.GrpcOffsetPageCommandDTO;
import io.github.pnoker.api.common.GrpcCommandDTO;
import io.github.pnoker.api.common.OffsetPage;
import io.github.pnoker.common.enums.CallTypeEnum;
import io.github.pnoker.common.enums.CommandTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.manager.grpc.GrpcPageUtil;
import io.github.pnoker.common.manager.grpc.builder.GrpcCommandBuilder;
import io.github.pnoker.common.manager.repository.CommandFilter;
import io.github.pnoker.common.manager.service.ReactiveCommandService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** gRPC server exposing manager command RPCs. */
@Service
@RequiredArgsConstructor
public class ManagerCommandServer extends CommandApiGrpc.CommandApiImplBase {

    private final GrpcCommandBuilder grpcCommandBuilder;
    private final ReactiveCommandService commandService;

    @Override
    public void list(GrpcOffsetCommandQuery request, StreamObserver<GrpcOffsetPageCommandDTO> observer) {
        ReactiveGrpcServerSupport.subscribe(
                Mono.fromSupplier(() -> filter(request))
                        .flatMap(commandService::list)
                        .map(page -> GrpcOffsetPageCommandDTO.newBuilder()
                                .setPage(OffsetPage.newBuilder()
                                        .setOffset(page.offset())
                                        .setLimit(page.limit())
                                        .setTotal(page.total())
                                        .setHasNext(page.hasNext()))
                                .addAllItems(page.items().stream()
                                        .map(grpcCommandBuilder::buildGrpcDTOByBO)
                                        .toList())
                                .build()),
                observer);
    }

    @Override
    public void getById(GrpcCommandQuery request, StreamObserver<GrpcCommandDTO> observer) {
        ReactiveGrpcServerSupport.subscribe(
                commandService
                        .getById(request.getTenantId(), request.getCommandId())
                        .map(grpcCommandBuilder::buildGrpcDTOByBO),
                observer);
    }

    @Override
    public void listByIds(GrpcCommandIdsQuery request, StreamObserver<GrpcCommandListDTO> observer) {
        ReactiveGrpcServerSupport.subscribe(
                commandService
                        .listByIds(request.getTenantId(), request.getCommandIdsList())
                        .map(grpcCommandBuilder::buildGrpcDTOByBO)
                        .collectList()
                        .map(items -> GrpcCommandListDTO.newBuilder()
                                .addAllItems(items)
                                .build()),
                observer);
    }

    private CommandFilter filter(GrpcOffsetCommandQuery request) {
        var page = GrpcPageUtil.require(request.hasPage() ? request.getPage() : null);
        return new CommandFilter(
                request.getTenantId(),
                request.getCommandName(),
                request.getCommandCode(),
                request.hasCommandTypeFlag() ? CommandTypeEnum.ofIndex((byte) request.getCommandTypeFlag()) : null,
                request.hasCallTypeFlag() ? CallTypeEnum.ofIndex((byte) request.getCallTypeFlag()) : null,
                request.hasProfileId() ? request.getProfileId() : null,
                request.hasEnableFlag() ? EnableFlagEnum.ofIndex((byte) request.getEnableFlag()) : null,
                request.hasVersion() ? request.getVersion() : null,
                request.hasDeviceId() ? request.getDeviceId() : null,
                page.offset(),
                page.limit(),
                page.sort());
    }
}
