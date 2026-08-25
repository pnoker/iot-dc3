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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.biz.PointValueService;
import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.biz.store.PointValueIngestService;
import io.github.pnoker.common.data.biz.store.PointValueLatestService;
import io.github.pnoker.common.data.biz.store.PointValueSampleConverter;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.query.PointValueQuery;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RepositoryException;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceQuery;
import io.github.pnoker.common.facade.entity.query.FacadePointQuery;
import io.github.pnoker.common.tsdb.model.TsdbModel.CursorPage;
import io.github.pnoker.common.tsdb.model.TsdbModel.PointValueSample;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesFilter;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesKey;
import io.github.pnoker.common.tsdb.model.TsdbModel.TimeWindow;
import io.github.pnoker.common.tsdb.model.TsdbModel.TsdbDeadline;
import io.github.pnoker.common.tsdb.spi.TsdbStore;
import io.github.pnoker.common.entity.bo.PointValueVolumeBO;
import io.github.pnoker.common.utils.LocalDateTimeUtil;
import io.github.pnoker.common.utils.TimeRangeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    /**
     * Upper bound for one offset-emulation fetch. The HTTP page shape stays
     * current/size while the port only pages by cursor, so page N is served by
     * fetching the newest N*size rows once and slicing; beyond this cap the
     * request degrades to an empty page rather than an unbounded scan.
     */
    private static final int MAX_PAGE_FETCH = 10_000;

    private static final long RESOLUTION_PAGE_SIZE = 200L;

    private final PointFacade pointFacade;

    private final DeviceFacade deviceFacade;

    private final AlarmRuleTriggerService alarmRuleTriggerService;

    private final PointValueIngestService pointValueIngestService;

    private final PointValueLatestService pointValueLatestService;

    private final PointValueSampleConverter converter;

    private final TsdbStore tsdbStore;

    @Override
    public void save(PointValueBO pointValueBO) {
        if (Objects.isNull(pointValueBO)) {
            return;
        }

        // create_time carries the driver's acquisition timestamp; operate_time
        // is stamped at persistence. Keeping them distinct lets the dashboard
        // measure the collect→store pipeline latency.
        if (Objects.isNull(pointValueBO.getCreateTime())) {
            pointValueBO.setCreateTime(LocalDateTimeUtil.now());
        }
        pointValueBO.setOperateTime(LocalDateTimeUtil.now());
        if (!persistPointValue(pointValueBO)) {
            return;
        }
        try {
            alarmRuleTriggerService.processPointValue(pointValueBO);
        } catch (Exception e) {
            log.warn("Alarm rule evaluation failed after point persistence, messageId={}",
                    pointValueBO.getMessageId(), e);
        }
    }

    @Override
    public void save(List<PointValueBO> pointValueBOList) {
        if (CollectionUtils.isEmpty(pointValueBOList)) {
            return;
        }

        final java.time.LocalDateTime operateTime = LocalDateTimeUtil.now();
        pointValueBOList.forEach(pointValue -> {
            if (Objects.isNull(pointValue.getCreateTime())) {
                pointValue.setCreateTime(LocalDateTimeUtil.now());
            }
            pointValue.setOperateTime(operateTime);
        });

        List<PointValueBO> acceptedValues = persistPointValues(pointValueBOList);
        int rejected = pointValueBOList.size() - acceptedValues.size();
        if (rejected > 0) {
            log.warn("Point-value batch contained replayed or stale-owner events, received={}, accepted={}, rejected={}",
                    pointValueBOList.size(), acceptedValues.size(), rejected);
        }
        if (acceptedValues.isEmpty()) {
            return;
        }
        try {
            alarmRuleTriggerService.processPointValues(acceptedValues);
        } catch (Exception e) {
            // Alarm evaluation runs after persistence: a failure here must not trigger a
            // re-queue that would re-insert the already-persisted rows.
            log.warn("Alarm rule evaluation failed, size={}, skipped", acceptedValues.size(), e);
        }
    }

    @Override
    public List<PointValueBO> history(Long tenantId, Long deviceId, Long pointId, int count) {
        if (Objects.isNull(tenantId) || Objects.isNull(deviceId) || Objects.isNull(pointId)) {
            return Collections.emptyList();
        }
        validateMetadataScope(tenantId, deviceId, pointId);
        if (count < 1) {
            count = 100;
        }
        if (count > 500) {
            count = 500;
        }

        SeriesKey series = new SeriesKey(tenantId, deviceId, pointId);
        List<PointValueSample> samples = tsdbStore.last(SeriesFilter.of(series), count, DEADLINE)
                .getOrDefault(series, List.of());
        return converter.toBOs(samples);
    }

    @Override
    public Page<PointValueBO> latest(PointValueQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        validateMetadataScope(entityQuery.getTenantId(), entityQuery.getDeviceId(), entityQuery.getPointId());

        Page<PointValueBO> entityPageBO = new Page<>();
        entityPageBO.setCurrent(entityQuery.getPage().getCurrent()).setSize(entityQuery.getPage().getSize());

        FacadePointQuery facadeQuery = FacadePointQuery.builder()
                .page(entityQuery.getPage())
                .pointName(entityQuery.getPointName())
                .tenantId(entityQuery.getTenantId())
                .deviceId(entityQuery.getDeviceId())
                .enableFlag(entityQuery.getEnableFlag())
                .build();

        FacadePage<FacadePointBO> page = pointFacade.listByPage(facadeQuery);
        List<Long> pointIds = page.getRecords().stream().map(FacadePointBO::getId).toList();

        if (pointIds.isEmpty()) {
            return entityPageBO;
        }

        Long tenantId = entityQuery.getTenantId();
        Map<Long, PointValueBO> pointValueBOMap = pointValueLatestService
                .listLatest(tenantId, entityQuery.getDeviceId(), pointIds)
                .stream()
                .filter(Objects::nonNull)
                .filter(value -> Objects.nonNull(value.getPointId()))
                .collect(java.util.stream.Collectors.toMap(PointValueBO::getPointId, value -> value));

        // Build the final list maintaining the original pointIds order
        List<PointValueBO> pointValueBOList = pointIds.stream().map(id -> {
            PointValueBO value = pointValueBOMap.get(id);
            return Objects.isNull(value) ? noLatestPointValue(tenantId, entityQuery.getDeviceId(), id)
                    : latestPointValue(value);
        }).toList();

        entityPageBO.setCurrent(page.getCurrent())
                .setSize(page.getSize())
                .setTotal(page.getTotal())
                .setRecords(pointValueBOList);

        return entityPageBO;
    }

    @Override
    public Page<PointValueBO> page(PointValueQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        validateMetadataScope(entityQuery.getTenantId(), entityQuery.getDeviceId(), entityQuery.getPointId());
        if (Objects.isNull(entityQuery.getCreateTimeFrom())) {
            java.time.LocalDateTime from = TimeRangeUtil
                    .resolveFrom(entityQuery.getRangeKey(), entityQuery.getRangeHours());
            if (Objects.nonNull(from)) {
                entityQuery.setCreateTimeFrom(from);
            }
        }

        long current = Math.max(1, entityQuery.getPage().getCurrent());
        long size = Math.max(1, entityQuery.getPage().getSize());
        SeriesFilter filter = resolveSeriesFilter(entityQuery);
        if (Objects.isNull(filter)) {
            return emptyPage(current, size);
        }
        TimeWindow window = new TimeWindow(
                Objects.nonNull(entityQuery.getCreateTimeFrom())
                        ? converter.toInstant(entityQuery.getCreateTimeFrom()) : Instant.EPOCH,
                Instant.now());

        Page<PointValueBO> entityPageBO = emptyPage(current, size);
        entityPageBO.setTotal(tsdbStore.count(filter, window, DEADLINE));

        int fetch = (int) Math.min(current * size, MAX_PAGE_FETCH);
        int offset = (int) Math.min((current - 1) * size, MAX_PAGE_FETCH);
        CursorPage<PointValueSample> cursorPage = tsdbStore.history(filter, window, null, fetch, DEADLINE);
        if (offset < cursorPage.items().size()) {
            entityPageBO.setRecords(converter.toBOs(
                    cursorPage.items().subList(offset, Math.min((int) (offset + size), cursorPage.items().size()))));
        }
        return entityPageBO;
    }

    @Override
    public List<PointValueVolumeBO> seriesVolumes(Long tenantId, Instant from) {
        if (Objects.isNull(tenantId)) {
            return List.of();
        }
        TimeWindow window = new TimeWindow(Objects.nonNull(from) ? from : Instant.EPOCH, Instant.now());
        return tsdbStore.seriesCounts(tenantId, window, DEADLINE).stream()
                .map(row -> new PointValueVolumeBO(row.series().deviceId(), row.series().pointId(), row.count()))
                .toList();
    }

    /**
     * Translate the query's name/enable filters into a series filter via
     * relational metadata (§6.2 of the TSDB design). Returns {@code null} when
     * the filters provably match nothing; a tenant-wide filter when no series
     * dimension is restricted; otherwise the explicit (device, point) pair
     * set — pairs are enumerated through profile bindings because a point is
     * reported under every device bound to its profile.
     */
    private SeriesFilter resolveSeriesFilter(PointValueQuery query) {
        Long tenantId = query.getTenantId();
        if (Objects.isNull(tenantId)) {
            return SeriesFilter.tenantWide(0L);
        }

        boolean deviceScoped = isValidId(query.getDeviceId());
        boolean deviceNamed = !deviceScoped && Objects.nonNull(query.getDeviceName())
                && !query.getDeviceName().isBlank();
        boolean pointScoped = isValidId(query.getPointId());
        boolean pointFiltered = !pointScoped && (Objects.nonNull(query.getPointName())
                || Objects.nonNull(query.getEnableFlag()));

        if (!deviceScoped && !deviceNamed && !pointScoped && !pointFiltered) {
            return SeriesFilter.tenantWide(tenantId);
        }

        // Point dimension: id when scoped, facade search when filtered.
        Map<Long, Long> pointsById = new LinkedHashMap<>();
        if (pointScoped) {
            FacadePointBO point = pointFacade.getById(tenantId, query.getPointId());
            if (Objects.isNull(point)) {
                return null;
            }
            pointsById.put(point.getId(), point.getProfileId());
        } else if (pointFiltered) {
            FacadePointQuery facadeQuery = FacadePointQuery.builder()
                    .tenantId(tenantId)
                    .pointName(query.getPointName())
                    .enableFlag(query.getEnableFlag())
                    .build();
            for (FacadePointBO point : pageThroughPoints(facadeQuery)) {
                pointsById.put(point.getId(), point.getProfileId());
            }
            if (pointsById.isEmpty()) {
                return null;
            }
        }

        // Device dimension: id when scoped, facade search when named.
        Map<Long, Long> devicesById = new LinkedHashMap<>();
        if (deviceScoped) {
            FacadeDeviceBO device = deviceFacade.getById(tenantId, query.getDeviceId());
            if (Objects.isNull(device)) {
                return null;
            }
            devicesById.put(device.getId(), device.getProfileId());
        } else if (deviceNamed) {
            FacadeDeviceQuery facadeQuery = FacadeDeviceQuery.builder()
                    .tenantId(tenantId)
                    .deviceName(query.getDeviceName())
                    .build();
            for (FacadeDeviceBO device : pageThroughDevices(facadeQuery)) {
                devicesById.put(device.getId(), device.getProfileId());
            }
            if (devicesById.isEmpty()) {
                return null;
            }
        }

        // Both dimensions restricted: the old SQL applied the predicates
        // independently on the row, so take the plain cross product.
        if (!pointsById.isEmpty() && !devicesById.isEmpty()) {
            List<SeriesKey> series = new ArrayList<>();
            for (Map.Entry<Long, Long> device : devicesById.entrySet()) {
                for (Map.Entry<Long, Long> point : pointsById.entrySet()) {
                    series.add(new SeriesKey(tenantId, device.getKey(), point.getKey()));
                }
            }
            return SeriesFilter.of(series);
        }

        // Only one dimension restricted: enumerate the plausible pairs through
        // the profile binding on the unrestricted side.
        List<SeriesKey> series = new ArrayList<>();
        if (!pointsById.isEmpty()) {
            for (Map.Entry<Long, Long> point : pointsById.entrySet()) {
                for (FacadeDeviceBO device : deviceFacade.listByProfileId(tenantId, point.getValue())) {
                    series.add(new SeriesKey(tenantId, device.getId(), point.getKey()));
                }
            }
        } else {
            for (Map.Entry<Long, Long> device : devicesById.entrySet()) {
                FacadePointQuery facadeQuery = FacadePointQuery.builder()
                        .tenantId(tenantId)
                        .profileId(device.getValue())
                        .build();
                for (FacadePointBO point : pageThroughPoints(facadeQuery)) {
                    series.add(new SeriesKey(tenantId, device.getKey(), point.getId()));
                }
            }
        }
        return series.isEmpty() ? null : SeriesFilter.of(series);
    }

    private List<FacadePointBO> pageThroughPoints(FacadePointQuery template) {
        List<FacadePointBO> out = new ArrayList<>();
        long current = 1;
        while (true) {
            Pages pages = new Pages();
            pages.setCurrent(current);
            pages.setSize(RESOLUTION_PAGE_SIZE);
            FacadePointQuery paged = FacadePointQuery.builder()
                    .tenantId(template.getTenantId())
                    .pointName(template.getPointName())
                    .pointCode(template.getPointCode())
                    .pointTypeFlag(template.getPointTypeFlag())
                    .rwFlag(template.getRwFlag())
                    .profileId(template.getProfileId())
                    .enableFlag(template.getEnableFlag())
                    .version(template.getVersion())
                    .deviceId(template.getDeviceId())
                    .page(pages)
                    .build();
            FacadePage<FacadePointBO> page = pointFacade.listByPage(paged);
            out.addAll(page.getRecords());
            if (out.size() >= page.getTotal() || page.getRecords().isEmpty()) {
                return out;
            }
            current++;
        }
    }

    private List<FacadeDeviceBO> pageThroughDevices(FacadeDeviceQuery template) {
        List<FacadeDeviceBO> out = new ArrayList<>();
        long current = 1;
        while (true) {
            Pages pages = new Pages();
            pages.setCurrent(current);
            pages.setSize(RESOLUTION_PAGE_SIZE);
            FacadeDeviceQuery paged = FacadeDeviceQuery.builder()
                    .tenantId(template.getTenantId())
                    .deviceName(template.getDeviceName())
                    .deviceCode(template.getDeviceCode())
                    .driverId(template.getDriverId())
                    .enableFlag(template.getEnableFlag())
                    .version(template.getVersion())
                    .profileId(template.getProfileId())
                    .page(pages)
                    .build();
            FacadePage<FacadeDeviceBO> page = deviceFacade.listByPage(paged);
            out.addAll(page.getRecords());
            if (out.size() >= page.getTotal() || page.getRecords().isEmpty()) {
                return out;
            }
            current++;
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
    private void validateMetadataScope(Long tenantId, Long deviceId, Long pointId) {
        if (Objects.isNull(tenantId)) {
            return;
        }

        FacadeDeviceBO device = null;
        if (isValidId(deviceId)) {
            device = deviceFacade.getById(tenantId, deviceId);
            if (Objects.isNull(device)) {
                throw new NotFoundException("Device does not exist");
            }
        }

        FacadePointBO point = null;
        if (isValidId(pointId)) {
            point = pointFacade.getById(tenantId, pointId);
            if (Objects.isNull(point)) {
                throw new NotFoundException("Point does not exist");
            }
        }

        if (Objects.nonNull(device) && Objects.nonNull(point)
                && (Objects.isNull(device.getProfileId()) || !Objects.equals(device.getProfileId(), point.getProfileId()))) {
            throw new NotFoundException("Point does not exist");
        }
    }

    /**
     * Return whether the given id is a valid reference (non-null and positive).
     *
     * @param id the id to test
     * @return {@code true} if the id is a positive non-null value
     */
    private boolean isValidId(Long id) {
        return Objects.nonNull(id) && id > 0;
    }

    private boolean persistPointValue(PointValueBO pointValueBO) {
        try {
            return pointValueIngestService.saveValue(pointValueBO);
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

    private List<PointValueBO> persistPointValues(List<PointValueBO> pointValueBOList) {
        try {
            return pointValueIngestService.saveValues(pointValueBOList);
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

    private Page<PointValueBO> emptyPage(long current, long size) {
        Page<PointValueBO> page = new Page<>(current, size);
        page.setRecords(List.of());
        return page;
    }

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
