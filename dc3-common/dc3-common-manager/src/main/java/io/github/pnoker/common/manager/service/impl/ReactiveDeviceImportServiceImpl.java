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

import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.operation.DeviceImportJob;
import io.github.pnoker.common.manager.entity.operation.OperationView;
import io.github.pnoker.common.manager.repository.ReactiveDeviceImportJobStore;
import io.github.pnoker.common.manager.service.DeviceImportSchemaService;
import io.github.pnoker.common.manager.service.DeviceImportWorkbookCodec;
import io.github.pnoker.common.manager.service.ReactiveDeviceImportService;
import io.github.pnoker.common.manager.support.ManagerFileScheduler;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.operation.OperationAccepted;
import io.github.pnoker.db.r2dbc.core.operation.OperationRepository;
import io.github.pnoker.db.r2dbc.core.operation.OperationState;
import io.github.pnoker.db.r2dbc.core.tenant.TenantScope;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ReactiveDeviceImportServiceImpl implements ReactiveDeviceImportService {

    private static final Duration OPERATION_TTL = Duration.ofDays(7);

    private final OperationRepository operationRepository;
    private final ReactiveDeviceImportJobStore jobStore;
    private final DeviceImportSchemaService schemaService;
    private final DeviceImportWorkbookCodec workbookCodec;
    private final ManagerFileScheduler fileScheduler;
    private final DeviceImportWorker worker;
    private final TransactionalOperator transactionalOperator;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<OperationAccepted> submit(DeviceBO context, String fileName, byte[] content, String idempotencyKey) {
        if (context == null
                || context.getTenantId() == null
                || context.getDriverId() == null
                || context.getProfileId() == null
                || content == null
                || content.length == 0
                || idempotencyKey == null
                || idempotencyKey.isBlank()) {
            return Mono.error(new RequestException(
                    "Tenant, driver, profile, non-empty XLSX file and Idempotency-Key are required"));
        }
        UUID operationId = UuidV7.next();
        Instant now = Instant.now();
        String requestHash = requestHash(context.getDriverId(), context.getProfileId(), content);
        OperationState pending = OperationState.pending(
                operationId, context.getTenantId(), idempotencyKey, requestHash, now, now.plus(OPERATION_TTL));
        TenantScope scope = new TenantScope(context.getTenantId());
        DeviceImportJob job = new DeviceImportJob(
                operationId,
                context.getTenantId(),
                context.getDriverId(),
                context.getProfileId(),
                context.getOperatorId(),
                context.getOperatorName(),
                fileName,
                content.clone(),
                "",
                null,
                0);
        Mono<OperationState> create = schemaService
                .load(context.getTenantId(), context.getDriverId(), context.getProfileId())
                .then(Mono.defer(() -> operationRepository.create(scope, pending)))
                .flatMap(saved -> saved.operationId().equals(operationId)
                        ? jobStore.insert(job).thenReturn(saved)
                        : Mono.just(saved));
        return transactionalOperator
                .transactional(create)
                .doOnNext(saved -> {
                    if (saved.operationId().equals(operationId)) worker.enqueue(operationId);
                })
                .map(saved -> accepted(saved.operationId()));
    }

    @Override
    public Mono<byte[]> generateTemplate(Long tenantId, Long driverId, Long profileId) {
        return schemaService
                .load(tenantId, driverId, profileId)
                .flatMap(manifest -> fileScheduler.call(() -> workbookCodec.create(manifest)));
    }

    @Override
    public Mono<OperationView> getOperation(Long tenantId, UUID operationId) {
        if (tenantId == null || operationId == null) {
            return Mono.error(new RequestException("Tenant ID and operation ID are required"));
        }
        return operationRepository
                .findById(new TenantScope(tenantId), operationId)
                .switchIfEmpty(
                        Mono.error(new io.github.pnoker.common.exception.NotFoundException("Operation does not exist")))
                .map(state -> new OperationView(
                        state.operationId(),
                        state.status(),
                        state.progress(),
                        json(state.result()),
                        json(state.error()),
                        state.createdAt(),
                        state.updatedAt(),
                        state.expiresAt()));
    }

    private OperationAccepted accepted(UUID operationId) {
        return new OperationAccepted(operationId, "/api/v3/manager/operations/get_by_id?id=" + operationId);
    }

    private Object json(String value) {
        if (value == null) return null;
        try {
            return objectMapper.readValue(value, Object.class);
        } catch (Exception error) {
            throw new IllegalStateException("Operation payload contains invalid JSON", error);
        }
    }

    private String requestHash(Long driverId, Long profileId, byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(ByteBuffer.allocate(Long.BYTES).putLong(driverId).array());
            digest.update(ByteBuffer.allocate(Long.BYTES).putLong(profileId).array());
            digest.update("device-import-v1".getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (Exception error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }
}
