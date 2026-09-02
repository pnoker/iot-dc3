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
package io.github.pnoker.common.manager.grpc.server.driver;

import io.github.pnoker.api.common.GrpcDriverQuery;
import io.github.pnoker.api.common.driver.DriverApiGrpc;
import io.github.pnoker.api.common.driver.GrpcDeviceLeaseDTO;
import io.github.pnoker.api.common.driver.GrpcDriverLeaseDTO;
import io.github.pnoker.api.common.driver.GrpcDriverLeaseRequest;
import io.github.pnoker.api.common.driver.GrpcDriverRegisterDTO;
import io.github.pnoker.api.common.driver.GrpcDriverRegistrationDTO;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.biz.DriverLeaseService;
import io.github.pnoker.common.manager.biz.ReactiveDriverRegisterService;
import io.github.pnoker.common.manager.entity.bo.DeviceLeaseBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.DriverLeaseGrantBO;
import io.github.pnoker.common.manager.grpc.builder.GrpcCommandAttributeBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcDriverAttributeBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcDriverBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcEventAttributeBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcPointAttributeBuilder;
import io.github.pnoker.common.manager.grpc.server.manager.ReactiveGrpcServerSupport;
import io.github.pnoker.common.manager.service.ReactiveCommandAttributeService;
import io.github.pnoker.common.manager.service.ReactiveDriverAttributeService;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.common.manager.service.ReactiveEventAttributeService;
import io.github.pnoker.common.manager.service.ReactivePointAttributeService;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive gRPC server handling driver-to-manager driver requests. */
@Slf4j
@Service
public class DriverDriverServer extends DriverApiGrpc.DriverApiImplBase {

    private static final int ASSIGNMENT_BATCH_SIZE = 1_000;

    private final GrpcDriverBuilder grpcDriverBuilder;
    private final GrpcDriverAttributeBuilder grpcDriverAttributeBuilder;
    private final GrpcPointAttributeBuilder grpcPointAttributeBuilder;
    private final GrpcCommandAttributeBuilder grpcCommandAttributeBuilder;
    private final GrpcEventAttributeBuilder grpcEventAttributeBuilder;
    private final ReactiveDriverRegisterService reactiveRegisterService;
    private final ReactiveDriverService reactiveDriverService;
    private final ReactiveDriverAttributeService reactiveDriverAttributeService;
    private final ReactivePointAttributeService reactivePointAttributeService;
    private final ReactiveCommandAttributeService reactiveCommandAttributeService;
    private final ReactiveEventAttributeService reactiveEventAttributeService;
    private final DriverLeaseService driverLeaseService;

    @Autowired
    public DriverDriverServer(
            GrpcDriverBuilder grpcDriverBuilder,
            GrpcDriverAttributeBuilder grpcDriverAttributeBuilder,
            GrpcPointAttributeBuilder grpcPointAttributeBuilder,
            GrpcCommandAttributeBuilder grpcCommandAttributeBuilder,
            GrpcEventAttributeBuilder grpcEventAttributeBuilder,
            ReactiveDriverRegisterService reactiveRegisterService,
            ReactiveDriverService reactiveDriverService,
            ReactiveDriverAttributeService reactiveDriverAttributeService,
            ReactivePointAttributeService reactivePointAttributeService,
            ReactiveCommandAttributeService reactiveCommandAttributeService,
            ReactiveEventAttributeService reactiveEventAttributeService,
            DriverLeaseService driverLeaseService) {
        this.grpcDriverBuilder = grpcDriverBuilder;
        this.grpcDriverAttributeBuilder = grpcDriverAttributeBuilder;
        this.grpcPointAttributeBuilder = grpcPointAttributeBuilder;
        this.grpcCommandAttributeBuilder = grpcCommandAttributeBuilder;
        this.grpcEventAttributeBuilder = grpcEventAttributeBuilder;
        this.reactiveRegisterService = reactiveRegisterService;
        this.reactiveDriverService = reactiveDriverService;
        this.reactiveDriverAttributeService = reactiveDriverAttributeService;
        this.reactivePointAttributeService = reactivePointAttributeService;
        this.reactiveCommandAttributeService = reactiveCommandAttributeService;
        this.reactiveEventAttributeService = reactiveEventAttributeService;
        this.driverLeaseService = driverLeaseService;
    }

    @Override
    public void driverRegister(GrpcDriverRegisterDTO request, StreamObserver<GrpcDriverRegistrationDTO> observer) {
        ReactiveGrpcServerSupport.subscribe(
                reactiveRegisterService.register(request).map(this::registrationResponse), observer);
    }

    @Override
    public void renewLease(GrpcDriverLeaseRequest request, StreamObserver<GrpcDriverLeaseDTO> observer) {
        Flux<GrpcDriverLeaseDTO> response = driverLeaseService
                .renew(
                        request.getTenantId(),
                        request.getDriverId(),
                        request.getNode(),
                        request.getClient(),
                        request.getHost(),
                        request.getLeaseSeconds(),
                        request.getAssignmentVersion())
                .flatMapMany(grant -> grant.assignmentsChanged()
                        ? streamAssignmentSnapshot(request, grant)
                        : Flux.just(leaseResponse(grant, List.of(), true)));
        ReactiveGrpcServerSupport.subscribe(response, observer);
    }

    @Override
    public void getById(GrpcDriverQuery request, StreamObserver<GrpcDriverRegistrationDTO> observer) {
        Mono<GrpcDriverRegistrationDTO> response = reactiveDriverService
                .getById(request.getTenantId(), request.getDriverId())
                .flatMap(this::metadataResponse)
                .switchIfEmpty(Mono.error(new NotFoundException("Driver")));
        ReactiveGrpcServerSupport.subscribe(response, observer);
    }

