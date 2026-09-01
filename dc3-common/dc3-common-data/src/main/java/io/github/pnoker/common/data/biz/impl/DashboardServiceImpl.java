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

import io.github.pnoker.common.constant.common.TimeConstant;
import io.github.pnoker.common.data.biz.DashboardService;
import io.github.pnoker.common.data.biz.store.PointValueSampleConverter;
import io.github.pnoker.common.data.biz.store.PointValueLatestService;
import io.github.pnoker.common.data.entity.bo.dashboard.AlertItemRow;
import io.github.pnoker.common.data.entity.vo.dashboard.*;
import io.github.pnoker.common.data.repository.ReactiveAlertStore;
import io.github.pnoker.common.data.repository.ReactiveAlertAnalyticsStore;
import io.github.pnoker.common.enums.AlarmTypeEnum;
import io.github.pnoker.common.enums.ConfirmFlagEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.tsdb.model.TsdbModel.BucketAggregate;
import io.github.pnoker.common.tsdb.model.TsdbModel.DimensionCount;
import io.github.pnoker.common.tsdb.model.TsdbModel.GroupDimension;
import io.github.pnoker.common.tsdb.model.TsdbModel.LatencyBin;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesFilter;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesLastSeen;
import io.github.pnoker.common.tsdb.model.TsdbModel.TimeWindow;
import io.github.pnoker.common.tsdb.model.TsdbModel.TsdbDeadline;
import io.github.pnoker.common.data.repository.ReactiveTsdbStore;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.github.pnoker.common.data.constant.DashboardLimits.*;

/**
 * Business service implementation for dashboard aggregation operations.
 * Point-value statistics go through the TSDB port (S13 analytics facet) and
 * the relational latest projection; alert statistics stay on the alert mapper.
 *
 * @author pnoker
 * @since 2026.5.2
 */
