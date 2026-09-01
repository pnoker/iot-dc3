package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.operation.DeviceImportManifest;
import io.github.pnoker.common.manager.repository.ReactiveDeviceImportJobStore;
import io.github.pnoker.common.manager.service.DeviceImportSchemaService;
import io.github.pnoker.common.manager.service.DeviceImportWorkbookCodec;
import io.github.pnoker.common.manager.support.ManagerFileScheduler;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.db.r2dbc.core.operation.OperationRepository;
import io.github.pnoker.db.r2dbc.core.operation.OperationState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveDeviceImportServiceImplTest {

    @Mock OperationRepository operationRepository;
    @Mock ReactiveDeviceImportJobStore jobStore;
    @Mock DeviceImportSchemaService schemaService;
    @Mock DeviceImportWorkbookCodec workbookCodec;
    @Mock ManagerFileScheduler fileScheduler;
    @Mock DeviceImportWorker worker;
    @Mock TransactionalOperator transactionalOperator;

    private ReactiveDeviceImportServiceImpl service;
    private DeviceBO context;

    @BeforeEach
    void setUp() {
        service = new ReactiveDeviceImportServiceImpl(operationRepository, jobStore, schemaService, workbookCodec,
                fileScheduler, worker, transactionalOperator, JsonUtil.getObjectMapper());
        context = new DeviceBO();
        context.setTenantId(7L);
        context.setDriverId(10L);
        context.setProfileId(20L);
        context.setOperatorId(30L);
        context.setOperatorName("operator");
        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(schemaService.load(7L, 10L, 20L)).thenReturn(Mono.just(
                new DeviceImportManifest(DeviceImportManifest.CURRENT_SCHEMA_VERSION, 10L, 20L,
                        List.of(), List.of())));
    }

    @Test
    void sameIdempotencyKeyAndRequestReturnsOriginalOperationWithoutDuplicateJob() {
        when(jobStore.insert(any())).thenReturn(Mono.empty());
        doNothing().when(worker).enqueue(any());
        AtomicReference<OperationState> original = new AtomicReference<>();
        when(operationRepository.create(any(), any())).thenAnswer(invocation -> {
            OperationState requested = invocation.getArgument(1);
            if (original.compareAndSet(null, requested)) return Mono.just(requested);
            return Mono.just(original.get());
        });

        var first = service.submit(context, "devices.xlsx", new byte[]{1, 2, 3}, "request-1");
        var second = service.submit(context, "devices.xlsx", new byte[]{1, 2, 3}, "request-1");

        StepVerifier.create(first.zipWith(second))
                .assertNext(tuple -> assertThat(tuple.getT2().operationId()).isEqualTo(tuple.getT1().operationId()))
                .verifyComplete();
        verify(jobStore, times(1)).insert(any());
        verify(worker, times(1)).enqueue(any());
    }

    @Test
    void sameIdempotencyKeyWithDifferentContentProducesDifferentRequestHash() {
        when(jobStore.insert(any())).thenReturn(Mono.empty());
        doNothing().when(worker).enqueue(any());
        ArgumentCaptor<OperationState> states = ArgumentCaptor.forClass(OperationState.class);
        when(operationRepository.create(any(), any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(1)));

        StepVerifier.create(service.submit(context, "devices.xlsx", new byte[]{1}, "request-1")
                        .then(service.submit(context, "devices.xlsx", new byte[]{2}, "request-1")))
                .expectNextCount(1)
                .verifyComplete();
        verify(operationRepository, times(2)).create(any(), states.capture());
        assertThat(states.getAllValues()).extracting(OperationState::idempotencyKey)
                .containsOnly("request-1");
        assertThat(states.getAllValues().get(0).requestHash())
                .isNotEqualTo(states.getAllValues().get(1).requestHash());
    }

    @Test
    void validatesTenantScopedDriverAndProfileBeforeCreatingOperation() {
        when(schemaService.load(7L, 10L, 20L)).thenReturn(Mono.error(
                new io.github.pnoker.common.exception.NotFoundException("Resource does not exist")));

        StepVerifier.create(service.submit(context, "devices.xlsx", new byte[]{1}, "request-1"))
                .expectError(io.github.pnoker.common.exception.NotFoundException.class)
                .verify();
        verify(operationRepository, times(0)).create(any(), any());
    }
}
