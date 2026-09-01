package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.ConflictException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.bo.DeviceByPointBO;
import io.github.pnoker.common.manager.entity.bo.PointConfigByDeviceBO;
import io.github.pnoker.common.manager.repository.PointFilter;
import io.github.pnoker.common.manager.repository.ReactiveDeviceStore;
import io.github.pnoker.common.manager.repository.ReactivePointAttributeConfigStore;
import io.github.pnoker.common.manager.repository.ReactivePointStore;
import io.github.pnoker.common.manager.repository.ReactiveProfileStore;
import io.github.pnoker.common.manager.service.ReactivePointService;
import io.github.pnoker.common.utils.CodeUtil;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReactivePointServiceImpl implements ReactivePointService {
    private final ReactivePointStore pointStore;
    private final ReactiveProfileStore profileStore;
    private final ReactiveDeviceStore deviceStore;
    private final ReactivePointAttributeConfigStore pointAttributeConfigStore;

    @Override public Mono<PointBO> add(PointBO value) {
        return Mono.defer(() -> { validate(value, false); value.setPointCode(value.getPointCode() == null || value.getPointCode().isBlank() ? CodeUtil.getCode() : value.getPointCode().trim()); return profileStore.get(value.getTenantId(), value.getProfileId()).switchIfEmpty(Mono.error(new NotFoundException("Profile does not exist"))).then(pointStore.existsByNameOrCode(value.getTenantId(), value.getProfileId(), value.getPointName(), value.getPointCode(), null)).flatMap(duplicate -> duplicate ? Mono.<PointBO>error(new DuplicateException("Point has been duplicated")) : pointStore.insert(normalize(value, false))).onErrorMap(DataIntegrityViolationException.class, error -> new DuplicateException("Point has been duplicated")); });
    }
    @Override public Mono<PointBO> update(PointBO value) {
        return Mono.defer(() -> { validate(value, true); return pointStore.get(value.getTenantId(), value.getId()).switchIfEmpty(Mono.error(new NotFoundException("Point does not exist"))).flatMap(current -> profileStore.get(value.getTenantId(), value.getProfileId()).switchIfEmpty(Mono.error(new NotFoundException("Profile does not exist"))).then(pointStore.existsByNameOrCode(value.getTenantId(), value.getProfileId(), value.getPointName(), current.getPointCode(), value.getId())).flatMap(duplicate -> duplicate ? Mono.<PointBO>error(new DuplicateException("Point has been duplicated")) : pointStore.update(normalize(value, true), value.getVersion()))).switchIfEmpty(Mono.error(new ConflictException("Point version conflict"))); });
    }
    @Override public Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName) { if (tenantId == null || id == null) return Mono.error(new RequestException("Tenant ID and point ID are required")); return pointStore.get(tenantId, id).switchIfEmpty(Mono.error(new NotFoundException("Point does not exist"))).flatMap(point -> pointStore.delete(tenantId, id, expectedVersion, operatorId, operatorName)).filter(Boolean.TRUE::equals).switchIfEmpty(Mono.error(new ConflictException("Point version conflict"))); }
    @Override public Mono<PointBO> getById(Long tenantId, Long id) { if (tenantId == null || id == null) return Mono.error(new RequestException("Tenant ID and point ID are required")); return pointStore.get(tenantId, id).switchIfEmpty(Mono.error(new NotFoundException("Point does not exist"))); }
    @Override public Mono<OffsetPage<PointBO>> list(PointFilter filter) { return pointStore.list(filter); }
    @Override public Flux<PointBO> listByIds(Long tenantId, List<Long> ids) { return pointStore.listByIds(tenantId, ids); }
    @Override public Flux<PointBO> listByProfileId(Long tenantId, Long profileId) { return pointStore.listByProfileId(tenantId, profileId); }
    @Override public Flux<PointBO> listByDeviceId(Long tenantId, Long deviceId) { return pointStore.listByDeviceId(tenantId, deviceId); }
    @Override public Mono<Map<String, String>> listUnits(Long tenantId, List<Long> ids) {
        if (tenantId == null) return Mono.error(new RequestException("Tenant ID is required"));
        return pointStore.listByIds(tenantId, ids).collectList().map(points -> {
            Map<String, String> units = new LinkedHashMap<>();
            points.stream().filter(point -> point.getUnit() != null)
                    .forEach(point -> units.put(String.valueOf(point.getId()), point.getUnit()));
            return Map.copyOf(units);
        });
    }
    @Override public Mono<DeviceByPointBO> getDeviceStatisticsByPointId(Long tenantId, Long pointId) {
        return getById(tenantId, pointId).then(pointStore.listConfiguredDeviceIdsByPointId(tenantId, pointId).collectList())
                .flatMap(ids -> deviceStore.listByIds(tenantId, ids).collectList())
                .map(devices -> new DeviceByPointBO((long) devices.size(), devices));
    }
    @Override public Mono<Long> getCountByDeviceId(Long tenantId, Long deviceId) {
        if (tenantId == null || deviceId == null) return Mono.error(new RequestException("Tenant ID and device ID are required"));
        return deviceStore.get(tenantId, deviceId).switchIfEmpty(Mono.error(new NotFoundException("Device does not exist")))
                .then(pointStore.countByDeviceId(tenantId, deviceId));
    }
    @Override public Mono<PointConfigByDeviceBO> getPointConfigByDeviceId(Long tenantId, Long deviceId) {
        if (tenantId == null || deviceId == null) return Mono.error(new RequestException("Tenant ID and device ID are required"));
        return deviceStore.get(tenantId, deviceId).switchIfEmpty(Mono.error(new NotFoundException("Device does not exist")))
                .then(Mono.zip(pointStore.listByDeviceId(tenantId, deviceId).collectList(),
                        pointAttributeConfigStore.listByDeviceId(tenantId, deviceId).collectList()))
                .map(tuple -> {
                    Set<Long> configuredIds = tuple.getT2().stream().map(config -> config.getPointId()).collect(Collectors.toSet());
                    List<PointBO> configuredPoints = tuple.getT1().stream()
                            .filter(point -> configuredIds.contains(point.getId())).toList();
                    return new PointConfigByDeviceBO((long) configuredPoints.size(),
                            (long) tuple.getT1().size() - configuredPoints.size(), configuredPoints);
                });
    }
    private void validate(PointBO value, boolean update) { if (value == null || value.getTenantId() == null || value.getTenantId() <= 0 || value.getProfileId() == null || value.getProfileId() <= 0 || value.getPointName() == null || value.getPointName().isBlank()) throw new RequestException("Tenant ID, profile ID and point name are required"); if (update && (value.getId() == null || value.getVersion() == null || value.getVersion() < 0)) throw new RequestException("Point ID and version are required for update"); value.setPointName(value.getPointName().trim()); }
    private PointBO normalize(PointBO value, boolean update) { if (!update && value.getVersion() == null) value.setVersion(0); LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC); if (!update && value.getCreateTime() == null) value.setCreateTime(now); value.setOperateTime(now); return value; }
}
