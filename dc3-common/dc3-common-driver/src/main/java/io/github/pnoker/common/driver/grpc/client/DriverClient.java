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

import io.github.pnoker.api.common.GrpcCommandAttributeDTO;
import io.github.pnoker.api.common.GrpcDriverAttributeDTO;
import io.github.pnoker.api.common.GrpcDriverDTO;
import io.github.pnoker.api.common.GrpcDriverQuery;
import io.github.pnoker.api.common.GrpcEventAttributeDTO;
import io.github.pnoker.api.common.GrpcPointAttributeDTO;
import io.github.pnoker.api.common.driver.DriverApiGrpc;
import io.github.pnoker.api.common.driver.GrpcDriverLeaseDTO;
import io.github.pnoker.api.common.driver.GrpcDriverLeaseRequest;
import io.github.pnoker.api.common.driver.GrpcDriverRegisterDTO;
import io.github.pnoker.api.common.driver.GrpcDriverRegistrationDTO;
import io.github.pnoker.common.driver.entity.bo.DriverBO;
import io.github.pnoker.common.driver.entity.bo.RegisterBO;
import io.github.pnoker.common.driver.entity.builder.DriverBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcCommandAttributeBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcDriverAttributeBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcEventAttributeBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcPointAttributeBuilder;
import io.github.pnoker.common.driver.entity.dto.CommandAttributeDTO;
import io.github.pnoker.common.driver.entity.dto.DriverAttributeDTO;
import io.github.pnoker.common.driver.entity.dto.EventAttributeDTO;
import io.github.pnoker.common.driver.entity.dto.PointAttributeDTO;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.optional.CollectionOptional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * gRPC client responsible for driver registration and for loading the metadata returned
 * by the manager center after registration succeeds.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DriverClient {

    private final DriverApiGrpc.DriverApiStub driverApiStub;

    private final DriverMetadata driverMetadata;

    private final DriverProperties driverProperties;

    private final DriverBuilder driverBuilder;

    private final GrpcDriverAttributeBuilder grpcDriverAttributeBuilder;

    private final GrpcPointAttributeBuilder grpcPointAttributeBuilder;

    private final GrpcCommandAttributeBuilder grpcCommandAttributeBuilder;

    private final GrpcEventAttributeBuilder grpcEventAttributeBuilder;

    /**
     * Registers the current driver and stores the returned metadata in the shared driver
     * cache.
     *
     * @param entityBO driver registration payload
     */
    public Mono<Void> driverRegister(RegisterBO entityBO) {
        return Mono.defer(() -> {
            GrpcDriverRegisterDTO.Builder builder = GrpcDriverRegisterDTO.newBuilder();
            GrpcDriverDTO grpcDriverDTO = driverBuilder.buildGrpcDTOByDTO(entityBO.getDriver());
            builder.setTenant(entityBO.getTenant())
                    .setClient(entityBO.getClient())
                    .setNode(entityBO.getNode())
                    .setLeaseSeconds(entityBO.getLeaseSeconds())
                    .setDriver(grpcDriverDTO);

            CollectionOptional.ofNullable(entityBO.getDriverAttributes())
                    .ifPresent(value -> builder.addAllDriverAttributes(value.stream()
                            .map(grpcDriverAttributeBuilder::buildGrpcDTOByDTO)
                            .toList()));
            CollectionOptional.ofNullable(entityBO.getPointAttributes())
                    .ifPresent(value -> builder.addAllPointAttributes(value.stream()
                            .map(grpcPointAttributeBuilder::buildGrpcDTOByDTO)
                            .toList()));
            CollectionOptional.ofNullable(entityBO.getCommandAttributes())
                    .ifPresent(value -> builder.addAllCommandAttributes(value.stream()
                            .map(grpcCommandAttributeBuilder::buildGrpcDTOByDTO)
                            .toList()));
            CollectionOptional.ofNullable(entityBO.getEventAttributes())
                    .ifPresent(value -> builder.addAllEventAttributes(value.stream()
                            .map(grpcEventAttributeBuilder::buildGrpcDTOByDTO)
                            .toList()));

            return ReactiveGrpcClientSupport.<GrpcDriverRegisterDTO, GrpcDriverRegistrationDTO>unary(
                            "register driver", observer -> driverApiStub.driverRegister(builder.build(), observer))
                    .flatMap(response -> {
                        applyMetadata(response);
                        return renewLease();
                    });
        });
    }

    /**
     * Reloads the current driver metadata from manager without submitting registration
     * properties again.
     *
     * @param driverId registered driver id
     */
    public Mono<Void> refreshMetadata(Long driverId) {
        return Mono.defer(() -> {
            if (Objects.isNull(driverId) || driverId <= 0) {
                return Mono.error(new ServiceException("Failed to refresh driver metadata: invalid driver id"));
            }
            DriverBO driver = driverMetadata.getDriver();
            if (Objects.isNull(driver) || Objects.isNull(driver.getTenantId())) {
                return Mono.error(new ServiceException("Failed to refresh driver metadata: driver is not registered"));
            }
            GrpcDriverQuery query = GrpcDriverQuery.newBuilder()
                    .setTenantId(driver.getTenantId())
                    .setDriverId(driverId)
                    .build();
            return ReactiveGrpcClientSupport.<GrpcDriverQuery, GrpcDriverRegistrationDTO>unary(
                            "refresh driver metadata", observer -> driverApiStub.getById(query, observer))
                    .doOnNext(this::applyMetadata)
                    .then();
        });
    }

    /**
     * Renew runtime membership and replace the locally owned device set.
     */
    public Mono<Void> renewLease() {
        return Mono.defer(() -> {
            DriverBO driver = driverMetadata.getDriver();
            if (Objects.isNull(driver)) {
                return Mono.error(new ServiceException("Failed to renew driver lease: driver is not registered"));
            }
            GrpcDriverLeaseRequest request = GrpcDriverLeaseRequest.newBuilder()
                    .setTenantId(driver.getTenantId())
                    .setDriverId(driver.getId())
                    .setNode(driverProperties.getNode())
                    .setClient(driverProperties.getClient())
                    .setHost(driverProperties.getHost())
                    .setLeaseSeconds(driverProperties.getLease().getSeconds())
                    .setAssignmentVersion(driverMetadata.getAssignmentVersion())
                    .build();
            return ReactiveGrpcClientSupport.<GrpcDriverLeaseRequest, GrpcDriverLeaseDTO>stream(
                            "renew driver lease", observer -> driverApiStub.renewLease(request, observer))
                    .collect(LeaseSnapshot::new, LeaseSnapshot::accept)
                    .flatMap(snapshot -> snapshot.install(driverMetadata));
        });
    }

    private static final class LeaseSnapshot {

        private final Map<Long, Long> owned = new HashMap<>();
        private Long assignmentVersion;
        private Long leaseUntilEpochMillis;
        private Boolean assignmentsChanged;
        private boolean snapshotComplete;
        private int batches;

        private void accept(GrpcDriverLeaseDTO response) {
            if (snapshotComplete) {
                throw new ServiceException("Driver lease stream continued after snapshot completion");
            }
            if (assignmentVersion == null) {
                assignmentVersion = response.getAssignmentVersion();
                leaseUntilEpochMillis = response.getLeaseUntilEpochMillis();
                assignmentsChanged = response.getAssignmentsChanged();
            } else if (!Objects.equals(assignmentVersion, response.getAssignmentVersion())
                    || !Objects.equals(leaseUntilEpochMillis, response.getLeaseUntilEpochMillis())
                    || !Objects.equals(assignmentsChanged, response.getAssignmentsChanged())) {
                throw new ServiceException("Driver lease stream metadata changed between batches");
            }
            response.getDeviceLeasesList().forEach(lease -> {
                Long previous = owned.put(lease.getDeviceId(), lease.getFencingToken());
                if (previous != null) {
                    throw new ServiceException("Driver lease stream contains duplicate device {}", lease.getDeviceId());
                }
            });
            snapshotComplete = response.getSnapshotComplete();
            batches++;
        }

        private Mono<Void> install(DriverMetadata metadata) {
            if (batches == 0
                    || !snapshotComplete
                    || assignmentVersion == null
                    || leaseUntilEpochMillis == null
                    || assignmentsChanged == null) {
                return Mono.error(new ServiceException("Driver lease stream ended before snapshot completion"));
            }
            if (assignmentsChanged) {
                metadata.setDeviceLeases(owned, leaseUntilEpochMillis, assignmentVersion);
            } else {
                if (!owned.isEmpty()) {
                    return Mono.error(
                            new ServiceException("Unchanged driver lease stream contains device assignments"));
                }
                metadata.renewLeaseDeadline(leaseUntilEpochMillis);
            }
            return Mono.empty();
        }
    }

    private void applyMetadata(GrpcDriverRegistrationDTO rDriverRegisterDTO) {
        DriverBO driverBO = driverBuilder.buildDTOByGrpcDTO(rDriverRegisterDTO.getDriver());
        driverMetadata.setDriver(driverBO);

        List<GrpcDriverAttributeDTO> driverAttributesList = rDriverRegisterDTO.getDriverAttributesList();
        Map<Long, DriverAttributeDTO> driverAttributeIdMap = driverAttributesList.stream()
                .collect(Collectors.toMap(
                        entity -> entity.getBase().getId(), grpcDriverAttributeBuilder::buildDTOByGrpcDTO));
        Map<String, DriverAttributeDTO> driverAttributeNameMap = driverAttributesList.stream()
                .collect(Collectors.toMap(
                        GrpcDriverAttributeDTO::getAttributeCode, grpcDriverAttributeBuilder::buildDTOByGrpcDTO));
        driverMetadata.setDriverAttributeIdMap(driverAttributeIdMap);
        driverMetadata.setDriverAttributeNameMap(driverAttributeNameMap);

        List<GrpcPointAttributeDTO> pointAttributesList = rDriverRegisterDTO.getPointAttributesList();
        Map<Long, PointAttributeDTO> pointAttributeIdMap = pointAttributesList.stream()
                .collect(Collectors.toMap(
                        entity -> entity.getBase().getId(), grpcPointAttributeBuilder::buildDTOByGrpcDTO));
        Map<String, PointAttributeDTO> pointAttributeNameMap = pointAttributesList.stream()
                .collect(Collectors.toMap(
                        GrpcPointAttributeDTO::getAttributeCode, grpcPointAttributeBuilder::buildDTOByGrpcDTO));
        driverMetadata.setPointAttributeIdMap(pointAttributeIdMap);
        driverMetadata.setPointAttributeNameMap(pointAttributeNameMap);

        List<GrpcCommandAttributeDTO> commandAttributesList = rDriverRegisterDTO.getCommandAttributesList();
        Map<Long, CommandAttributeDTO> commandAttributeIdMap = commandAttributesList.stream()
                .collect(Collectors.toMap(
                        entity -> entity.getBase().getId(), grpcCommandAttributeBuilder::buildDTOByGrpcDTO));
        Map<String, CommandAttributeDTO> commandAttributeNameMap = commandAttributesList.stream()
                .collect(Collectors.toMap(
                        GrpcCommandAttributeDTO::getAttributeCode, grpcCommandAttributeBuilder::buildDTOByGrpcDTO));
        driverMetadata.setCommandAttributeIdMap(commandAttributeIdMap);
        driverMetadata.setCommandAttributeNameMap(commandAttributeNameMap);

        List<GrpcEventAttributeDTO> eventAttributesList = rDriverRegisterDTO.getEventAttributesList();
        Map<Long, EventAttributeDTO> eventAttributeIdMap = eventAttributesList.stream()
                .collect(Collectors.toMap(
                        entity -> entity.getBase().getId(), grpcEventAttributeBuilder::buildDTOByGrpcDTO));
        Map<String, EventAttributeDTO> eventAttributeNameMap = eventAttributesList.stream()
                .collect(Collectors.toMap(
                        GrpcEventAttributeDTO::getAttributeCode, grpcEventAttributeBuilder::buildDTOByGrpcDTO));
        driverMetadata.setEventAttributeIdMap(eventAttributeIdMap);
        driverMetadata.setEventAttributeNameMap(eventAttributeNameMap);

        driverMetadata.setDriverStatus(EntityStatusEnum.ONLINE);
    }
}
