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
package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.biz.PointValueService;
import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.biz.store.PointValueIngestService;
import io.github.pnoker.common.data.biz.store.PointValueLatestService;
import io.github.pnoker.common.data.biz.store.PointValueSampleConverter;
import io.github.pnoker.common.data.repository.ReactiveTsdbStore;
import io.github.pnoker.common.data.support.PointValueCursorCodec;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.bo.PointValueVolumeBO;
import io.github.pnoker.common.entity.query.PointValueQuery;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceOffsetQuery;
import io.github.pnoker.common.facade.entity.query.FacadePointOffsetQuery;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesFilter;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesKey;
import io.github.pnoker.common.tsdb.model.TsdbModel.TimeWindow;
import io.github.pnoker.common.tsdb.model.TsdbModel.TsdbDeadline;
import io.github.pnoker.common.utils.LocalDateTimeUtil;
import io.github.pnoker.common.utils.TimeRangeUtil;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Business service implementation for point value operations. Reads go through
 * the TSDB port (history/page) and the relational latest projection; writes go
 * through the ingest orchestration.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointValueServiceImpl implements PointValueService {

    /**
     * Read deadline shared by the value-query paths (S18 runaway-scan guard).
     */
    private static final TsdbDeadline DEADLINE = TsdbDeadline.ofSeconds(30);

    private static final int RESOLUTION_PAGE_SIZE = PageRequest.MAX_LIMIT;

    private final PointFacade pointFacade;

    private final DeviceFacade deviceFacade;

    private final AlarmRuleTriggerService alarmRuleTriggerService;

    private final PointValueIngestService pointValueIngestService;

    private final PointValueLatestService pointValueLatestService;

    private final PointValueSampleConverter converter;

    private final ReactiveTsdbStore reactiveTsdbStore;

    private final PointValueCursorCodec pointValueCursorCodec;

    @Override
    public Mono<Void> save(PointValueBO pointValueBO) {
        if (pointValueBO == null) return Mono.empty();
        return Mono.defer(() -> {
            if (pointValueBO.getCreateTime() == null) pointValueBO.setCreateTime(LocalDateTimeUtil.now());
            pointValueBO.setOperateTime(LocalDateTimeUtil.now());
            return pointValueIngestService
                    .saveValue(pointValueBO)
                    .flatMap(accepted -> accepted
                            ? alarmRuleTriggerService
                                    .processPointValue(pointValueBO)
                                    .then(Mono.defer(
                                            () -> safe(pointValueIngestService.markProcessed(List.of(pointValueBO)))))
                            : Mono.empty());
        });
    }

    @Override
    public Mono<Void> save(List<PointValueBO> pointValueBOList) {
        if (CollectionUtils.isEmpty(pointValueBOList)) return Mono.empty();
        return Mono.defer(() -> {
            LocalDateTime operateTime = LocalDateTimeUtil.now();
            pointValueBOList.forEach(value -> {
                if (value.getCreateTime() == null) value.setCreateTime(LocalDateTimeUtil.now());
                value.setOperateTime(operateTime);
            });
            return pointValueIngestService
                    .saveValues(pointValueBOList)
                    .flatMap(acceptedValues -> acceptedValues.isEmpty()
                            ? Mono.empty()
                            : alarmRuleTriggerService
                                    .processPointValues(acceptedValues)
                                    .then(Mono.defer(
                                            () -> safe(pointValueIngestService.markProcessed(acceptedValues)))));
        });
    }

    @Override
    public Mono<io.github.pnoker.db.r2dbc.core.page.CursorPage<PointValueBO>> history(
            Long tenantId, Long deviceId, Long pointId, String cursor, int limit) {
        if (!isValidId(tenantId) || !isValidId(deviceId) || !isValidId(pointId)) {
            return Mono.error(new IllegalArgumentException("tenantId, deviceId and pointId must be positive"));
        }
        if (limit < 1 || limit > PageRequest.MAX_LIMIT) {
            return Mono.error(new IllegalArgumentException("limit must be between 1 and " + PageRequest.MAX_LIMIT));
        }
        int bounded = limit;
        return Mono.defer(() -> {
            io.github.pnoker.common.tsdb.model.TsdbModel.Cursor internalCursor = cursor == null || cursor.isBlank()
                    ? null
                    : cursorCodec().decodeCursor(cursor, tenantId, deviceId, pointId);
            TimeWindow requestedWindow = new TimeWindow(Instant.EPOCH, Instant.now());
            TimeWindow window = internalCursor != null && internalCursor.windowFrom() != null
                    ? new TimeWindow(internalCursor.windowFrom(), internalCursor.windowTo())
                    : requestedWindow;
            return validateMetadataScopeReactive(tenantId, deviceId, pointId)
                    .then(Mono.defer(() -> safe(reactiveTsdbStore.history(
                            SeriesFilter.of(new SeriesKey(tenantId, deviceId, pointId)),
                            window,
                            internalCursor,
                            bounded,
                            DEADLINE))))
                    .map(page -> {
                        List<PointValueBO> items = converter.toBOs(page.items());
                        String nextCursor = page.nextCursor() == null
                                ? null
                                : cursorCodec()
                                        .encodeCursor(
                                                tenantId,
                                                deviceId,
                                                pointId,
                                                page.nextCursor().withWindow(window));
                        return io.github.pnoker.db.r2dbc.core.page.CursorPage.of(items, nextCursor);
                    });
        });
    }

    @Override
    public Mono<OffsetPage<PointValueBO>> latest(PointValueQuery entityQuery) {
        PointValueQuery query = entityQuery == null ? new PointValueQuery() : entityQuery;
        long offset = normalizeOffset(query.getOffset());
        int limit = normalizeLimit(query.getLimit());
        Long tenantId = query.getTenantId();
        Mono<Void> validation = validateMetadataScopeReactive(tenantId, query.getDeviceId(), query.getPointId());
        Mono<OffsetPage<FacadePointBO>> points;
        if (isValidId(query.getPointId())) {
            points = safe(pointFacade.getByIdReactive(tenantId, query.getPointId()))
                    .map(point -> offset == 0
                            ? OffsetPage.<FacadePointBO>of(List.of(point), 0, limit, 1L)
                            : OffsetPage.<FacadePointBO>of(List.of(), offset, limit, 1L))
                    .defaultIfEmpty(OffsetPage.<FacadePointBO>of(List.of(), offset, limit, 0L));
        } else {
            FacadePointOffsetQuery pointQuery = new FacadePointOffsetQuery(
                    tenantId,
                    query.getPointName(),
                    null,
                    null,
                    null,
                    null,
                    query.getEnableFlag(),
                    null,
                    null,
                    null,
                    query.getDeviceId(),
                    offset,
                    limit,
                    query.getSort());
            points = pointFacade.listReactive(pointQuery);
        }
        return validation.then(points).flatMap(page -> {
            List<Long> pointIds =
                    page.items().stream().map(FacadePointBO::getId).toList();
            if (pointIds.isEmpty()) return Mono.just(OffsetPage.of(List.of(), offset, limit, page.total()));
            return pointValueLatestService
                    .listLatest(tenantId, query.getDeviceId(), pointIds)
                    .collectMap(PointValueBO::getPointId)
                    .map(values -> {
                        List<PointValueBO> records = page.items().stream()
                                .map(point -> {
                                    PointValueBO value = values.get(point.getId());
                                    return value == null
                                            ? noLatestPointValue(tenantId, query.getDeviceId(), point.getId())
                                            : latestPointValue(value);
                                })
                                .toList();
                        return OffsetPage.of(records, page.offset(), page.limit(), page.total());
                    });
        });
    }

    @Override
    public Mono<io.github.pnoker.db.r2dbc.core.page.CursorPage<PointValueBO>> page(PointValueQuery entityQuery) {
        PointValueQuery query = entityQuery == null ? new PointValueQuery() : entityQuery;
        return Mono.defer(() -> {
                    if (!isValidId(query.getTenantId())) {
                        return Mono.error(new IllegalArgumentException("tenantId must be positive"));
                    }
                    if (query.getOffset() != 0) {
                        return Mono.error(new IllegalArgumentException(
                                "offset is not supported for point-value history; use cursor"));
                    }
                    int limit = normalizeLimit(query.getLimit());
                    LocalDateTime from = query.getCreateTimeFrom() == null
                            ? TimeRangeUtil.resolveFrom(query.getRangeKey(), query.getRangeHours())
                            : query.getCreateTimeFrom();
                    TimeWindow window = new TimeWindow(
                            query.getCreateTimeFrom() == null
                                    ? (from == null ? Instant.EPOCH : converter.toInstant(from))
                                    : converter.toInstant(from),
                            Instant.now());
                    return validateMetadataScopeReactive(query.getTenantId(), query.getDeviceId(), query.getPointId())
                            .then(resolveSeriesFilterReactive(query))
                            .map(filter -> new PreparedCursorPage(
                                    filter.orElse(null),
                                    window,
                                    limit,
                                    PointValueCursorCodec.normalizeFingerprint(
                                            cursorFingerprint(query, filter.orElse(null))),
                                    query.getCursor()));
                })
                .flatMap(prepared -> {
                    if (prepared.filter() == null) {
                        return Mono.just(io.github.pnoker.db.r2dbc.core.page.CursorPage.of(List.of(), null));
                    }
                    io.github.pnoker.common.tsdb.model.TsdbModel.Cursor internalCursor = prepared.cursor() == null
                                    || prepared.cursor().isBlank()
                            ? null
                            : cursorCodec()
                                    .decode(prepared.cursor(), prepared.filter().tenantId(), prepared.fingerprint());
                    TimeWindow window = internalCursor != null && internalCursor.windowFrom() != null
                            ? new TimeWindow(internalCursor.windowFrom(), internalCursor.windowTo())
                            : prepared.window();
                    return reactiveTsdbStore
                            .history(prepared.filter(), window, internalCursor, prepared.limit(), DEADLINE)
                            .map(page -> {
                                List<PointValueBO> records = converter.toBOs(page.items());
                                String nextCursor = page.nextCursor() == null
                                        ? null
                                        : cursorCodec()
                                                .encode(
                                                        prepared.filter().tenantId(),
                                                        prepared.fingerprint(),
                                                        page.nextCursor().withWindow(window));
                                return io.github.pnoker.db.r2dbc.core.page.CursorPage.of(records, nextCursor);
                            });
                });
    }

    private String cursorFingerprint(PointValueQuery query, SeriesFilter filter) {
        String series = filter == null || filter.tenantWide()
                ? "*"
                : filter.series().stream()
                        .sorted(java.util.Comparator.comparingLong(SeriesKey::deviceId)
                                .thenComparingLong(SeriesKey::pointId))
                        .map(key -> key.deviceId() + ":" + key.pointId())
                        .collect(java.util.stream.Collectors.joining(","));
        return "tenant=" + query.getTenantId() + ";series=" + series + ";rangeKey="
                + Objects.requireNonNullElse(query.getRangeKey(), "") + ";rangeHours="
                + Objects.requireNonNullElse(query.getRangeHours(), 0) + ";from="
                + Objects.requireNonNullElse(query.getCreateTimeFrom(), "")
                + ";sort=create_time.desc,tenant_id.desc,device_id.desc,point_id.desc,message_id.desc";
    }

    private PointValueCursorCodec cursorCodec() {
        if (pointValueCursorCodec == null) {
            throw new IllegalStateException("Point-value cursor codec is not configured");
        }
        return pointValueCursorCodec;
    }

    @Override
    public Mono<List<PointValueVolumeBO>> seriesVolumes(Long tenantId, Instant from) {
        if (!isValidId(tenantId)) return Mono.just(List.of());
        TimeWindow window = new TimeWindow(from == null ? Instant.EPOCH : from, Instant.now());
        return reactiveTsdbStore
                .seriesCounts(tenantId, window, DEADLINE)
                .map(row -> new PointValueVolumeBO(
                        row.series().deviceId(), row.series().pointId(), row.count()))
                .collectList();
    }

    /**
     * Resolve query dimensions through the reactive manager facades. The returned
     * optional is empty when a restricted dimension has no matching metadata.
     */
    private Mono<Optional<SeriesFilter>> resolveSeriesFilterReactive(PointValueQuery query) {
        Long tenantId = query.getTenantId();
        if (!isValidId(tenantId)) {
            return Mono.error(new IllegalArgumentException("tenantId must be positive"));
        }

        boolean deviceScoped = isValidId(query.getDeviceId());
        boolean deviceNamed = !deviceScoped
                && query.getDeviceName() != null
                && !query.getDeviceName().isBlank();
        boolean pointScoped = isValidId(query.getPointId());
        boolean pointFiltered = !pointScoped && (query.getPointName() != null || query.getEnableFlag() != null);
        if (!deviceScoped && !deviceNamed && !pointScoped && !pointFiltered) {
            return Mono.just(Optional.of(SeriesFilter.tenantWide(tenantId)));
        }

        Mono<List<FacadePointBO>> points = pointScoped
                ? safe(pointFacade.getByIdReactive(tenantId, query.getPointId()))
                        .map(List::of)
                        .defaultIfEmpty(List.of())
                : pointFiltered
                        ? pageThroughPointsReactive(tenantId, query.getPointName(), query.getEnableFlag(), null)
                                .collectList()
                        : Mono.just(List.of());
        Mono<List<FacadeDeviceBO>> devices = deviceScoped
                ? safe(deviceFacade.getByIdReactive(tenantId, query.getDeviceId()))
                        .map(List::of)
                        .defaultIfEmpty(List.of())
                : deviceNamed
                        ? pageThroughDevicesReactive(tenantId, query.getDeviceName(), null, null, null)
                                .collectList()
                        : Mono.just(List.of());

        return Mono.zip(points, devices).flatMap(tuple -> {
            List<FacadePointBO> pointRows = tuple.getT1();
            List<FacadeDeviceBO> deviceRows = tuple.getT2();
            if ((pointScoped || pointFiltered) && pointRows.isEmpty()) return Mono.just(Optional.<SeriesFilter>empty());
            if ((deviceScoped || deviceNamed) && deviceRows.isEmpty()) return Mono.just(Optional.<SeriesFilter>empty());

            Map<Long, Long> pointsById = pointRows.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            FacadePointBO::getId,
                            FacadePointBO::getProfileId,
                            (left, right) -> left,
                            LinkedHashMap::new));
            Map<Long, Long> devicesById = deviceRows.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            FacadeDeviceBO::getId,
                            FacadeDeviceBO::getProfileId,
                            (left, right) -> left,
                            LinkedHashMap::new));
            Mono<List<FacadeDeviceBO>> profileDevices = pointsById.isEmpty()
                    ? Mono.just(List.of())
                    : Flux.fromIterable(pointsById.values())
                            .distinct()
                            .flatMap(profileId -> deviceFacade.listByProfileIdReactive(tenantId, profileId))
                            .collectList();
            Mono<List<FacadePointBO>> profilePoints = devicesById.isEmpty()
                    ? Mono.just(List.of())
                    : Flux.fromIterable(devicesById.values())
                            .distinct()
                            .flatMap(profileId -> pageThroughPointsReactive(tenantId, null, null, profileId))
                            .collectList();
            return new ResolvedDimensions(pointsById, devicesById, profileDevices, profilePoints)
                    .toFilter(tenantId)
                    .map(Optional::of)
                    .defaultIfEmpty(Optional.empty());
        });
    }

    private Flux<FacadePointBO> pageThroughPointsReactive(
            Long tenantId, String pointName, io.github.pnoker.common.enums.EnableFlagEnum enableFlag, Long profileId) {
        return Flux.defer(() -> fetchPointPage(tenantId, pointName, enableFlag, profileId, 0))
                .expand(state -> state.page().hasNext()
                        ? fetchPointPage(
                                tenantId,
                                pointName,
                                enableFlag,
                                profileId,
                                state.offset() + state.page().items().size())
                        : Mono.empty())
                .concatMapIterable(state -> state.page().items());
    }

    private Mono<PageState<FacadePointBO>> fetchPointPage(
            Long tenantId,
            String pointName,
            io.github.pnoker.common.enums.EnableFlagEnum enableFlag,
            Long profileId,
            long offset) {
        FacadePointOffsetQuery request = new FacadePointOffsetQuery(
                tenantId,
                pointName,
                null,
                null,
                null,
                profileId,
                enableFlag,
                null,
                null,
                null,
                null,
                offset,
                RESOLUTION_PAGE_SIZE,
                List.of());
        return safe(pointFacade.listReactive(request))
                .defaultIfEmpty(OffsetPage.of(List.of(), offset, RESOLUTION_PAGE_SIZE, offset))
                .map(page -> new PageState<>(offset, page));
    }

    private Flux<FacadeDeviceBO> pageThroughDevicesReactive(
            Long tenantId,
            String deviceName,
            Long driverId,
            Long profileId,
            io.github.pnoker.common.enums.EnableFlagEnum enableFlag) {
        return Flux.defer(() -> fetchDevicePage(tenantId, deviceName, driverId, profileId, enableFlag, 0))
                .expand(state -> state.page().hasNext()
                        ? fetchDevicePage(
                                tenantId,
                                deviceName,
                                driverId,
                                profileId,
                                enableFlag,
                                state.offset() + state.page().items().size())
                        : Mono.empty())
                .concatMapIterable(state -> state.page().items());
    }

    private Mono<PageState<FacadeDeviceBO>> fetchDevicePage(
            Long tenantId,
            String deviceName,
            Long driverId,
            Long profileId,
            io.github.pnoker.common.enums.EnableFlagEnum enableFlag,
            long offset) {
        FacadeDeviceOffsetQuery request = new FacadeDeviceOffsetQuery(
                tenantId,
                deviceName,
                null,
                driverId,
                profileId,
                enableFlag,
                null,
                null,
                null,
                offset,
                RESOLUTION_PAGE_SIZE,
                List.of());
        return safe(deviceFacade.listReactive(request))
                .defaultIfEmpty(OffsetPage.of(List.of(), offset, RESOLUTION_PAGE_SIZE, offset))
                .map(page -> new PageState<>(offset, page));
    }

    private record PageState<T>(long offset, OffsetPage<T> page) {}

    private record ResolvedDimensions(
            Map<Long, Long> pointsById,
            Map<Long, Long> devicesById,
            Mono<List<FacadeDeviceBO>> profileDevices,
            Mono<List<FacadePointBO>> profilePoints) {
        Mono<SeriesFilter> toFilter(Long tenantId) {
            if (!pointsById.isEmpty() && !devicesById.isEmpty()) {
                return Mono.just(SeriesFilter.of(crossProduct(tenantId, devicesById.keySet(), pointsById.keySet())));
            }
            if (!pointsById.isEmpty()) {
                return profileDevices.flatMap(devices -> {
                    List<SeriesKey> series = devices.stream()
                            .flatMap(device -> pointsById.keySet().stream()
                                    .map(pointId -> new SeriesKey(tenantId, device.getId(), pointId)))
                            .toList();
                    return series.isEmpty() ? Mono.empty() : Mono.just(SeriesFilter.of(series));
                });
            }
            return profilePoints.flatMap(points -> {
                List<SeriesKey> series = points.stream()
                        .flatMap(point -> devicesById.keySet().stream()
                                .map(deviceId -> new SeriesKey(tenantId, deviceId, point.getId())))
                        .toList();
                return series.isEmpty() ? Mono.empty() : Mono.just(SeriesFilter.of(series));
            });
        }

        private static List<SeriesKey> crossProduct(
                Long tenantId, java.util.Collection<Long> devices, java.util.Collection<Long> points) {
            List<SeriesKey> result = new ArrayList<>(devices.size() * points.size());
            for (Long deviceId : devices)
                for (Long pointId : points) {
                    result.add(new SeriesKey(tenantId, deviceId, pointId));
                }
            return result;
        }
    }

    /**
     * Validate that the device and point exist within the tenant and that the point
     * belongs to the device's profile. A no-op when {@code tenantId} is null; throws
     * {@link NotFoundException} on any missing or mismatched entity.
     *
     * @param tenantId tenant scope
     * @param deviceId device id to validate, skipped when not a positive id
     * @param pointId  point id to validate, skipped when not a positive id
     */
    private Mono<Void> validateMetadataScopeReactive(Long tenantId, Long deviceId, Long pointId) {
        if (!isValidId(tenantId)) return Mono.empty();
        boolean hasDevice = isValidId(deviceId);
        boolean hasPoint = isValidId(pointId);
        if (hasDevice && hasPoint) {
            return safe(deviceFacade.getByIdReactive(tenantId, deviceId))
                    .map(Optional::of)
                    .defaultIfEmpty(Optional.empty())
                    .zipWith(safe(pointFacade.getByIdReactive(tenantId, pointId))
                            .map(Optional::of)
                            .defaultIfEmpty(Optional.empty()))
                    .flatMap(tuple -> tuple.getT1().isEmpty() || tuple.getT2().isEmpty()
                            ? Mono.error(new NotFoundException("Device or point does not exist"))
                            : Objects.equals(
                                            tuple.getT1().get().getProfileId(),
                                            tuple.getT2().get().getProfileId())
                                    ? Mono.empty()
                                    : Mono.error(new NotFoundException("Point does not exist")));
        }
        if (hasDevice)
            return safe(deviceFacade.getByIdReactive(tenantId, deviceId))
                    .switchIfEmpty(Mono.error(new NotFoundException("Device does not exist")))
                    .then();
        if (hasPoint)
            return safe(pointFacade.getByIdReactive(tenantId, pointId))
                    .switchIfEmpty(Mono.error(new NotFoundException("Point does not exist")))
                    .then();
        return Mono.empty();
    }

    /**
     * Return whether the given id is a valid reference (non-null and positive).
     *
     * @param id the id to test
     * @return {@code true} if the id is a positive non-null value
     */
    private <T> Mono<T> safe(Mono<T> publisher) {
        return publisher == null ? Mono.empty() : publisher;
    }

    private boolean isValidId(Long id) {
        return Objects.nonNull(id) && id > 0;
    }

    private long normalizeOffset(long offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be non-negative");
        }
        return offset;
    }

    private int normalizeLimit(int limit) {
        if (limit < 1 || limit > PageRequest.MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + PageRequest.MAX_LIMIT);
        }
        return limit;
    }

    private record PreparedCursorPage(
            SeriesFilter filter, TimeWindow window, int limit, String fingerprint, String cursor) {}

    /**
     * Build a placeholder point value indicating no latest value is available.
     *
     * @param tenantId tenant scope
     * @param deviceId device id
     * @param pointId  point id
     * @return a point value marked as having no latest value
     */
    private PointValueBO noLatestPointValue(Long tenantId, Long deviceId, Long pointId) {
        return PointValueBO.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .pointId(pointId)
                .rawValue(DataConstant.PointValue.NO_LATEST_VALUE)
                .calValue(DataConstant.PointValue.NO_LATEST_VALUE)
                .hasLatestValue(false)
                .build();
    }

    /**
     * Mark a resolved point value as having a latest value.
     *
     * @param pointValueBO the point value to flag
     * @return the same point value, marked as having a latest value
     */
    private PointValueBO latestPointValue(PointValueBO pointValueBO) {
        pointValueBO.setHasLatestValue(true);
        return pointValueBO;
    }
}
