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

import io.github.pnoker.common.data.biz.PointValueDashboardService;
import io.github.pnoker.common.data.entity.vo.dashboard.PointValueDashboardVO;
import io.github.pnoker.common.data.repository.ReactiveTsdbStore;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.tsdb.model.TsdbModel.AggregateFunction;
import io.github.pnoker.common.tsdb.model.TsdbModel.BucketAggregate;
import io.github.pnoker.common.tsdb.model.TsdbModel.Cursor;
import io.github.pnoker.common.tsdb.model.TsdbModel.PointValueSample;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesFilter;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesKey;
import io.github.pnoker.common.tsdb.model.TsdbModel.TimeWindow;
import io.github.pnoker.common.tsdb.model.TsdbModel.TsdbDeadline;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * TSDB-backed implementation of {@link PointValueDashboardService}. Trend band,
 * hourly volume and typical-day curve come from store-side bucket aggregates
 * (S7); the value histogram, interval health and gaps come from a bounded raw
 * walk (S5) so no platform-wide scan is involved.
 *
 * @author pnoker
 * @since 2026.9.22
 */
@Slf4j
@Service
public class PointValueDashboardServiceImpl implements PointValueDashboardService {

    /**
     * Read deadline shared with the value-query paths.
     */
    private static final TsdbDeadline DEADLINE = TsdbDeadline.ofSeconds(30);

    /**
     * Max lookback window in hours.
     */
    private static final int MAX_RANGE_HOURS = 168;

    /**
     * Typical-day lookback in days.
     */
    private static final int TYPICAL_DAY_DAYS = 7;

    /**
     * Raw page size for the sample walk.
     */
    private static final int PAGE_SIZE = 500;

    /**
     * Interval histogram edges in milliseconds; the final bin is open-ended.
     */
    private static final long[] INTERVAL_EDGES_MS = {0, 500, 1_000, 2_000, 5_000, 10_000, 30_000, 60_000, 300_000, 1_800_000};

    /**
     * Gaps are reported when a sampling interval exceeds max(5x median, this floor).
     */
    private static final long GAP_FLOOR_MS = 60_000;

    /**
     * Upper bound of the raw walk; the walk stops early and reports truncated.
     */
    private final int rawCap;

    private final DeviceFacade deviceFacade;

    private final PointFacade pointFacade;

    private final ReactiveTsdbStore reactiveTsdbStore;

    public PointValueDashboardServiceImpl(
            @Value("${dc3.data.dashboard.raw-cap:8000}") int rawCap,
            DeviceFacade deviceFacade,
            PointFacade pointFacade,
            ReactiveTsdbStore reactiveTsdbStore) {
        this.rawCap = rawCap;
        this.deviceFacade = deviceFacade;
        this.pointFacade = pointFacade;
        this.reactiveTsdbStore = reactiveTsdbStore;
    }

    @Override
    public Mono<PointValueDashboardVO> dashboard(Long tenantId, Long deviceId, Long pointId, Integer rangeHours) {
        if (!isValidId(tenantId) || !isValidId(deviceId) || !isValidId(pointId)) {
            return Mono.error(new IllegalArgumentException("tenantId, deviceId and pointId must be positive"));
        }
        int range = rangeHours == null ? 24 : rangeHours;
        if (range < 1 || range > MAX_RANGE_HOURS) {
            return Mono.error(new IllegalArgumentException("rangeHours must be between 1 and " + MAX_RANGE_HOURS));
        }
        Instant now = Instant.now();
        SeriesFilter filter = SeriesFilter.of(new SeriesKey(tenantId, deviceId, pointId));
        TimeWindow window = new TimeWindow(now.minus(Duration.ofHours(range)), now);
        Duration bucket = bucketWidth(range);
        Duration hour = Duration.ofHours(1);

        Mono<Map<SeriesKey, List<BucketAggregate>>> avgBuckets =
                safeBucketed(filter, AggregateFunction.AVG, window, bucket);
        Mono<Map<SeriesKey, List<BucketAggregate>>> minBuckets =
                safeBucketed(filter, AggregateFunction.MIN, window, bucket);
        Mono<Map<SeriesKey, List<BucketAggregate>>> maxBuckets =
                safeBucketed(filter, AggregateFunction.MAX, window, bucket);
        Mono<Map<SeriesKey, List<BucketAggregate>>> hourlyCounts =
                safeBucketed(filter, AggregateFunction.COUNT, window, hour);
        Mono<Map<SeriesKey, List<BucketAggregate>>> typicalDayBuckets = safeBucketed(
                filter, AggregateFunction.AVG, new TimeWindow(now.minus(Duration.ofDays(TYPICAL_DAY_DAYS)), now), hour);
        Mono<RawProfile> rawProfile = walkRaw(filter, window);

        return validateMetadataScope(tenantId, deviceId, pointId)
                .then(Mono.zip(avgBuckets, minBuckets, maxBuckets, hourlyCounts, typicalDayBuckets, rawProfile))
                .map(tuple -> build(
                        tuple.getT1(),
                        tuple.getT2(),
                        tuple.getT3(),
                        tuple.getT4(),
                        tuple.getT5(),
                        tuple.getT6()));
    }

