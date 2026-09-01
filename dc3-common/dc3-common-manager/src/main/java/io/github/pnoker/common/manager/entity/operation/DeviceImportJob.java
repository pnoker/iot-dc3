package io.github.pnoker.common.manager.entity.operation;

import java.time.Instant;
import java.util.UUID;

public record DeviceImportJob(
        UUID operationId,
        Long tenantId,
        Long driverId,
        Long profileId,
        Long operatorId,
        String operatorName,
        String fileName,
        byte[] content,
        String claimedBy,
        Instant claimedUntil,
        int attempts) {
}