    private GrpcDriverRegistrationDTO registrationResponse(ReactiveDriverRegisterService.Registration registration) {
        return GrpcDriverRegistrationDTO.newBuilder()
                .setDriver(grpcDriverBuilder.buildGrpcDTOByBO(registration.driver()))
                .addAllDriverAttributes(registration.driverAttributes().stream()
                        .map(grpcDriverAttributeBuilder::buildGrpcDTOByBO)
                        .toList())
                .addAllPointAttributes(registration.pointAttributes().stream()
                        .map(grpcPointAttributeBuilder::buildGrpcDTOByBO)
                        .toList())
                .addAllCommandAttributes(registration.commandAttributes().stream()
                        .map(grpcCommandAttributeBuilder::buildGrpcDTOByBO)
                        .toList())
                .addAllEventAttributes(registration.eventAttributes().stream()
                        .map(grpcEventAttributeBuilder::buildGrpcDTOByBO)
                        .toList())
                .build();
    }

    private Mono<GrpcDriverRegistrationDTO> metadataResponse(DriverBO driver) {
        return Mono.zip(
                        reactiveDriverAttributeService
                                .listByDriverId(driver.getTenantId(), driver.getId())
                                .collectList(),
                        reactivePointAttributeService
                                .listByDriverId(driver.getTenantId(), driver.getId())
                                .collectList(),
                        reactiveCommandAttributeService
                                .listByDriverId(driver.getTenantId(), driver.getId())
                                .collectList(),
                        reactiveEventAttributeService
                                .listByDriverId(driver.getTenantId(), driver.getId())
                                .collectList())
                .map(tuple -> GrpcDriverRegistrationDTO.newBuilder()
                        .setDriver(grpcDriverBuilder.buildGrpcDTOByBO(driver))
                        .addAllDriverAttributes(tuple.getT1().stream()
                                .filter(value -> Objects.equals(driver.getTenantId(), value.getTenantId()))
                                .map(grpcDriverAttributeBuilder::buildGrpcDTOByBO)
                                .toList())
                        .addAllPointAttributes(tuple.getT2().stream()
                                .filter(value -> Objects.equals(driver.getTenantId(), value.getTenantId()))
                                .map(grpcPointAttributeBuilder::buildGrpcDTOByBO)
                                .toList())
                        .addAllCommandAttributes(tuple.getT3().stream()
                                .filter(value -> Objects.equals(driver.getTenantId(), value.getTenantId()))
                                .map(grpcCommandAttributeBuilder::buildGrpcDTOByBO)
                                .toList())
                        .addAllEventAttributes(tuple.getT4().stream()
                                .filter(value -> Objects.equals(driver.getTenantId(), value.getTenantId()))
                                .map(grpcEventAttributeBuilder::buildGrpcDTOByBO)
                                .toList())
                        .build());
    }

    private Flux<GrpcDriverLeaseDTO> streamAssignmentSnapshot(
            GrpcDriverLeaseRequest request, DriverLeaseGrantBO grant) {
        return assertAssignmentVersion(request, grant.assignmentVersion())
                .thenMany(streamAssignmentPage(request, grant, 0L));
    }

    private Flux<GrpcDriverLeaseDTO> streamAssignmentPage(
            GrpcDriverLeaseRequest request, DriverLeaseGrantBO grant, long afterDeviceId) {
        return assertAssignmentVersion(request, grant.assignmentVersion())
                .thenMany(driverLeaseService
                        .listOwnedLeases(
                                request.getTenantId(),
                                request.getDriverId(),
                                request.getNode(),
                                afterDeviceId,
                                ASSIGNMENT_BATCH_SIZE + 1)
                        .collectList()
                        .flatMapMany(page -> {
                            boolean complete = page.size() <= ASSIGNMENT_BATCH_SIZE;
                            List<DeviceLeaseBO> batch = complete ? page : page.subList(0, ASSIGNMENT_BATCH_SIZE);
                            long nextCursor = batch.isEmpty()
                                    ? afterDeviceId
                                    : batch.getLast().deviceId();
                            Flux<GrpcDriverLeaseDTO> current = complete
                                    ? assertAssignmentVersion(request, grant.assignmentVersion())
                                            .thenMany(Flux.just(leaseResponse(grant, batch, true)))
                                    : Flux.just(leaseResponse(grant, batch, false));
                            return complete
                                    ? current
                                    : Flux.concat(current, streamAssignmentPage(request, grant, nextCursor));
                        }));
    }

    private Mono<Void> assertAssignmentVersion(GrpcDriverLeaseRequest request, long expectedVersion) {
        return driverLeaseService
                .getAssignmentVersion(request.getTenantId(), request.getDriverId())
                .flatMap(current -> current == expectedVersion
                        ? Mono.empty()
                        : Mono.error(new IllegalStateException("Driver assignment changed while streaming snapshot")));
    }

    private GrpcDriverLeaseDTO leaseResponse(DriverLeaseGrantBO grant, List<DeviceLeaseBO> leases, boolean complete) {
        return GrpcDriverLeaseDTO.newBuilder()
                .addAllDeviceLeases(leases.stream()
                        .map(lease -> GrpcDeviceLeaseDTO.newBuilder()
                                .setDeviceId(lease.deviceId())
                                .setFencingToken(lease.fencingToken())
                                .build())
                        .toList())
                .setLeaseUntilEpochMillis(grant.leaseUntilEpochMillis())
                .setAssignmentVersion(grant.assignmentVersion())
                .setAssignmentsChanged(grant.assignmentsChanged())
                .setSnapshotComplete(complete)
                .build();
    }
}
