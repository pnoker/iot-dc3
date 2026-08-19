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

package io.github.pnoker.common.manager.biz.impl;

import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.manager.dal.DriverLeaseManager;
import io.github.pnoker.common.manager.entity.bo.DriverLeaseGrantBO;
import io.github.pnoker.common.manager.entity.model.DeviceLeaseDO;
import io.github.pnoker.common.manager.entity.model.DriverLeaseStateDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverLeaseServiceImplTest {

    @Mock
    private DriverLeaseManager manager;

    private DriverLeaseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DriverLeaseServiceImpl(manager);
    }

    @Test
    void initialRenewReconcilesAllDevicesAndReturnsOwnedAssignment() {
        List<Long> deviceIds = LongStream.rangeClosed(1, 100).boxed().toList();
        when(manager.listActiveNodes(1L, 2L)).thenReturn(List.of("node-a", "node-b"));
        when(manager.getLeaseState(1L, 2L)).thenReturn(null);
        when(manager.listDriverDeviceIds(1L, 2L, 0L, 5000)).thenReturn(deviceIds);
        when(manager.advanceAssignmentVersion(eq(1L), eq(2L), anyString(), eq(0L))).thenReturn(8L);
        DriverLeaseGrantBO grant = service.renew(1L, 2L, "node-a", "client-a", "host-a", 30, 0);

        ArgumentCaptor<List<DeviceLeaseDO>> assignments = ArgumentCaptor.forClass(List.class);
        verify(manager).reconcileDeviceLeases(assignments.capture());
        assertThat(assignments.getValue()).hasSize(100);
        assertThat(assignments.getValue()).extracting(DeviceLeaseDO::getOwnerNode)
                .containsOnlyElementsOf(Set.of("node-a", "node-b"))
                .contains("node-a", "node-b");
        assertThat(grant.assignmentVersion()).isEqualTo(8L);
        assertThat(grant.assignmentsChanged()).isTrue();
    }

    @Test
    void stableHeartbeatDoesNotScanDevicesOrReturnAssignmentAgain() {
        when(manager.listActiveNodes(1L, 2L)).thenReturn(List.of("node-a", "node-b"));
        when(manager.getLeaseState(1L, 2L)).thenReturn(null);
        when(manager.listDriverDeviceIds(1L, 2L, 0L, 5000)).thenReturn(List.of(10L));
        when(manager.advanceAssignmentVersion(eq(1L), eq(2L), anyString(), eq(0L))).thenReturn(9L);
        service.renew(1L, 2L, "node-a", "client-a", "host-a", 30, 0);

        ArgumentCaptor<String> membershipHash = ArgumentCaptor.forClass(String.class);
        verify(manager).advanceAssignmentVersion(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(2L), membershipHash.capture(), eq(0L));
        DriverLeaseStateDO state = new DriverLeaseStateDO();
        state.setMembershipHash(membershipHash.getValue());
        state.setDeviceRevision(0L);
        state.setAssignmentVersion(9L);
        when(manager.getLeaseState(1L, 2L)).thenReturn(state);

        DriverLeaseGrantBO heartbeat = service.renew(
                1L, 2L, "node-a", "client-a", "host-a", 30, 9L);

        verify(manager, times(1)).listDriverDeviceIds(1L, 2L, 0L, 5000);
        verify(manager, times(2)).getDeviceRevision(1L, 2L);
        verify(manager, times(1)).reconcileDeviceLeases(anyList());
        assertThat(heartbeat.assignmentsChanged()).isFalse();
    }

    @Test
    void ownedAssignmentsAreReadWithBoundedKeysetPages() {
        when(manager.listOwnedLeases(1L, 2L, "node-a", 100L, 1000))
                .thenReturn(List.of(new DeviceLeaseDO(1L, 2L, 101L, "node-a", 501L)));

        assertThat(service.listOwnedLeases(1L, 2L, "node-a", 100L, 1000))
                .containsExactly(new io.github.pnoker.common.manager.entity.bo.DeviceLeaseBO(
                        2L, 101L, "node-a", 501L));
    }

    @Test
    void invalidLeaseIdentityFailsBeforeDatabaseWork() {
        assertThatThrownBy(() -> service.renew(1L, 2L, "", "client", "host", 30, 0))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("identity");
        verify(manager, never()).acquireDriverLock(2L);
    }
}