@Slf4j
@Service("dataDashboardService")
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final TsdbDeadline DEADLINE = TsdbDeadline.ofSeconds(20);

    /**
     * UI layout of the latency histogram — six buckets, fixed edges.
     */
    private static final List<Long> LATENCY_EDGES_MS = List.of(100L, 500L, 1000L, 5000L, 30000L);

    /**
     * Dimensions accepted for the top-N grouping. The port's GroupDimension
     * enum is the whitelist — never pass user input further.
     */
    private static final Map<String, GroupDimension> DIMENSIONS = Map.of(
            "device", GroupDimension.DEVICE, "point", GroupDimension.POINT, "driver", GroupDimension.DRIVER);

    private static final Set<String> GRANULARITY = Set.of("hour", "day");

    /**
     * Whitelist for the alert source parameter.
     */
    private static final Set<String> ALERT_SOURCES = Set.of(SOURCE_DEVICE, SOURCE_DRIVER, SOURCE_POINT);

    private final PointValueLatestService pointValueLatestService;

    private final ReactiveAlertStore alertStore;

    private final ReactiveAlertAnalyticsStore alertAnalyticsStore;

    private final DeviceFacade deviceFacade;

    private final PointFacade pointFacade;

    private final DriverFacade driverFacade;

    private final PointValueSampleConverter converter;

    private final ReactiveTsdbStore tsdbStore;

    /**
     * BucketRow.key is Object (shared across SMALLINT / VARCHAR / BIGINT group columns);
     * stringify for the VO.
     */
    private static String asString(Object v) {
        return Objects.isNull(v) ? null : v.toString();
    }

    private TimeWindow windowSince(LocalDateTime from) {
        return new TimeWindow(converter.toInstant(from), Instant.now());
    }

    @Override
    public Mono<List<LatencyBucketVO>> latencyHistogram(Long tenantId, int rangeHours) {
        int hours = Math.clamp(rangeHours, 1, MAX_HOURS_90D);
        LocalDateTime to = LocalDateTime.now(TimeConstant.DEFAULT_ZONEID);
        LocalDateTime from = to.minusHours(hours);
        // Capability-gated op: stores without store-side binning (e.g. TDengine)
        // degrade to zero-filled bins instead of failing the dashboard.
        Mono<List<LatencyBin>> bins = tsdbStore.capabilities().latencyHistogram()
                ? tsdbStore.latencyHistogram(tenantId, windowSince(from), LATENCY_EDGES_MS, DEADLINE)
                : Mono.just(List.of());
        return bins.map(values -> {
            List<LatencyBucketVO> out = new ArrayList<>(LATENCY_EDGES_MS.size() + 1);
            for (int i = 0; i <= LATENCY_EDGES_MS.size(); i++) {
                LatencyBucketVO vo = new LatencyBucketVO(); vo.setBin(i);
                vo.setCount(i < values.size() ? values.get(i).count() : 0L); out.add(vo);
            }
            return out;
        });
    }

    @Override
    public Mono<List<ActivityCellVO>> hourlyActivity(Long tenantId, int rangeHours) {
        int hours = Math.clamp(rangeHours, 1, MAX_HOURS_90D);
        LocalDateTime to = LocalDateTime.now(TimeConstant.DEFAULT_ZONEID);
        LocalDateTime from = to.minusHours(hours);
        return tsdbStore.bucketedCount(tenantId, windowSince(from), Duration.ofHours(1), DEADLINE).map(buckets -> {
            long[][] grid = new long[7][24];
            for (BucketAggregate bucket : buckets) {
                LocalDateTime wallClock = converter.toWallClock(bucket.bucketStart());
                if (wallClock != null) grid[wallClock.getDayOfWeek().getValue() % 7][wallClock.getHour()] = bucket.sampleCount();
            }
            List<ActivityCellVO> out = new ArrayList<>(7 * 24);
            for (int d = 0; d < 7; d++) for (int h = 0; h < 24; h++) {
                ActivityCellVO vo = new ActivityCellVO(); vo.setDow(d); vo.setHour(h); vo.setCount(grid[d][h]); out.add(vo);
            }
            return out;
        });
    }

    @Override
    public Mono<Long> countToday(Long tenantId) {
        return tsdbStore.count(SeriesFilter.tenantWide(tenantId), windowSince(LocalDate.now(TimeConstant.DEFAULT_ZONEID).atStartOfDay()), DEADLINE);
    }

    @Override
    public Mono<Long> countYesterday(Long tenantId) {
        LocalDate today = LocalDate.now(TimeConstant.DEFAULT_ZONEID);
        return tsdbStore.count(SeriesFilter.tenantWide(tenantId),
                new TimeWindow(converter.toInstant(today.minusDays(1).atStartOfDay()), converter.toInstant(today.atStartOfDay())), DEADLINE);
    }

    @Override
    public Mono<Long> countTotal(Long tenantId) {
        return tsdbStore.count(SeriesFilter.tenantWide(tenantId), new TimeWindow(Instant.EPOCH, Instant.now()), DEADLINE);
    }

    @Override
    public Mono<List<TimeseriesPointVO>> timeseries(Long tenantId, String granularity, int rangeHours) {
        String g = GRANULARITY.contains(granularity) ? granularity : "hour";
        int hours = Math.clamp(rangeHours, 1, MAX_HOURS_90D);
        LocalDateTime from = LocalDateTime.now(TimeConstant.DEFAULT_ZONEID).minusHours(hours);
        return tsdbStore.bucketedCount(tenantId, windowSince(from), "day".equals(g) ? Duration.ofDays(1) : Duration.ofHours(1), DEADLINE)
                .map(buckets -> buckets.stream().map(bucket -> {
                    TimeseriesPointVO vo = new TimeseriesPointVO(); vo.setBucket(converter.toWallClock(bucket.bucketStart())); vo.setCount(bucket.sampleCount()); return vo;
                }).toList());
    }

    @Override
    public Mono<List<TopEntityVO>> top(Long tenantId, String dimension, int rangeHours, int limit) {
        GroupDimension groupDimension = DIMENSIONS.get(dimension);
        if (groupDimension == null) return Mono.error(new IllegalArgumentException("Unsupported dimension: " + dimension));
        LocalDateTime from = LocalDateTime.now(TimeConstant.DEFAULT_ZONEID).minusHours(Math.clamp(rangeHours, 1, MAX_HOURS_90D));
        return tsdbStore.countByDimension(tenantId, windowSince(from), groupDimension, Math.clamp(limit, 1, MAX_LIMIT), DEADLINE)
                .map(rows -> rows.stream().map(row -> { TopEntityVO vo = new TopEntityVO(); vo.setEntityId(String.valueOf(row.entityId())); vo.setCount(row.count()); return vo; }).toList());
    }

    @Override
    public Mono<List<SilentSourceVO>> silentSources(Long tenantId, int baselineDays, int silentMinutes, int limit) {
        LocalDateTime now = LocalDateTime.now(TimeConstant.DEFAULT_ZONEID);
        LocalDateTime from = now.minusDays(Math.clamp(baselineDays, 1, MAX_BASELINE_DAYS));
        Instant threshold = converter.toInstant(now.minusMinutes(Math.clamp(silentMinutes, 5, 60 * 24)));
        return tsdbStore.lastSeenPerSeries(tenantId, windowSince(from), DEADLINE).map(seen -> seen.stream()
                .filter(row -> row.lastSeen() != null && row.lastSeen().isBefore(threshold))
                .sorted(Comparator.comparing(SeriesLastSeen::lastSeen).reversed()).limit(Math.clamp(limit, 1, MAX_COVERAGE_GAP_LIMIT))
                .map(row -> { SilentSourceVO vo = new SilentSourceVO(); vo.setDeviceId(String.valueOf(row.series().deviceId())); vo.setPointId(String.valueOf(row.series().pointId())); vo.setLastSeen(converter.toWallClock(row.lastSeen())); vo.setSilentSeconds(Duration.between(vo.getLastSeen(), now).getSeconds()); return vo; }).toList());
    }

    @Override
    public Mono<CoverageGapVO> coverageGap(Long tenantId, int limit) {
        return listAllPoints(tenantId)
                .zipWith(tsdbStore.lastSeenPerSeries(tenantId, new TimeWindow(Instant.EPOCH, Instant.now()), DEADLINE))
                .map(tuple -> {
                    List<FacadePointBO> points = tuple.getT1();
                    Set<Long> reported = tuple.getT2().stream().map(row -> row.series().pointId()).collect(java.util.stream.Collectors.toSet());
                    CoverageGapVO vo = new CoverageGapVO(); vo.setTotalPoints(points.size());
                    points.stream().filter(point -> !reported.contains(point.getId()))
                            .limit(Math.clamp(limit, 1, MAX_COVERAGE_GAP_LIMIT)).forEach(point -> {
                                CoverageGapVO.Item item = new CoverageGapVO.Item();
                                item.setPointId(String.valueOf(point.getId())); item.setProfileId(String.valueOf(point.getProfileId())); vo.addItem(item);
                            });
                    vo.setMissingPoints(Math.max(0, points.size() - reported.size())); return vo;
                });
    }

    private Mono<List<FacadePointBO>> listAllPoints(Long tenantId) {
        return listAllPoints(tenantId, 0, new ArrayList<>());
    }

    private Mono<List<FacadePointBO>> listAllPoints(Long tenantId, long offset, List<FacadePointBO> collected) {
        return pointFacade.listReactive(new io.github.pnoker.common.facade.entity.query.FacadePointOffsetQuery(tenantId, offset, 200))
                .flatMap(page -> {
                    collected.addAll(page.items());
                    return page.hasNext() ? listAllPoints(tenantId, offset + page.items().size(), collected) : Mono.just(List.copyOf(collected));
                });
    }

    @Override
    public Mono<OffsetPage<AlertItemVO>> alertPage(Long tenantId, String source, Integer alarmTypeFlag,
                                                   Integer confirmFlag, LocalDateTime from, PageRequest page) {
        String src = normalizeSource(source);
        return alertStore.list(tenantId, src, alarmTypeFlag, confirmFlag, from, page)
                .map(result -> OffsetPage.of(result.items().stream().map(this::toAlertVO).toList(),
                        result.offset(), result.limit(), result.total()));
    }

    @Override
    public Mono<Boolean> confirmAlert(Long tenantId, String source, Long id) {
        return alertStore.updateConfirm(tenantId, normalizeSourceRequired(source), id, (byte) 1);
    }

    @Override
    public Mono<Boolean> unconfirmAlert(Long tenantId, String source, Long id) {
        return alertStore.updateConfirm(tenantId, normalizeSourceRequired(source), id, (byte) 0);
    }

    @Override
    public Mono<Integer> bulkConfirmAlert(Long tenantId, List<AlertBulkConfirmVO.Item> items, boolean confirm) {
        if (items == null || items.isEmpty()) return Mono.just(0);
        byte flag = (byte) (confirm ? 1 : 0);
        return Flux.fromIterable(items)
                .filter(Objects::nonNull)
                .concatMap(item -> parseAlertId(item)
                        .flatMap(id -> {
                            String source;
                            try {
                                source = normalizeSourceRequired(item.getSource());
                            } catch (IllegalArgumentException exception) {
                                return Mono.just(false);
                            }
                            return alertStore.updateConfirm(tenantId, source, id, flag);
                        }))
                .filter(Boolean::booleanValue)
                .count()
                .map(Long::intValue);
    }

    private AlertItemVO toAlertVO(AlertItemRow row) {
        AlertItemVO vo = new AlertItemVO();
        vo.setId(String.valueOf(row.getId()));
        vo.setSource(row.getSource());
        vo.setSourceId(String.valueOf(row.getSourceId()));
        vo.setPointId(String.valueOf(row.getPointId()));
        vo.setAlarmTypeFlag(AlarmTypeEnum.ofIndex((byte) row.getAlarmTypeFlag()));
        vo.setConfirmFlag(ConfirmFlagEnum.ofIndex((byte) row.getConfirmFlag()));
        vo.setCreateTime(row.getCreateTime());
        vo.setMessage(row.getMessage());
        return vo;
    }

    private String normalizeSource(String source) {
        if (source == null || source.isBlank()) return null;
        if (!ALERT_SOURCES.contains(source)) {
            throw new IllegalArgumentException("alert source is not allowed: " + source);
        }
        return source;
    }

    private String normalizeSourceRequired(String source) {
        String value = normalizeSource(source);
        if (value == null) throw new IllegalArgumentException("alert source is required");
        return value;
    }

    private Mono<Long> parseAlertId(AlertBulkConfirmVO.Item item) {
        if (item.getId() == null || item.getId().isBlank()) return Mono.empty();
        try {
            long id = Long.parseLong(item.getId());
            return id > 0 ? Mono.just(id) : Mono.empty();
        } catch (NumberFormatException exception) {
            return Mono.empty();
        }
    }











    @Override
    public Mono<List<LatestPointValueVO>> latestStream(Long tenantId, int limit) {
        int clamped = Math.clamp(limit, 1, MAX_LIVE_SIZE);
        return pointValueLatestService.listLatestStream(tenantId, clamped).collectList().flatMap(rows -> {
            List<LatestPointValueVO> out = new ArrayList<>(rows.size());
            Set<Long> deviceIds = new HashSet<>();
            Set<Long> pointIds = new HashSet<>();
            Set<Long> driverIds = new HashSet<>();
            for (var row : rows) {
                LatestPointValueVO vo = new LatestPointValueVO();
                vo.setDeviceId(String.valueOf(row.getDeviceId()));
                vo.setPointId(String.valueOf(row.getPointId()));
                vo.setDriverId(String.valueOf(row.getDriverId()));
                vo.setRawValue(row.getRawValue());
                vo.setCalValue(row.getCalValue());
                vo.setValueType(Objects.nonNull(row.getNumValue()) ? "NUMERIC" : "STRING");
                vo.setCreateTime(row.getCreateTime());
                out.add(vo);
                if (row.getDeviceId() != null && row.getDeviceId() > 0) deviceIds.add(row.getDeviceId());
                if (row.getPointId() != null && row.getPointId() > 0) pointIds.add(row.getPointId());
                if (row.getDriverId() != null && row.getDriverId() > 0) driverIds.add(row.getDriverId());
            }
            return Mono.zip(deviceFacade.listByIdsReactive(tenantId, deviceIds).collectList(),
                            pointFacade.listByIdsReactive(tenantId, pointIds).collectList(),
                            driverFacade.listByIdsReactive(tenantId, driverIds).collectList())
                    .map(tuple -> {
                        Map<Long, String> deviceNames = tuple.getT1().stream().collect(java.util.stream.Collectors.toMap(FacadeDeviceBO::getId, FacadeDeviceBO::getDeviceName, (a, b) -> a));
                        Map<Long, String> pointNames = tuple.getT2().stream().collect(java.util.stream.Collectors.toMap(FacadePointBO::getId, FacadePointBO::getPointName, (a, b) -> a));
                        Map<Long, String> driverNames = tuple.getT3().stream().collect(java.util.stream.Collectors.toMap(FacadeDriverBO::getId, FacadeDriverBO::getDriverName, (a, b) -> a));
                        out.forEach(vo -> {
                            vo.setDeviceName(deviceNames.get(Long.valueOf(vo.getDeviceId())));
                            vo.setPointName(pointNames.get(Long.valueOf(vo.getPointId())));
                            vo.setDriverName(driverNames.get(Long.valueOf(vo.getDriverId())));
                        });
                        return out;
                    });
        });
    }

    @Override
    public Mono<AlertStatsVO> alertStats(Long tenantId) {
        LocalDateTime todayStart = LocalDate.now(TimeConstant.DEFAULT_ZONEID).atStartOfDay();
        LocalDateTime anchor = LocalDateTime.now(TimeConstant.DEFAULT_ZONEID)
                .withMinute(0).withSecond(0).withNano(0).minusHours(23);
        return Mono.zip(
                        alertAnalyticsStore.countAll(tenantId).defaultIfEmpty(new io.github.pnoker.common.data.entity.bo.dashboard.AlertCountersRow()),
                        alertAnalyticsStore.countByType(tenantId).collectList(),
                        alertAnalyticsStore.countBySource(tenantId).collectList(),
                        alertAnalyticsStore.todayBySource(tenantId, todayStart).collectList(),
                        alertAnalyticsStore.hourlyCounts(tenantId, anchor).collectList())
                .map(tuple -> {
                    AlertStatsVO vo = new AlertStatsVO();
                    var totals = tuple.getT1();
                    vo.setTotal(totals.getTotal());
                    vo.setUnconfirmed(totals.getUnconfirmed());
                    vo.setByType(tuple.getT2().stream().map(row -> {
                        AlertStatsVO.BucketVO bucket = new AlertStatsVO.BucketVO();
                        bucket.setKey(asString(row.getBucketKey()));
                        bucket.setCount(row.getCount());
                        return bucket;
                    }).toList());
                    applySourceStats(tuple.getT3(), vo, false);
                    applySourceStats(tuple.getT4(), vo, true);
                    long[] series = new long[24];
                    for (var row : tuple.getT5()) {
                        if (row.getBucket() == null) continue;
                        int index = (int) Duration.between(anchor, row.getBucket()).toHours();
                        if (index >= 0 && index < series.length) series[index] = row.getCount();
                    }
                    vo.setSparkline24h(java.util.Arrays.stream(series).boxed().toList());
                    return vo;
                });
    }

    private void applySourceStats(List<io.github.pnoker.common.data.entity.bo.dashboard.SourceStatsRow> rows,
                                  AlertStatsVO target, boolean today) {
        for (var row : rows) {
            if ("device".equals(row.getSource())) {
                if (today) {
                    target.setTodayDeviceAlarms(row.getTotal());
                    target.setTodayDeviceUnconfirmed(row.getUnconfirmed());
                } else {
                    target.setDeviceAlerts(row.getTotal());
                    target.setDeviceUnconfirmed(row.getUnconfirmed());
                }
            } else if ("driver".equals(row.getSource())) {
                if (today) {
                    target.setTodayDriverAlarms(row.getTotal());
                    target.setTodayDriverUnconfirmed(row.getUnconfirmed());
                } else {
                    target.setDriverAlerts(row.getTotal());
                    target.setDriverUnconfirmed(row.getUnconfirmed());
                }
            }
        }
    }

    @Override
    public Mono<List<AlertItemVO>> alertLatest(Long tenantId, int limit) {
        int clamped = Math.clamp(limit, 1, MAX_LIMIT);
        return alertStore.list(tenantId, null, null, null, null, new PageRequest(0, clamped))
                .map(page -> page.items().stream().map(this::toAlertVO).toList());
    }

    @Override
    public Mono<List<AlertTrendVO>> alertTrend(Long tenantId, int days) {
        int clamped = Math.clamp(days, 1, MAX_DAYS);
        LocalDateTime from = LocalDate.now(TimeConstant.DEFAULT_ZONEID).minusDays(clamped).atTime(LocalTime.MIN);
        return alertAnalyticsStore.dailyTrend(tenantId, from).collectList().map(rows -> {
            Map<String, io.github.pnoker.common.data.entity.bo.dashboard.AlertTrendRow> byDate = rows.stream()
                    .collect(java.util.stream.Collectors.toMap(io.github.pnoker.common.data.entity.bo.dashboard.AlertTrendRow::getDate,
                            row -> row, (left, right) -> left));
            List<AlertTrendVO> out = new ArrayList<>(clamped + 1);
            for (int index = 0; index <= clamped; index++) {
                String date = LocalDate.now(TimeConstant.DEFAULT_ZONEID).minusDays(clamped - index).toString();
                var row = byDate.get(date);
                AlertTrendVO vo = new AlertTrendVO();
                vo.setDate(date);
                vo.setDeviceCount(row == null ? 0 : row.getDeviceCount());
                vo.setDriverCount(row == null ? 0 : row.getDriverCount());
                out.add(vo);
            }
            return out;
        });
    }

    @Override
    public Mono<List<AlertTopSourceVO>> alertTopSources(Long tenantId, int days, int limit) {
        int clampedDays = Math.clamp(days, 1, MAX_DAYS);
        int clampedLimit = Math.clamp(limit, 1, MAX_LIMIT);
        LocalDateTime from = LocalDate.now(TimeConstant.DEFAULT_ZONEID).minusDays(clampedDays).atTime(LocalTime.MIN);
        return alertAnalyticsStore.topSources(tenantId, from, clampedLimit).map(row -> {
            AlertTopSourceVO vo = new AlertTopSourceVO();
            vo.setSource(row.getSource());
            vo.setSourceId(String.valueOf(row.getSourceId()));
            vo.setCount(row.getCount());
            return vo;
        }).collectList();
    }

    @Override
    public Mono<List<AlertActivityCellVO>> alertActivity(Long tenantId, int days) {
        int clampedDays = Math.clamp(days, 1, MAX_DAYS);
        LocalDateTime from = LocalDate.now(TimeConstant.DEFAULT_ZONEID).minusDays(clampedDays).atTime(LocalTime.MIN);
        return alertAnalyticsStore.activityHeatmap(tenantId, from).collectList().map(rows -> {
            long[][] grid = new long[7][24];
            for (var row : rows) if (row.getDow() >= 0 && row.getDow() < 7 && row.getHour() >= 0 && row.getHour() < 24)
                grid[row.getDow()][row.getHour()] = row.getCount();
            List<AlertActivityCellVO> out = new ArrayList<>(7 * 24);
            for (int d = 0; d < 7; d++) for (int h = 0; h < 24; h++) {
                AlertActivityCellVO vo = new AlertActivityCellVO(); vo.setDow(d); vo.setHour(h); vo.setCount(grid[d][h]); out.add(vo);
            }
            return out;
        });
    }

    @Override
    public Mono<List<AlertTypeBucketVO>> alertTypeDistribution(Long tenantId, int days) {
        int clampedDays = Math.clamp(days, 1, MAX_DAYS);
        LocalDateTime from = LocalDate.now(TimeConstant.DEFAULT_ZONEID).minusDays(clampedDays).atTime(LocalTime.MIN);
        return alertAnalyticsStore.typeDistribution(tenantId, from).map(row -> {
            AlertTypeBucketVO vo = new AlertTypeBucketVO();
            vo.setType(Objects.isNull(row.getBucketKey()) ? null : row.getBucketKey().toString());
            vo.setCount(row.getCount());
            return vo;
        }).collectList();
    }

    @Override
    public Mono<List<AlertTopSourceVO>> alertStormSources(Long tenantId, int hours, int minCount, int limit) {
        int clampedHours = Math.clamp(hours, 1, MAX_HOURS_30D);
        int clampedMin = Math.max(1, minCount);
        int clampedLimit = Math.clamp(limit, 1, MAX_LIMIT);
        LocalDateTime from = LocalDateTime.now(TimeConstant.DEFAULT_ZONEID).minusHours(clampedHours);
        return alertAnalyticsStore.stormSources(tenantId, from, clampedMin, clampedLimit).map(row -> {
            AlertTopSourceVO vo = new AlertTopSourceVO(); vo.setSource(row.getSource()); vo.setSourceId(String.valueOf(row.getSourceId())); vo.setCount(row.getCount()); return vo;
        }).collectList();
    }

    // ================================================================
    // Phase-2 insights
    // ================================================================

    @Override
    public Mono<List<FlappingSourceVO>> alertFlapping(Long tenantId, int hours, int minCount, int limit) {
        int h = Math.clamp(hours, 1, MAX_HOURS_7D);
        int min = Math.max(MIN_FLAPPING_COUNT, minCount);
        int lim = Math.clamp(limit, 1, MAX_LIMIT);
        LocalDateTime from = LocalDateTime.now(TimeConstant.DEFAULT_ZONEID).minusHours(h);
        return alertAnalyticsStore.flappingSources(tenantId, from, min, lim).map(r -> {
            FlappingSourceVO vo = new FlappingSourceVO();
            vo.setSource(r.getSource());
            vo.setSourceId(String.valueOf(r.getSourceId()));
            vo.setAlarmTypeFlag(r.getAlarmTypeFlag());
            vo.setCount(r.getCount());
            return vo;
        }).collectList();
    }

    @Override
    public Mono<List<CorrelationPairVO>> alertCorrelation(Long tenantId, int hours, int windowSec, int limit) {
        int h = Math.clamp(hours, 1, MAX_HOURS_7D);
        int w = Math.clamp(windowSec, MIN_CORRELATION_WINDOW_SEC, MAX_CORRELATION_WINDOW_SEC);
        int lim = Math.clamp(limit, 1, MAX_CORRELATION_PAIRS);
        LocalDateTime from = LocalDateTime.now(TimeConstant.DEFAULT_ZONEID).minusHours(h);
        return alertAnalyticsStore.correlationPairs(tenantId, from, w, lim).map(r -> {
            CorrelationPairVO vo = new CorrelationPairVO();
            vo.setASource(r.getASource());
            vo.setASourceId(String.valueOf(r.getASourceId()));
            vo.setAEventType(r.getAEventType());
            vo.setBSource(r.getBSource());
            vo.setBSourceId(String.valueOf(r.getBSourceId()));
            vo.setBEventType(r.getBEventType());
            vo.setCoCount(r.getCoCount());
            return vo;
        }).collectList();
    }

    @Override
    public Mono<List<PeerDeviationVO>> alertPeerDeviation(Long tenantId, int days) {
        int d = Math.clamp(days, 1, MAX_PEER_DAYS);
        LocalDateTime from = LocalDate.now(TimeConstant.DEFAULT_ZONEID).minusDays(d).atTime(LocalTime.MIN);
        return alertAnalyticsStore.peerAlarmCounts(tenantId, from).collectList().map(rows -> {

        // Group by profile → list of (device, alarmCount); then pick median
        // and flag devices with count >= 3x median (and a floor of 5 alarms
        // so a profile with median=1 doesn't emit noise).
        Map<Long, List<long[]>> byProfile = new HashMap<>();
        for (var r : rows) {
            long prof = r.getProfileId();
            long dev = r.getDeviceId();
            long cnt = r.getAlarmCount();
            byProfile.computeIfAbsent(prof, k -> new ArrayList<>()).add(new long[]{dev, cnt});
        }
        List<PeerDeviationVO> out = new ArrayList<>();
        for (Map.Entry<Long, List<long[]>> e : byProfile.entrySet()) {
            List<long[]> devs = e.getValue();
            if (devs.size() < 3)
                continue; // need enough peers for a peer test
            long[] sorted = devs.stream().mapToLong(a -> a[1]).sorted().toArray();
            long median = sorted[sorted.length / 2];
            for (long[] a : devs) {
                if (a[1] < 5)
                    continue;
                if (median > 0 && a[1] < median * 3)
                    continue;
                if (median == 0 && a[1] < 5)
                    continue;
                PeerDeviationVO vo = new PeerDeviationVO();
                vo.setProfileId(String.valueOf(e.getKey()));
                vo.setDeviceId(String.valueOf(a[0]));
                vo.setAlarmCount(a[1]);
                vo.setPeerMedian(median);
                vo.setRatio(median == 0 ? 0.0 : Math.round((double) a[1] / median * 100.0) / 100.0);
                out.add(vo);
            }
        }
        out.sort((a, b) -> Long.compare(b.getAlarmCount(), a.getAlarmCount()));
        // Cap to 50 to keep payload bounded
        return out.size() > 50 ? out.subList(0, 50) : out;
        });
    }

    @Override
    public Mono<AgingBacklogVO> alertAgingBacklog(Long tenantId) {
        return alertAnalyticsStore.agingBuckets(tenantId).map(row -> {
            AgingBacklogVO vo = new AgingBacklogVO();
            vo.setUnder1h(row.getUnder1h());
            vo.setH1to6(row.getH1to6());
            vo.setH6to24(row.getH6to24());
            vo.setOver24h(row.getOver24h());
            vo.setTotal(row.getTotal());
            return vo;
        });
    }

    @Override
    public Mono<List<MttaTrendVO>> alertMtta(Long tenantId, int days) {
        int d = Math.clamp(days, 1, MAX_DAYS);
        LocalDateTime from = LocalDate.now(TimeConstant.DEFAULT_ZONEID).minusDays(d).atTime(LocalTime.MIN);
        return alertAnalyticsStore.mttaByDay(tenantId, from).map(r -> {
            MttaTrendVO vo = new MttaTrendVO();
            vo.setDate(r.getDate());
            vo.setP50Ms(r.getP50Ms());
            vo.setP95Ms(r.getP95Ms());
            vo.setConfirmedCount(r.getConfirmedCount());
            return vo;
        }).collectList();
    }

    @Override
    public Mono<List<ProtocolHealthVO>> protocolHealth(Long tenantId) {
        return alertAnalyticsStore.protocolHealth(tenantId).map(r -> {
            ProtocolHealthVO vo = new ProtocolHealthVO();
            vo.setServiceName(r.getServiceName());
            vo.setDriverCount(r.getDriverCount());
            vo.setEnabledCount(r.getEnabledCount());
            vo.setDeviceCount(r.getDeviceCount());
            return vo;
        }).collectList();
    }

    @Override
    public Mono<List<ChangeImpactVO>> changeImpact(Long tenantId, int days, int limit) {
        int d = Math.clamp(days, 1, MAX_DAYS);
        int lim = Math.clamp(limit, 1, MAX_LIMIT);
        LocalDateTime from = LocalDate.now(TimeConstant.DEFAULT_ZONEID).minusDays(d).atTime(LocalTime.MIN);
        return alertAnalyticsStore.recentChanges(tenantId, from, lim).map(r -> {
            ChangeImpactVO vo = new ChangeImpactVO();
            vo.setKind(r.getKind());
            vo.setEntityId(String.valueOf(r.getEntityId()));
            vo.setOperateTime(r.getOperateTime());
            return vo;
        }).collectList();
    }




}
