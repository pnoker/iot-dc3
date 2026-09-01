package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.manager.entity.operation.DeviceImportManifest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DeviceImportSchemaService {

    private final ReactiveDriverService driverService;
    private final ReactiveProfileService profileService;
    private final ReactiveDriverAttributeService driverAttributeService;
    private final ReactivePointAttributeService pointAttributeService;
    private final ReactivePointService pointService;
    private final DeviceImportWorkbookCodec workbookCodec;

    public Mono<DeviceImportManifest> load(Long tenantId, Long driverId, Long profileId) {
        if (tenantId == null || driverId == null || profileId == null) {
            return Mono.error(new RequestException("Tenant ID, driver ID and profile ID are required"));
        }
        return Mono.zip(driverService.getById(tenantId, driverId), profileService.getById(tenantId, profileId))
                .switchIfEmpty(Mono.error(new NotFoundException("Resource does not exist")))
                .then(Mono.zip(driverAttributeService.listByDriverId(tenantId, driverId).collectList(),
                        pointAttributeService.listByDriverId(tenantId, driverId).collectList(),
                        pointService.listByProfileId(tenantId, profileId).collectList()))
                .map(tuple -> workbookCodec.manifest(driverId, profileId, tuple.getT1(), tuple.getT2(),
                        tuple.getT3()));
    }
}