    /**
     * Merge the single-series bucket maps into the dashboard payload. Bucket
     * alignment is guaranteed by the shared window and bucket width; the merge
     * still keys on the bucket start to stay adapter-agnostic.
     */
    private PointValueDashboardVO build(
            Map<SeriesKey, List<BucketAggregate>> avgBuckets,
            Map<SeriesKey, List<BucketAggregate>> minBuckets,
            Map<SeriesKey, List<BucketAggregate>> maxBuckets,
            Map<SeriesKey, List<BucketAggregate>> hourlyCounts,
            Map<SeriesKey, List<BucketAggregate>> typicalDayBuckets,
            RawProfile rawProfile) {
        PointValueDashboardVO vo = new PointValueDashboardVO();

        List<BucketAggregate> avg = onlySeries(avgBuckets);
        List<BucketAggregate> min = onlySeries(minBuckets);
        List<BucketAggregate> max = onlySeries(maxBuckets);
        Map<Instant, PointValueDashboardVO.TrendBucket> trend = new LinkedHashMap<>();
        for (BucketAggregate bucket : avg) {
            trend.computeIfAbsent(bucket.bucketStart(), key -> new PointValueDashboardVO.TrendBucket())
                    .setFrom(bucket.bucketStart());
            trend.get(bucket.bucketStart()).setAvg(bucket.value());
            trend.get(bucket.bucketStart()).setSampleCount(bucket.sampleCount());
        }
        for (BucketAggregate bucket : min) {
            trend.computeIfAbsent(bucket.bucketStart(), key -> new PointValueDashboardVO.TrendBucket())
                    .setFrom(bucket.bucketStart());
            trend.get(bucket.bucketStart()).setMin(bucket.value());
        }
        for (BucketAggregate bucket : max) {
            trend.computeIfAbsent(bucket.bucketStart(), key -> new PointValueDashboardVO.TrendBucket())
                    .setFrom(bucket.bucketStart());
            trend.get(bucket.bucketStart()).setMax(bucket.value());
        }
        vo.setTrend(List.copyOf(trend.values()));

        for (BucketAggregate bucket : onlySeries(hourlyCounts)) {
            PointValueDashboardVO.HourVolume row = new PointValueDashboardVO.HourVolume();
            row.setHourStart(bucket.bucketStart());
            row.setCount(bucket.sampleCount());
            vo.getHourlyVolume().add(row);
        }

        // Typical day: average the 1h buckets per hour-of-day across the days.
        Map<Integer, List<Double>> byHour = new LinkedHashMap<>();
        for (BucketAggregate bucket : onlySeries(typicalDayBuckets)) {
            if (bucket.value() == null) continue;
            byHour.computeIfAbsent(
                            bucket.bucketStart().atZone(ZoneId.systemDefault()).getHour(), key -> new ArrayList<>())
                    .add(bucket.value());
        }
        byHour.forEach((hour, values) -> {
            PointValueDashboardVO.HourAverage row = new PointValueDashboardVO.HourAverage();
            row.setHourOfDay(hour);
            row.setAvg(values.stream().mapToDouble(Double::doubleValue).average().orElse(0d));
            vo.getTypicalDay().add(row);
        });

        vo.setValueHistogram(rawProfile.valueBins());
        vo.setIntervalHistogram(rawProfile.intervalBins());
        vo.setGaps(rawProfile.gaps());
        vo.setStats(rawProfile.stats());
        return vo;
    }

