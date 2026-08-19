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

import io.github.pnoker.api.common.GrpcRFactory;
import io.github.pnoker.api.common.driver.DriverApiGrpc;
import io.github.pnoker.api.common.driver.GrpcDeviceLeaseDTO;
import io.github.pnoker.api.common.driver.GrpcRDriverLeaseDTO;
import io.github.pnoker.common.driver.entity.bo.DriverBO;
import io.github.pnoker.common.driver.entity.builder.DriverBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcCommandAttributeBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcDriverAttributeBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcEventAttributeBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcPointAttributeBuilder;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverClientLeaseTest {

    @Mock
    private DriverApiGrpc.DriverApiBlockingStub stub;
    @Mock
    private DriverBuilder driverBuilder;
    @Mock
    private GrpcDriverAttributeBuilder driverAttributeBuilder;
    @Mock
    private GrpcPointAttributeBuilder pointAttributeBuilder;
    @Mock
    private GrpcCommandAttributeBuilder commandAttributeBuilder;
    @Mock
    private GrpcEventAttributeBuilder eventAttributeBuilder;

    private DriverMetadata metadata;
    private DriverClient client;

    @BeforeEach
    void setUp() {
        DriverProperties properties = new DriverProperties();
        properties.setNode("node-a");
        properties.setClient("client-a");
        properties.setHost("host-a");
        metadata = new DriverMetadata();
        DriverBO driver = new DriverBO();
        driver.setId(7L);
        driver.setTenantId(100L);
        metadata.setDriver(driver);
        metadata.setDeviceLeases(Map.of(99L, 400L), System.currentTimeMillis() + 60_000, 4L);
        client = new DriverClient(stub, metadata, properties, driverBuilder, driverAttributeBuilder,
                pointAttributeBuilder, commandAttributeBuilder, eventAttributeBuilder);
    }

    @Test
    void changedLeaseSnapshotIsInstalledOnlyAfterAllBatchesComplete() {
        long deadline = System.currentTimeMillis() + 60_000;
        when(stub.renewLease(any())).thenReturn(List.of(
                response(deadline, 5L, false, lease(1L, 501L)),
                response(deadline, 5L, true, lease(2L, 502L))).iterator());

        client.renewLease();

        assertThat(metadata.getDeviceIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(metadata.getFencingToken(1L)).isEqualTo(501L);
        assertThat(metadata.getAssignmentVersion()).isEqualTo(5L);
    }

    @Test
    void incompleteLeaseSnapshotDoesNotReplaceCurrentOwnership() {
        long deadline = System.currentTimeMillis() + 60_000;
        when(stub.renewLease(any())).thenReturn(List.of(
                response(deadline, 5L, false, lease(1L, 501L))).iterator());

        assertThatThrownBy(client::renewLease)
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("before snapshot completion");
        assertThat(metadata.getDeviceIds()).containsExactly(99L);
        assertThat(metadata.getAssignmentVersion()).isEqualTo(4L);
    }

    private GrpcRDriverLeaseDTO response(long deadline, long version, boolean complete,
                                         GrpcDeviceLeaseDTO lease) {
        return GrpcRDriverLeaseDTO.newBuilder()
                .setResult(GrpcRFactory.ok())
                .setLeaseUntilEpochMillis(deadline)
                .setAssignmentVersion(version)
                .setAssignmentsChanged(true)
                .setSnapshotComplete(complete)
                .addDeviceLeases(lease)
                .build();
    }

    private GrpcDeviceLeaseDTO lease(long deviceId, long fencingToken) {
        return GrpcDeviceLeaseDTO.newBuilder()
                .setDeviceId(deviceId)
                .setFencingToken(fencingToken)
                .build();
    }
}
