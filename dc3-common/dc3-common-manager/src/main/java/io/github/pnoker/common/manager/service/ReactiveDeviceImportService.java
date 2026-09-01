package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.operation.OperationView;
import io.github.pnoker.db.r2dbc.core.operation.OperationAccepted;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ReactiveDeviceImportService {

    Mono<OperationAccepted> submit(DeviceBO context, String fileName, byte[] content, String idempotencyKey);

    Mono<byte[]> generateTemplate(Long tenantId, Long driverId, Long profileId);

    Mono<OperationView> getOperation(Long tenantId, UUID operationId);
}
