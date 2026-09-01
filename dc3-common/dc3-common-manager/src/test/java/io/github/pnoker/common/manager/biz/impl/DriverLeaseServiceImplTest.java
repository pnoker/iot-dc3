package io.github.pnoker.common.manager.biz.impl;

import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.manager.entity.bo.DeviceLeaseBO;
import io.github.pnoker.common.manager.entity.model.DeviceLeaseDO;
import io.github.pnoker.common.manager.entity.model.DriverLeaseStateDO;
import io.github.pnoker.common.manager.repository.ReactiveDriverLeaseStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverLeaseServiceImplTest {
    @Mock private ReactiveDriverLeaseStore store;
    @Mock private TransactionalOperator transactionalOperator;
    private DriverLeaseServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(store.acquireDriverLock(any(), any())).thenReturn(Mono.empty());
        lenient().when(store.renewInstance(any(), any(), anyString(), anyString(), anyString(), any())).thenReturn(Mono.empty());
        lenient().when(store.deleteExpiredInstances(any(), any(), any())).thenReturn(Mono.empty());
        lenient().when(store.deleteOrphanedLeases(any(), any())).thenReturn(Mono.empty());
        lenient().when(store.reconcileDeviceLeases(anyList())).thenReturn(Mono.empty());
        lenient().when(store.listActiveNodes(any(), any())).thenReturn(Flux.empty());
        lenient().when(store.getDeviceRevision(any(), any())).thenReturn(Mono.just(0L));
        lenient().when(store.getLeaseState(any(), any())).thenReturn(Mono.empty());
        lenient().when(store.listDriverDeviceIds(any(), any(), anyLong(), anyInt())).thenReturn(Flux.empty());
        lenient().when(store.advanceAssignmentVersion(any(), any(), anyString(), anyLong())).thenReturn(Mono.just(1L));
        lenient().when(store.listOwnedLeases(any(), any(), anyString(), anyLong(), anyInt())).thenReturn(Flux.empty());
        lenient().when(store.getActiveLease(any(), any())).thenReturn(Mono.empty());
        service = new DriverLeaseServiceImpl(store, transactionalOperator);
    }

    @Test
    void initialRenewReconcilesAllDevicesAndReturnsChangedGrant() {
        when(store.listActiveNodes(1L, 2L)).thenReturn(Flux.just("node-a", "node-b"));
        when(store.getDeviceRevision(1L, 2L)).thenReturn(Mono.just(0L));
        when(store.getLeaseState(1L, 2L)).thenReturn(Mono.empty());
        when(store.listDriverDeviceIds(1L, 2L, 0L, 5000)).thenReturn(Flux.fromStream(LongStream.rangeClosed(1, 100).boxed()));
        when(store.advanceAssignmentVersion(eq(1L), eq(2L), anyString(), eq(0L))).thenReturn(Mono.just(8L));

        StepVerifier.create(service.renew(1L, 2L, "node-a", "client-a", "host-a", 30, 0))
                .assertNext(grant -> {
                    assertThat(grant.assignmentVersion()).isEqualTo(8L);
                    assertThat(grant.assignmentsChanged()).isTrue();
                }).verifyComplete();

        ArgumentCaptor<List<DeviceLeaseDO>> assignments = ArgumentCaptor.forClass(List.class);
        verify(store).reconcileDeviceLeases(assignments.capture());
        assertThat(assignments.getValue()).hasSize(100);
        assertThat(assignments.getValue()).extracting(DeviceLeaseDO::getOwnerNode).contains("node-a", "node-b");
    }

    @Test
    void stableHeartbeatSkipsReconciliation() {
        DriverLeaseStateDO state = new DriverLeaseStateDO();
        state.setMembershipHash(hash(List.of("node-a", "node-b")));
        state.setDeviceRevision(0L);
        state.setAssignmentVersion(9L);
        when(store.listActiveNodes(1L, 2L)).thenReturn(Flux.just("node-a", "node-b"));
        when(store.getDeviceRevision(1L, 2L)).thenReturn(Mono.just(0L));
        when(store.getLeaseState(1L, 2L)).thenReturn(Mono.just(state));

        StepVerifier.create(service.renew(1L, 2L, "node-a", "client-a", "host-a", 30, 9L))
                .assertNext(grant -> assertThat(grant.assignmentsChanged()).isFalse()).verifyComplete();
        verify(store, never()).listDriverDeviceIds(any(), any(), anyLong(), anyInt());
        verify(store, never()).reconcileDeviceLeases(anyList());
        verify(store, never()).advanceAssignmentVersion(any(), any(), anyString(), anyLong());
    }

    @Test
    void ownedAssignmentsUseValidatedKeysetCursor() {
        when(store.listOwnedLeases(1L, 2L, "node-a", 100L, 1000)).thenReturn(Flux.just(new DeviceLeaseDO(1L, 2L, 101L, "node-a", 7L)));
        StepVerifier.create(service.listOwnedLeases(1L, 2L, "node-a", 100L, 1000))
                .assertNext(lease -> assertThat(lease.deviceId()).isEqualTo(101L)).verifyComplete();
        StepVerifier.create(service.listOwnedLeases(1L, 2L, "node-a", -1L, 1000)).expectError(ServiceException.class).verify();
    }

    @Test
    void missingAssignmentStateIsAnError() {
        when(store.getLeaseState(1L, 2L)).thenReturn(Mono.empty());
        StepVerifier.create(service.getAssignmentVersion(1L, 2L)).expectError(ServiceException.class).verify();
    }

    @Test
    void invalidRenewInputFailsBeforeDatabase() {
        StepVerifier.create(service.renew(1L, 2L, "", "client", "host", 30, 0)).expectError(ServiceException.class).verify();
        verifyNoInteractions(store);
    }

    @Test
    void activeOwnerIsTenantScopedAndReactive() {
        when(store.getActiveLease(1L, 10L)).thenReturn(Mono.just(new DeviceLeaseDO(1L, 2L, 10L, "node-a", 4L)));
        StepVerifier.create(service.getActiveOwner(1L, 10L))
                .assertNext(owner -> assertThat(owner).isEqualTo(new DeviceLeaseBO(2L, 10L, "node-a", 4L))).verifyComplete();
    }

    private String hash(List<String> nodes) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(String.join("\u0000", nodes).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception error) { throw new AssertionError(error); }
    }
}