    /**
     * Extract the aggregate list of a single-series filter result; empty when
     * the series has no samples in the window.
     */
    private List<BucketAggregate> onlySeries(Map<SeriesKey, List<BucketAggregate>> buckets) {
        return buckets.values().stream().findFirst().orElseGet(List::of);
    }

    /**
     * Walk raw samples inside the window, newest first, bounded by the raw cap.
     */
    private Mono<RawProfile> walkRaw(SeriesFilter filter, TimeWindow window) {
        return walk(filter, window, null, new RawProfile(rawCap));
    }

    private Mono<RawProfile> walk(SeriesFilter filter, TimeWindow window, Cursor cursor, RawProfile profile) {
        return Mono.defer(() -> reactiveTsdbStore.history(filter, window, cursor, PAGE_SIZE, DEADLINE))
                .flatMap(page -> {
                    for (PointValueSample sample : page.items()) {
                        profile.add(sample);
                    }
                    if (page.nextCursor() == null) {
                        profile.markComplete();
                        return Mono.just(profile);
                    }
                    if (profile.samples().size() >= rawCap) {
                        return Mono.just(profile);
                    }
                    return walk(filter, window, page.nextCursor(), profile);
                })
                .onErrorResume(error -> {
                    log.warn("Raw sample walk aborted: {}", error.getMessage());
                    return Mono.just(profile);
                });
    }

