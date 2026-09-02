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
package io.github.pnoker.common.manager.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.operation.DeviceImportJob;
import io.github.pnoker.common.manager.entity.operation.DeviceImportManifest;
import io.github.pnoker.common.manager.entity.operation.DeviceImportRow;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.repository.ReactiveDeviceImportJobStore;
import io.github.pnoker.common.manager.repository.ReactiveDeviceStore;
import io.github.pnoker.common.manager.repository.ReactiveDriverAttributeConfigStore;
import io.github.pnoker.common.manager.repository.ReactivePointAttributeConfigStore;
import io.github.pnoker.common.manager.service.DeviceImportSchemaService;
import io.github.pnoker.common.manager.service.DeviceImportWorkbookCodec;
import io.github.pnoker.common.manager.support.ManagerFileScheduler;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.db.r2dbc.core.operation.OperationRepository;
import io.github.pnoker.db.r2dbc.core.operation.OperationState;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class DeviceImportWorkerTest {

    private static final UUID OPERATION_ID = UUID.fromString("0198f1d4-3400-7000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-08-31T00:00:00Z");

    @Mock
    ReactiveDeviceImportJobStore jobStore;

    @Mock
    OperationRepository operationRepository;

    @Mock
    DeviceImportSchemaService schemaService;

    @Mock
    DeviceImportWorkbookCodec workbookCodec;

    @Mock
    ManagerFileScheduler fileScheduler;

    @Mock
    ReactiveDeviceStore deviceStore;

    @Mock
    ReactiveDriverAttributeConfigStore driverConfigStore;

    @Mock
    ReactivePointAttributeConfigStore pointConfigStore;

    @Mock
    MetadataEventPublisher metadataEventPublisher;

    @Mock
    TransactionalOperator transactionalOperator;

    private DeviceImportWorker worker;
    private DeviceImportJob job;
    private DeviceImportManifest manifest;

    @BeforeEach
    void setUp() {
        worker = new DeviceImportWorker(
                jobStore,
                operationRepository,
                schemaService,
                workbookCodec,
                fileScheduler,
                deviceStore,
                driverConfigStore,
                pointConfigStore,
                metadataEventPublisher,
                transactionalOperator,
                JsonUtil.getObjectMapper());
        job = new DeviceImportJob(
                OPERATION_ID, 7L, 10L, 20L, 30L, "operator", "devices.xlsx", new byte[] {1}, "", null, 0);
        manifest =
                new DeviceImportManifest(DeviceImportManifest.CURRENT_SCHEMA_VERSION, 10L, 20L, List.of(), List.of());
    }

    @Test
    void successfulImportCommitsDevicesOperationAndJobDeletionInOneTransaction() {
        passThroughTransactions();
        OperationState pending = pending();
        OperationState running = pending.transition(OperationState.Status.RUNNING, 5, NOW.plusSeconds(1));
        DeviceBO saved = new DeviceBO();
        saved.setId(101L);
        when(jobStore.claim(eq(OPERATION_ID), any(), any(), any())).thenReturn(Mono.just(job));
        when(operationRepository.findById(any(), eq(OPERATION_ID))).thenReturn(Mono.just(pending), Mono.just(running));
        when(operationRepository.transition(any(), eq(OPERATION_ID), eq(OperationState.Status.PENDING), any()))
                .thenReturn(Mono.just(running));
        when(operationRepository.transition(any(), eq(OPERATION_ID), eq(OperationState.Status.RUNNING), any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(3)));
        when(schemaService.load(7L, 10L, 20L)).thenReturn(Mono.just(manifest));
        when(fileScheduler.call(any()))
                .thenReturn(Mono.just(List.of(new DeviceImportRow(2, "Boiler-A", "", List.of(), List.of()))));
        when(deviceStore.insert(any())).thenReturn(Mono.just(saved));
        when(jobStore.delete(OPERATION_ID, 7L)).thenReturn(Mono.empty());

        StepVerifier.create(worker.processSafely(OPERATION_ID)).verifyComplete();

        ArgumentCaptor<OperationState> completed = ArgumentCaptor.forClass(OperationState.class);
        verify(operationRepository)
                .transition(any(), eq(OPERATION_ID), eq(OperationState.Status.RUNNING), completed.capture());
        assertThat(completed.getValue().status()).isEqualTo(OperationState.Status.SUCCEEDED);
        assertThat(completed.getValue().result()).contains("\"deviceIds\":[101]");
        InOrder order = inOrder(deviceStore, operationRepository, jobStore);
        order.verify(deviceStore).insert(any());
        order.verify(operationRepository).transition(any(), eq(OPERATION_ID), eq(OperationState.Status.RUNNING), any());
        order.verify(jobStore).delete(OPERATION_ID, 7L);
        verify(transactionalOperator).transactional(any(Mono.class));
        verify(metadataEventPublisher).publishEvent(any());
    }

    @Test
    void rowFailureDoesNotMarkSuccessAndTransitionsOperationToFailed() {
        passThroughTransactions();
        OperationState pending = pending();
        OperationState running = pending.transition(OperationState.Status.RUNNING, 5, NOW.plusSeconds(1));
        when(jobStore.claim(eq(OPERATION_ID), any(), any(), any())).thenReturn(Mono.just(job));
        when(operationRepository.findById(any(), eq(OPERATION_ID))).thenReturn(Mono.just(pending), Mono.just(running));
        when(operationRepository.transition(any(), eq(OPERATION_ID), eq(OperationState.Status.PENDING), any()))
                .thenReturn(Mono.just(running));
        when(operationRepository.transition(any(), eq(OPERATION_ID), eq(OperationState.Status.RUNNING), any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(3)));
        when(schemaService.load(7L, 10L, 20L)).thenReturn(Mono.just(manifest));
        when(fileScheduler.call(any()))
                .thenReturn(Mono.just(List.of(new DeviceImportRow(2, "Boiler-A", "", List.of(), List.of()))));
        when(deviceStore.insert(any())).thenReturn(Mono.error(new IllegalStateException("duplicate device")));
        when(jobStore.delete(OPERATION_ID, 7L)).thenReturn(Mono.empty());

        StepVerifier.create(worker.processSafely(OPERATION_ID)).verifyComplete();

        ArgumentCaptor<OperationState> terminal = ArgumentCaptor.forClass(OperationState.class);
        verify(operationRepository)
                .transition(any(), eq(OPERATION_ID), eq(OperationState.Status.RUNNING), terminal.capture());
        assertThat(terminal.getValue().status()).isEqualTo(OperationState.Status.FAILED);
        assertThat(terminal.getValue().error()).contains("duplicate device");
        verify(metadataEventPublisher, never()).publishEvent(any());
    }

    @Test
    void terminalOperationOnlyDeletesOrphanedJob() {
        OperationState succeeded = pending()
                .transition(OperationState.Status.RUNNING, 5, NOW.plusSeconds(1))
                .transition(OperationState.Status.SUCCEEDED, 100, "{}", null, NOW.plusSeconds(2));
        when(jobStore.claim(eq(OPERATION_ID), any(), any(), any())).thenReturn(Mono.just(job));
        when(operationRepository.findById(any(), eq(OPERATION_ID))).thenReturn(Mono.just(succeeded));
        when(jobStore.delete(OPERATION_ID, 7L)).thenReturn(Mono.empty());

        StepVerifier.create(worker.processSafely(OPERATION_ID)).verifyComplete();

        verify(jobStore).delete(OPERATION_ID, 7L);
        verifyNoInteractions(
                schemaService,
                workbookCodec,
                fileScheduler,
                deviceStore,
                driverConfigStore,
                pointConfigStore,
                metadataEventPublisher);
    }

    @Test
    void metadataFailureCannotChangeCommittedSuccess() {
        passThroughTransactions();
        OperationState pending = pending();
        OperationState running = pending.transition(OperationState.Status.RUNNING, 5, NOW.plusSeconds(1));
        DeviceBO saved = new DeviceBO();
        saved.setId(101L);
        when(jobStore.claim(eq(OPERATION_ID), any(), any(), any())).thenReturn(Mono.just(job));
        when(operationRepository.findById(any(), eq(OPERATION_ID))).thenReturn(Mono.just(pending), Mono.just(running));
        when(operationRepository.transition(any(), eq(OPERATION_ID), eq(OperationState.Status.PENDING), any()))
                .thenReturn(Mono.just(running));
        when(operationRepository.transition(any(), eq(OPERATION_ID), eq(OperationState.Status.RUNNING), any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(3)));
        when(schemaService.load(7L, 10L, 20L)).thenReturn(Mono.just(manifest));
        when(fileScheduler.call(any()))
                .thenReturn(Mono.just(List.of(new DeviceImportRow(2, "Boiler-A", "", List.of(), List.of()))));
        when(deviceStore.insert(any())).thenReturn(Mono.just(saved));
        when(jobStore.delete(OPERATION_ID, 7L)).thenReturn(Mono.empty());
        doThrow(new IllegalStateException("publisher unavailable"))
                .when(metadataEventPublisher)
                .publishEvent(any());

        StepVerifier.create(worker.processSafely(OPERATION_ID)).verifyComplete();

        verify(operationRepository, never())
                .transition(any(), eq(OPERATION_ID), eq(OperationState.Status.SUCCEEDED), any());
    }

    private OperationState pending() {
        return OperationState.pending(OPERATION_ID, 7L, "request-1", "a".repeat(64), NOW, NOW.plusSeconds(3600));
    }

    private void passThroughTransactions() {
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