    /**
     * Tenant + device + point scope validation, mirroring the value-query paths.
     */
    private Mono<Void> validateMetadataScope(Long tenantId, Long deviceId, Long pointId) {
        return safeGet(deviceFacade.getByIdReactive(tenantId, deviceId))
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .zipWith(safeGet(pointFacade.getByIdReactive(tenantId, pointId))
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty()))
                .flatMap(tuple -> {
                    var device = tuple.getT1();
                    var point = tuple.getT2();
                    if (device.isEmpty() || point.isEmpty()) {
                        return Mono.error(new NotFoundException("Device or point does not exist"));
                    }
                    return Objects.equals(device.get().getProfileId(), point.get().getProfileId())
                            ? Mono.empty()
                            : Mono.error(new NotFoundException("Point does not exist"));
                });
    }

    private <T> Mono<T> safeGet(Mono<T> source) {
        return source.onErrorResume(error -> Mono.error(error));
    }

    private Mono<Map<SeriesKey, List<BucketAggregate>>> safeBucketed(
            SeriesFilter filter, AggregateFunction fn, TimeWindow window, Duration bucket) {
        // defer keeps the store call off the assembly path: an invalid scope
        // must fail validation without touching the TSDB, and a store failure
        // degrades to an empty series instead of failing the dashboard.
        return Mono.defer(() -> reactiveTsdbStore.bucketedAggregate(filter, fn, window, bucket, null, DEADLINE))
                .onErrorResume(error -> {
                    log.warn("bucketedAggregate {} aborted: {}", fn, error.getMessage());
                    return Mono.just(Map.of());
                });
    }

    /**
     * Bucket width keeps the bucket count bounded for every supported window so
     * charts stay legible: 1m for short windows up to 2h for the 7d window.
     */
    private Duration bucketWidth(int rangeHours) {
        if (rangeHours <= 6) return Duration.ofMinutes(1);
        if (rangeHours <= 24) return Duration.ofMinutes(15);
        if (rangeHours <= 72) return Duration.ofHours(1);
        return Duration.ofHours(2);
    }

    private boolean isValidId(Long id) {
        return Objects.nonNull(id) && id > 0;
    }

    /**
     * Mutable accumulator for the raw walk: numeric histogram, interval
     * distribution, gap detection and window stats.
     */
    private static final class RawProfile {

        private final int cap;

        private boolean complete;

        private final List<PointValueSample> samples = new ArrayList<>();
        private final List<Double> numericValues = new ArrayList<>();
        private final List<Long> intervalsMs = new ArrayList<>();

        RawProfile(int cap) {
            this.cap = cap;
        }

        void markComplete() {
            this.complete = true;
        }

        void add(PointValueSample sample) {
            if (samples.isEmpty()) {
                samples.add(sample);
                remember(sample);
                return;
            }
            PointValueSample previous = samples.get(samples.size() - 1);
            // Walk is newest first: |Δt| between the previous (newer) and the
            // current (older) sample is the forward sampling interval.
            intervalsMs.add(Math.abs(Duration.between(
                            previous.deviceTime(), sample.deviceTime())
                    .toMillis()));
            samples.add(sample);
            remember(sample);
        }

        private void remember(PointValueSample sample) {
            if (sample.numericValue() != null && Double.isFinite(sample.numericValue())) {
                numericValues.add(sample.numericValue());
            }
        }

        List<PointValueSample> samples() {
            return samples;
        }

        PointValueDashboardVO.Stats stats() {
            PointValueDashboardVO.Stats stats = new PointValueDashboardVO.Stats();
            stats.setSampleCount(samples.size());
            stats.setTruncated(!complete && samples.size() >= cap);
            if (!numericValues.isEmpty()) {
                stats.setMin(Collections.min(numericValues));
                stats.setMax(Collections.max(numericValues));
                stats.setAvg(numericValues.stream().mapToDouble(Double::doubleValue).average().orElse(0d));
            }
            if (!intervalsMs.isEmpty()) {
                List<Long> sorted = new ArrayList<>(intervalsMs);
                sorted.sort(Comparator.naturalOrder());
                stats.setMedianIntervalMs(sorted.get(sorted.size() / 2));
            }
            return stats;
        }

        List<PointValueDashboardVO.ValueBin> valueBins() {
            List<PointValueDashboardVO.ValueBin> bins = new ArrayList<>();
            if (numericValues.size() < 2) return bins;
            double min = Collections.min(numericValues);
            double max = Collections.max(numericValues);
            if (min == max) {
                PointValueDashboardVO.ValueBin bin = new PointValueDashboardVO.ValueBin();
                bin.setFrom(min);
                bin.setTo(max);
                bin.setCount(numericValues.size());
                bins.add(bin);
                return bins;
            }
            int binCount = (int) Math.clamp(Math.round(Math.sqrt(numericValues.size())), 4, 30);
            double width = (max - min) / binCount;
            long[] counts = new long[binCount];
            for (double value : numericValues) {
                int index = (int) Math.min(binCount - 1, (long) ((value - min) / width));
                counts[index]++;
            }
            for (int i = 0; i < binCount; i++) {
                PointValueDashboardVO.ValueBin bin = new PointValueDashboardVO.ValueBin();
                bin.setFrom(min + i * width);
                bin.setTo(i == binCount - 1 ? max : min + (i + 1) * width);
                bin.setCount(counts[i]);
                bins.add(bin);
            }
            return bins;
        }

        List<PointValueDashboardVO.IntervalBin> intervalBins() {
            long[] counts = new long[INTERVAL_EDGES_MS.length];
            for (long interval : intervalsMs) {
                int index = INTERVAL_EDGES_MS.length - 1;
                for (int i = 1; i < INTERVAL_EDGES_MS.length; i++) {
                    if (interval < INTERVAL_EDGES_MS[i]) {
                        index = i - 1;
                        break;
                    }
                }
                counts[index]++;
            }
            List<PointValueDashboardVO.IntervalBin> bins = new ArrayList<>();
            for (int i = 0; i < INTERVAL_EDGES_MS.length; i++) {
                PointValueDashboardVO.IntervalBin bin = new PointValueDashboardVO.IntervalBin();
                bin.setFromMs(INTERVAL_EDGES_MS[i]);
                bin.setToMs(i == INTERVAL_EDGES_MS.length - 1 ? null : INTERVAL_EDGES_MS[i + 1]);
                bin.setCount(counts[i]);
                bins.add(bin);
            }
            return bins;
        }

        List<PointValueDashboardVO.Gap> gaps() {
            if (intervalsMs.size() < 2) return List.of();
            List<Long> sorted = new ArrayList<>(intervalsMs);
            sorted.sort(Comparator.naturalOrder());
            long median = sorted.get(sorted.size() / 2);
            long threshold = Math.max(median * 5, GAP_FLOOR_MS);
            List<PointValueDashboardVO.Gap> gaps = new ArrayList<>();
            for (int i = 1; i < samples.size(); i++) {
                long interval = Math.abs(Duration.between(
                                samples.get(i - 1).deviceTime(), samples.get(i).deviceTime())
                        .toMillis());
                if (interval > threshold) {
                    PointValueDashboardVO.Gap gap = new PointValueDashboardVO.Gap();
                    gap.setFrom(samples.get(i).deviceTime());
                    gap.setTo(samples.get(i - 1).deviceTime());
                    gap.setDurationMs(interval);
                    gaps.add(gap);
                }
            }
            return gaps;
        }
    }
}
