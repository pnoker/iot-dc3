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
package io.github.pnoker.common.data.biz.analytics.impl;

import io.github.pnoker.common.data.biz.analytics.DataAnalyticsService;
import io.github.pnoker.common.data.biz.store.PointValueLatestService;
import io.github.pnoker.common.data.biz.store.PointValueSampleConverter;
import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel;
import io.github.pnoker.common.data.repository.ReactiveTsdbStore;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceOffsetQuery;
import io.github.pnoker.common.facade.entity.query.FacadePointOffsetQuery;
import io.github.pnoker.common.tsdb.model.TsdbModel.AggregateFunction;
import io.github.pnoker.common.tsdb.model.TsdbModel.BucketAggregate;
import io.github.pnoker.common.tsdb.model.TsdbModel.CursorPage;
import io.github.pnoker.common.tsdb.model.TsdbModel.GroupDimension;
import io.github.pnoker.common.tsdb.model.TsdbModel.PointValueSample;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesFilter;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesKey;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesLastSeen;
import io.github.pnoker.common.tsdb.model.TsdbModel.TimeWindow;
import io.github.pnoker.common.tsdb.model.TsdbModel.TsdbDeadline;
import io.github.pnoker.common.tsdb.model.TsdbModel.WindowAggregate;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataAnalyticsServiceImpl implements DataAnalyticsService {

    private static final TsdbDeadline DEADLINE = TsdbDeadline.ofSeconds(30);
    private static final int MAX_SERIES_PER_CALL = 20;
    private static final int MAX_SAMPLES_PER_PULL = 5000;
    private static final int MAX_WINDOW_HOURS = 24 * 90;
    private static final int HISTORY_PAGE = 5000;
    private static final int MAX_THRESHOLD_INTERVALS = 20;

    private final ReactiveTsdbStore tsdbStore;
    private final PointValueLatestService pointValueLatestService;
    private final PointValueSampleConverter converter;
    private final DeviceFacade deviceFacade;
    private final PointFacade pointFacade;
    private final DriverFacade driverFacade;

    private static void requireTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new ServiceException("Analytics requires an authenticated tenant context");
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.clamp(value, min, max);
    }

    private static long clamp(Long value, long min, long max) {
        return value == null ? min : Math.clamp(value, min, max);
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private static Double round(Double value) {
        return value == null ? null : round(value.doubleValue());
    }

    private static Duration windowLength(TimeWindow window) {
        return Duration.between(window.from(), window.toExclusive());
    }

    private static double percentile(List<Double> sorted, double p) {
        if (sorted.isEmpty()) return Double.NaN;
        double position = p * (sorted.size() - 1);
        int lower = (int) Math.floor(position);
        int upper = Math.min(lower + 1, sorted.size() - 1);
        return round(sorted.get(lower) + (sorted.get(upper) - sorted.get(lower)) * (position - lower));
    }

    private static double leastSquaresSlope(List<Double> values) {
        int n = values.size();
        double meanX = (n - 1) / 2.0;
        double meanY =
                values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double numerator = 0;
        double denominator = 0;
        for (int i = 0; i < n; i++) {
            numerator += (i - meanX) * (values.get(i) - meanY);
            denominator += (i - meanX) * (i - meanX);
        }
        return denominator == 0 ? 0 : numerator / denominator;
    }

    private static double pearson(List<double[]> pairs) {
        if (pairs.size() < 2) return 0;
        double meanX = pairs.stream().mapToDouble(pair -> pair[0]).average().orElse(0);
        double meanY = pairs.stream().mapToDouble(pair -> pair[1]).average().orElse(0);
        double numerator = 0;
        double dx = 0;
        double dy = 0;
        for (double[] pair : pairs) {
            numerator += (pair[0] - meanX) * (pair[1] - meanY);
            dx += (pair[0] - meanX) * (pair[0] - meanX);
            dy += (pair[1] - meanY) * (pair[1] - meanY);
        }
        return dx == 0 || dy == 0 ? 0 : numerator / Math.sqrt(dx * dy);
    }

    private static GroupDimension dimensionOf(String dimension) {
        if (dimension == null) return GroupDimension.DEVICE;
        try {
            return GroupDimension.valueOf(dimension.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ServiceException("Unsupported dimension: " + dimension + " (expect DEVICE / POINT / DRIVER)");
        }
    }

    @Override
    public Mono<AnalyticsModel.LatestValuesResponse> queryLatest(
            Long tenantId, AnalyticsModel.QueryLatestRequest request) {
        return Mono.defer(() -> {
            requireTenant(tenantId);
            return resolveSeries(tenantId, request == null ? null : request.series())
                    .flatMap(series -> refs(tenantId, series).flatMap(refs -> {
                        Map<Long, List<Long>> byDevice = series.stream()
                                .collect(Collectors.groupingBy(
                                        SeriesKey::deviceId,
                                        LinkedHashMap::new,
                                        Collectors.mapping(SeriesKey::pointId, Collectors.toList())));
                        return Flux.fromIterable(byDevice.entrySet())
                                .concatMap(entry ->
                                        pointValueLatestService.listLatest(tenantId, entry.getKey(), entry.getValue()))
                                .collectMap(value -> new SeriesKey(tenantId, value.getDeviceId(), value.getPointId()))
                                .map(latest -> latestResponse(series, refs, latest));
                    }));
        });
    }

    private AnalyticsModel.LatestValuesResponse latestResponse(
            List<SeriesKey> series,
            Map<SeriesKey, AnalyticsModel.SeriesRef> refs,
            Map<SeriesKey, PointValueBO> latest) {
        List<AnalyticsModel.LatestItem> items = series.stream()
                .map(key -> {
                    PointValueBO value = latest.get(key);
                    AnalyticsModel.SeriesRef ref = refs.get(key);
                    return value == null
                            ? new AnalyticsModel.LatestItem(ref, null, null, null, null, null, false)
                            : new AnalyticsModel.LatestItem(
                                    ref,
                                    value.getRawValue(),
                                    value.getCalValue(),
                                    value.getNumValue(),
                                    value.getNumValue() == null ? "STRING" : "NUMERIC",
                                    Objects.toString(value.getCreateTime(), null),
                                    true);
                })
                .toList();
        String conclusion = items.stream()
                .filter(AnalyticsModel.LatestItem::hasValue)
                .map(item -> "%s = %s (at %s)"
                        .formatted(
                                item.series().label(),
                                Objects.requireNonNullElse(item.calValue(), item.rawValue()),
                                item.createTime()))
                .collect(Collectors.joining("; ", "Latest values: ", ""));
        return new AnalyticsModel.LatestValuesResponse(conclusion, latest.size(), null, items);
    }

    @Override
    public Mono<AnalyticsModel.HistoryResponse> queryHistory(
            Long tenantId, AnalyticsModel.QueryHistoryRequest request) {
        return Mono.defer(() -> {
            requireTenant(tenantId);
            TimeWindow window = resolveWindow(request == null ? null : request.window());
            int maxPoints = clamp(request == null || request.maxPoints() == null ? 200 : request.maxPoints(), 1, 1000);
            return resolveSeries(tenantId, request == null ? null : request.series())
                    .flatMap(series -> refs(tenantId, series).flatMap(refs -> {
                        if (request != null && "M4".equalsIgnoreCase(request.mode())) {
                            Duration bucket = windowLength(window).dividedBy(maxPoints);
                            SeriesFilter filter = SeriesFilter.of(series);
                            return Mono.zip(
                                            tsdbStore.bucketedAggregate(
                                                    filter, AggregateFunction.MIN, window, bucket, null, DEADLINE),
                                            tsdbStore.bucketedAggregate(
                                                    filter, AggregateFunction.MAX, window, bucket, null, DEADLINE),
                                            tsdbStore.bucketedAggregate(
                                                    filter, AggregateFunction.FIRST, window, bucket, null, DEADLINE),
                                            tsdbStore.bucketedAggregate(
                                                    filter, AggregateFunction.LAST, window, bucket, null, DEADLINE))
                                    .map(tuple -> historyM4(
                                            series, refs, tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4()));
                        }
                        int limit = Math.min(maxPoints * series.size(), MAX_SAMPLES_PER_PULL);
                        return tsdbStore
                                .history(SeriesFilter.of(series), window, null, limit, DEADLINE)
                                .map(page -> historyRaw(refs, page));
                    }));
        });
    }

    private AnalyticsModel.HistoryResponse historyM4(
            List<SeriesKey> series,
            Map<SeriesKey, AnalyticsModel.SeriesRef> refs,
            Map<SeriesKey, List<BucketAggregate>> mins,
            Map<SeriesKey, List<BucketAggregate>> maxs,
            Map<SeriesKey, List<BucketAggregate>> firsts,
            Map<SeriesKey, List<BucketAggregate>> lasts) {
        Map<String, List<AnalyticsModel.HistoryPoint>> out = new LinkedHashMap<>();
        long count = 0;
        for (SeriesKey key : series) {
            List<BucketAggregate> min = mins.getOrDefault(key, List.of());
            List<AnalyticsModel.HistoryPoint> points = new ArrayList<>();
            for (int i = 0; i < min.size(); i++) {
                points.add(new AnalyticsModel.HistoryPoint(
                        min.get(i).bucketStart().toString(),
                        null,
                        value(firsts, key, i),
                        min.get(i).value(),
                        value(maxs, key, i),
                        value(lasts, key, i)));
            }
            out.put(refs.get(key).label(), points);
            count += points.size();
        }
        return new AnalyticsModel.HistoryResponse(
                "history: "
                        + out.entrySet().stream()
                                .map(entry -> "%s (%d points)"
                                        .formatted(
                                                entry.getKey(), entry.getValue().size()))
                                .collect(Collectors.joining(", ")),
                count,
                null,
                out);
    }

    private static Double value(Map<SeriesKey, List<BucketAggregate>> buckets, SeriesKey key, int index) {
        List<BucketAggregate> values = buckets.get(key);
        return values == null || index >= values.size()
                ? null
                : values.get(index).value();
    }

    private AnalyticsModel.HistoryResponse historyRaw(
            Map<SeriesKey, AnalyticsModel.SeriesRef> refs, CursorPage<PointValueSample> page) {
        Map<String, List<AnalyticsModel.HistoryPoint>> out = new LinkedHashMap<>();
        for (PointValueSample sample : page.items()) {
            AnalyticsModel.SeriesRef ref = refs.get(sample.series());
            if (ref == null) continue;
            out.computeIfAbsent(ref.label(), ignored -> new ArrayList<>())
                    .add(new AnalyticsModel.HistoryPoint(
                            sample.deviceTime().toString(), sample.numericValue(), null, null, null, null));
        }
        String degradation = page.nextCursor() == null
                ? null
                : "raw history capped at the newest %d samples; narrow the window or switch to M4"
                        .formatted(page.items().size());
        return new AnalyticsModel.HistoryResponse(
                "history: "
                        + out.entrySet().stream()
                                .map(entry -> "%s (%d points)"
                                        .formatted(
                                                entry.getKey(), entry.getValue().size()))
                                .collect(Collectors.joining(", ")),
                page.items().size(),
                degradation,
                out);
    }

    @Override
    public Mono<AnalyticsModel.StatsResponse> computeStats(Long tenantId, AnalyticsModel.ComputeStatsRequest request) {
        return Mono.defer(() -> {
            requireTenant(tenantId);
            List<Double> percentiles = request == null
                            || request.percentiles() == null
                            || request.percentiles().isEmpty()
                    ? List.of(0.5, 0.95)
                    : request.percentiles().stream().distinct().toList();
            return resolveSeries(tenantId, request == null ? null : request.series())
                    .flatMap(series -> refs(tenantId, series)
                            .flatMap(refs -> pullPerSeries(
                                            series, resolveWindow(request == null ? null : request.window()))
                                    .map(data -> statsResponse(refs, data, percentiles))));
        });
    }

    private AnalyticsModel.StatsResponse statsResponse(
            Map<SeriesKey, AnalyticsModel.SeriesRef> refs,
            Map<SeriesKey, List<PointValueSample>> data,
            List<Double> percentiles) {
        Map<String, AnalyticsModel.StatItem> stats = new LinkedHashMap<>();
        long total = 0;
        String degradation = null;
        for (Map.Entry<SeriesKey, List<PointValueSample>> entry : data.entrySet()) {
            List<Double> values = entry.getValue().stream()
                    .map(PointValueSample::numericValue)
                    .filter(Objects::nonNull)
                    .sorted()
                    .toList();
            AnalyticsModel.SeriesRef ref = refs.get(entry.getKey());
            if (values.isEmpty()) {
                stats.put(
                        ref.label(),
                        new AnalyticsModel.StatItem(
                                ref, null, null, null, null, entry.getValue().size(), Map.of()));
                continue;
            }
            double mean =
                    values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double variance =
                    values.stream().mapToDouble(v -> (v - mean) * (v - mean)).sum() / values.size();
            Map<Double, Double> p = new LinkedHashMap<>();
            percentiles.forEach(percentile -> p.put(percentile, percentile(values, percentile)));
            stats.put(
                    ref.label(),
                    new AnalyticsModel.StatItem(
                            ref,
                            round(mean),
                            round(Math.sqrt(variance)),
                            values.getFirst(),
                            values.getLast(),
                            entry.getValue().size(),
                            p));
            total += entry.getValue().size();
            if (entry.getValue().size() >= MAX_SAMPLES_PER_PULL)
                degradation = "stats computed from the newest %d samples per series".formatted(MAX_SAMPLES_PER_PULL);
        }
        return new AnalyticsModel.StatsResponse(
                stats.entrySet().stream()
                        .map(entry -> "%s: mean=%s n=%d"
                                .formatted(
                                        entry.getKey(),
                                        entry.getValue().mean(),
                                        entry.getValue().count()))
                        .collect(Collectors.joining("; ", "Stats: ", "")),
                total,
                degradation,
                stats);
    }

    @Override
    public Mono<AnalyticsModel.CompareResponse> comparePeriods(
            Long tenantId, AnalyticsModel.ComparePeriodsRequest request) {
        return Mono.defer(() -> {
            requireTenant(tenantId);
            TimeWindow current = resolveWindow(request == null ? null : request.current());
            TimeWindow baseline = resolveWindow(request == null ? null : request.baseline());
            return resolveSeries(tenantId, request == null ? null : request.series())
                    .flatMap(series -> refs(tenantId, series)
                            .flatMap(refs -> Mono.zip(
                                            tsdbStore.aggregate(
                                                    SeriesFilter.of(series),
                                                    AggregateFunction.AVG,
                                                    current,
                                                    null,
                                                    DEADLINE),
                                            tsdbStore.aggregate(
                                                    SeriesFilter.of(series),
                                                    AggregateFunction.AVG,
                                                    baseline,
                                                    null,
                                                    DEADLINE))
                                    .map(tuple -> compareResponse(series, refs, tuple.getT1(), tuple.getT2()))));
        });
    }

    private AnalyticsModel.CompareResponse compareResponse(
            List<SeriesKey> series,
            Map<SeriesKey, AnalyticsModel.SeriesRef> refs,
            Map<SeriesKey, WindowAggregate> current,
            Map<SeriesKey, WindowAggregate> baseline) {
        Map<String, AnalyticsModel.CompareItem> comparisons = new LinkedHashMap<>();
        for (SeriesKey key : series) {
            WindowAggregate c = current.get(key);
            WindowAggregate b = baseline.get(key);
            AnalyticsModel.SeriesRef ref = refs.get(key);
            Double cv = c == null ? null : c.value();
            Double bv = b == null ? null : b.value();
            Double delta = cv == null || bv == null ? null : round(cv - bv);
            Double pct = delta == null || bv == 0 ? null : round(delta / Math.abs(bv) * 100);
            comparisons.put(
                    ref.label(),
                    new AnalyticsModel.CompareItem(
                            ref, cv, bv, delta, pct, c == null ? 0 : c.sampleCount(), b == null ? 0 : b.sampleCount()));
        }
        return new AnalyticsModel.CompareResponse(
                comparisons.entrySet().stream()
                        .map(entry -> "%s: %s → %s (%s%%)"
                                .formatted(
                                        entry.getKey(),
                                        entry.getValue().baseline(),
                                        entry.getValue().current(),
                                        Objects.toString(entry.getValue().pctChange(), "n/a")))
                        .collect(Collectors.joining("; ", "Period comparison (baseline → current): ", "")),
                comparisons.values().stream()
                        .mapToLong(item -> item.currentCount() + item.baselineCount())
                        .sum(),
                null,
                comparisons);
    }

    @Override
    public Mono<AnalyticsModel.RankResponse> rankEntities(Long tenantId, AnalyticsModel.RankEntitiesRequest request) {
        return Mono.defer(() -> {
            requireTenant(tenantId);
            GroupDimension dimension = dimensionOf(request == null ? null : request.dimension());
            String metric = request == null || request.metric() == null
                    ? "ACTIVITY"
                    : request.metric().toUpperCase();
            int limit = clamp(request == null || request.limit() == null ? 10 : request.limit(), 1, 50);
            TimeWindow window = resolveWindow(request == null ? null : request.window());
            if ("ACTIVITY".equals(metric)) {
                return tsdbStore
                        .countByDimension(tenantId, window, dimension, limit, DEADLINE)
                        .flatMap(counts -> Flux.fromIterable(counts)
                                .concatMap(count -> entityLabel(tenantId, dimension, count.entityId())
                                        .map(label -> new AnalyticsModel.RankItem(
                                                String.valueOf(count.entityId()), label, count.count(), null)))
                                .collectList())
                        .map(this::rankResponse);
            }
            AggregateFunction fn =
                    switch (metric) {
                        case "MEAN" -> AggregateFunction.AVG;
                        case "MAX" -> AggregateFunction.MAX;
                        case "MIN" -> AggregateFunction.MIN;
                        default ->
                            throw new ServiceException(
                                    "Unsupported rank metric: " + metric + " (expect ACTIVITY / MEAN / MAX / MIN)");
                    };
            return tsdbStore
                    .aggregate(SeriesFilter.tenantWide(tenantId), fn, window, null, DEADLINE)
                    .flatMap(values -> Flux.fromIterable(values.entrySet())
                            .sort(Comparator.comparing(
                                            (Map.Entry<SeriesKey, WindowAggregate> e) -> Objects.requireNonNullElse(
                                                    e.getValue().value(), Double.NEGATIVE_INFINITY))
                                    .reversed())
                            .take(limit)
                            .concatMap(entry -> ref(tenantId, entry.getKey())
                                    .map(ref -> new AnalyticsModel.RankItem(
                                            String.valueOf(
                                                    dimension == GroupDimension.DEVICE
                                                            ? entry.getKey().deviceId()
                                                            : entry.getKey().pointId()),
                                            ref.label(),
                                            entry.getValue().sampleCount(),
                                            round(entry.getValue().value()))))
                            .collectList())
                    .map(this::rankResponse);
        });
    }

    private AnalyticsModel.RankResponse rankResponse(List<AnalyticsModel.RankItem> ranked) {
        String conclusion = "top " + ranked.size() + ": "
                + ranked.stream()
                        .map(item -> "%s (%s)"
                                .formatted(
                                        Objects.requireNonNullElse(item.label(), item.entityId()),
                                        Objects.requireNonNullElse(item.metricValue(), item.count())))
                        .collect(Collectors.joining(", "));
        return new AnalyticsModel.RankResponse(
                conclusion,
                ranked.stream().mapToLong(AnalyticsModel.RankItem::count).sum(),
                null,
                ranked);
    }

    @Override
    public Mono<AnalyticsModel.TrendResponse> trendAnalysis(
            Long tenantId, AnalyticsModel.TrendAnalysisRequest request) {
        return Mono.defer(() -> {
            requireTenant(tenantId);
            TimeWindow window = resolveWindow(request == null ? null : request.window());
            int buckets = clamp(request == null || request.buckets() == null ? 50 : request.buckets(), 2, 200);
            Duration width = windowLength(window).dividedBy(buckets);
            return resolveSeries(tenantId, request == null ? null : request.series())
                    .flatMap(series -> refs(tenantId, series)
                            .flatMap(refs -> tsdbStore
                                    .bucketedAggregate(
                                            SeriesFilter.of(series),
                                            AggregateFunction.AVG,
                                            window,
                                            width,
                                            null,
                                            DEADLINE)
                                    .map(values -> trendResponse(series, refs, values))));
        });
    }

    private AnalyticsModel.TrendResponse trendResponse(
            List<SeriesKey> series,
            Map<SeriesKey, AnalyticsModel.SeriesRef> refs,
            Map<SeriesKey, List<BucketAggregate>> values) {
        Map<String, AnalyticsModel.TrendItem> trends = new LinkedHashMap<>();
        for (SeriesKey key : series) {
            List<Double> points = values.getOrDefault(key, List.of()).stream()
                    .map(BucketAggregate::value)
                    .filter(Objects::nonNull)
                    .toList();
            double first = points.isEmpty() ? 0 : points.getFirst();
            double last = points.isEmpty() ? 0 : points.getLast();
            double change = first == 0 ? 0 : (last - first) / Math.abs(first) * 100;
            trends.put(
                    refs.get(key).label(),
                    new AnalyticsModel.TrendItem(
                            refs.get(key),
                            points.size() < 2 ? 0 : round(leastSquaresSlope(points)),
                            first,
                            last,
                            round(change),
                            points.size()));
        }
        return new AnalyticsModel.TrendResponse(
                trends.entrySet().stream()
                        .map(entry -> "%s %s (%.1f%% over %d buckets)"
                                .formatted(
                                        entry.getKey(),
                                        entry.getValue().slopePerBucket() >= 0 ? "rising" : "falling",
                                        entry.getValue().totalChangePct(),
                                        entry.getValue().bucketCount()))
                        .collect(Collectors.joining("; ", "Trends: ", "")),
                trends.values().stream()
                        .mapToLong(AnalyticsModel.TrendItem::bucketCount)
                        .sum(),
                null,
                trends);
    }

    @Override
    public Mono<AnalyticsModel.ThresholdResponse> thresholdReport(
            Long tenantId, AnalyticsModel.ThresholdReportRequest request) {
        return Mono.defer(() -> {
            requireTenant(tenantId);
            if (request == null || request.threshold() == null)
                return Mono.error(new ServiceException("threshold is required"));
            TimeWindow window = resolveWindow(request.window());
            boolean greater = request.operator() == null || "GREATER".equalsIgnoreCase(request.operator());
            return resolveSeries(tenantId, request.series())
                    .flatMap(series -> refs(tenantId, series)
                            .flatMap(refs -> pullPerSeries(series, window)
                                    .map(data -> thresholdResponse(refs, data, request.threshold(), greater))));
        });
    }

    private AnalyticsModel.ThresholdResponse thresholdResponse(
            Map<SeriesKey, AnalyticsModel.SeriesRef> refs,
            Map<SeriesKey, List<PointValueSample>> data,
            double threshold,
            boolean greater) {
        Map<String, AnalyticsModel.ThresholdItem> report = new LinkedHashMap<>();
        long total = 0;
        for (Map.Entry<SeriesKey, List<PointValueSample>> entry : data.entrySet()) {
            List<PointValueSample> samples = entry.getValue().stream()
                    .filter(sample -> sample.numericValue() != null)
                    .sorted(Comparator.comparing(PointValueSample::deviceTime))
                    .toList();
            List<AnalyticsModel.ThresholdInterval> intervals = new ArrayList<>();
            Instant open = null;
            Instant last = null;
            long count = 0;
            Double peak = null;
            for (PointValueSample sample : samples) {
                boolean hit = greater ? sample.numericValue() > threshold : sample.numericValue() < threshold;
                if (!hit) {
                    if (open != null && intervals.size() < MAX_THRESHOLD_INTERVALS)
                        intervals.add(new AnalyticsModel.ThresholdInterval(open.toString(), last.toString()));
                    open = null;
                    continue;
                }
                count++;
                peak = peak == null || (greater ? sample.numericValue() > peak : sample.numericValue() < peak)
                        ? sample.numericValue()
                        : peak;
                if (open == null) open = sample.deviceTime();
                last = sample.deviceTime();
            }
            if (open != null && intervals.size() < MAX_THRESHOLD_INTERVALS)
                intervals.add(new AnalyticsModel.ThresholdInterval(open.toString(), last.toString()));
            long seconds = intervals.stream()
                    .mapToLong(
                            interval -> Duration.between(Instant.parse(interval.from()), Instant.parse(interval.to()))
                                    .getSeconds())
                    .sum();
            report.put(
                    refs.get(entry.getKey()).label(),
                    new AnalyticsModel.ThresholdItem(refs.get(entry.getKey()), count, seconds, peak, intervals));
            total += count;
        }
        return new AnalyticsModel.ThresholdResponse(
                "threshold breaches: "
                        + report.entrySet().stream()
                                .map(entry -> "%s (%d samples)"
                                        .formatted(
                                                entry.getKey(), entry.getValue().exceedCount()))
                                .collect(Collectors.joining(", ")),
                total,
                null,
                report);
    }

    @Override
    public Mono<AnalyticsModel.CorrelationResponse> correlate(Long tenantId, AnalyticsModel.CorrelateRequest request) {
        return Mono.defer(() -> {
            requireTenant(tenantId);
            if (request == null || request.seriesA() == null || request.seriesB() == null)
                return Mono.error(new ServiceException("seriesA and seriesB are required"));
            TimeWindow window = resolveWindow(request.window());
            Duration bucket = Duration.ofSeconds(clamp(request.alignBucketSeconds(), 10, 86400));
            return resolveOne(tenantId, request.seriesA())
                    .zipWith(resolveOne(tenantId, request.seriesB()))
                    .flatMap(pair -> {
                        if (tsdbStore.capabilities().correlation()) {
                            return tsdbStore
                                    .correlation(pair.getT1(), pair.getT2(), window, bucket, DEADLINE)
                                    .map(result ->
                                            correlationResponse(result.pearson(), result.alignedBuckets(), "STORE"));
                        }
                        return Mono.zip(
                                        tsdbStore.bucketedAggregate(
                                                SeriesFilter.of(pair.getT1()),
                                                AggregateFunction.AVG,
                                                window,
                                                bucket,
                                                null,
                                                DEADLINE),
                                        tsdbStore.bucketedAggregate(
                                                SeriesFilter.of(pair.getT2()),
                                                AggregateFunction.AVG,
                                                window,
                                                bucket,
                                                null,
                                                DEADLINE))
                                .map(values -> {
                                    Map<Long, Double> a =
                                            averages(values.getT1().getOrDefault(pair.getT1(), List.of()));
                                    Map<Long, Double> b =
                                            averages(values.getT2().getOrDefault(pair.getT2(), List.of()));
                                    List<double[]> aligned = a.entrySet().stream()
                                            .filter(entry -> b.containsKey(entry.getKey()))
                                            .map(entry -> new double[] {entry.getValue(), b.get(entry.getKey())})
                                            .toList();
                                    return correlationResponse(pearson(aligned), aligned.size(), "FACADE");
                                });
                    });
        });
    }

    private static Map<Long, Double> averages(List<BucketAggregate> buckets) {
        Map<Long, Double> values = new TreeMap<>();
        for (BucketAggregate bucket : buckets)
            if (bucket.value() != null) values.put(bucket.bucketStart().toEpochMilli(), bucket.value());
        return values;
    }

    private AnalyticsModel.CorrelationResponse correlationResponse(double pearson, long aligned, String method) {
        String strength = Math.abs(pearson) >= 0.9
                ? "very strong"
                : Math.abs(pearson) >= 0.7
                        ? "strong"
                        : Math.abs(pearson) >= 0.4 ? "moderate" : Math.abs(pearson) >= 0.2 ? "weak" : "negligible";
        return new AnalyticsModel.CorrelationResponse(
                "Pearson r=%.4f (%s %s) over %d aligned buckets"
                        .formatted(pearson, strength, pearson >= 0 ? "positive" : "negative", aligned),
                aligned,
                "FACADE".equals(method) ? "computed from bucketed pulls" : null,
                round(pearson),
                aligned,
                method);
    }

    @Override
    public Mono<AnalyticsModel.QualityResponse> qualityReport(
            Long tenantId, AnalyticsModel.QualityReportRequest request) {
        return Mono.defer(() -> {
            requireTenant(tenantId);
            TimeWindow window = resolveWindow(request == null ? null : request.window());
            int silentMinutes = clamp(
                    request == null || request.silentMinutes() == null ? 30 : request.silentMinutes(), 5, 24 * 60);
            return Mono.zip(
                            tsdbStore.lastSeenPerSeries(tenantId, window, DEADLINE),
                            tsdbStore.history(
                                    SeriesFilter.tenantWide(tenantId), window, null, MAX_SAMPLES_PER_PULL, DEADLINE),
                            countTenantPoints(tenantId))
                    .flatMap(tuple -> refs(
                                    tenantId,
                                    tuple.getT1().stream()
                                            .map(row -> row.series())
                                            .toList())
                            .map(refs ->
                                    qualityResponse(tuple.getT1(), tuple.getT2(), tuple.getT3(), refs, silentMinutes)));
        });
    }

    private AnalyticsModel.QualityResponse qualityResponse(
            List<SeriesLastSeen> seen,
            CursorPage<PointValueSample> probe,
            long totalPoints,
            Map<SeriesKey, AnalyticsModel.SeriesRef> refs,
            int silentMinutes) {
        Instant silentSince = Instant.now().minus(Duration.ofMinutes(silentMinutes));
        List<AnalyticsModel.SilentItem> silent = seen.stream()
                .filter(row -> row.lastSeen() != null && row.lastSeen().isBefore(silentSince))
                .map(row -> new AnalyticsModel.SilentItem(
                        refs.get(row.series()), row.lastSeen().toString()))
                .toList();
        Map<String, Long> quality = new TreeMap<>(probe.items().stream()
                .collect(Collectors.groupingBy(sample -> String.valueOf(sample.quality()), Collectors.counting())));
        double coverage = totalPoints == 0 ? 0 : Math.min(100, round(seen.size() * 100.0 / totalPoints));
        return new AnalyticsModel.QualityResponse(
                "%d of %d points reported in the window (%.1f%% coverage), %d silent for over %d minutes"
                        .formatted(seen.size(), totalPoints, coverage, silent.size(), silentMinutes),
                probe.items().size(),
                probe.nextCursor() == null
                        ? null
                        : "quality distribution sampled from newest %d samples"
                                .formatted(probe.items().size()),
                seen.size(),
                totalPoints,
                coverage,
                silent,
                quality);
    }

    private TimeWindow resolveWindow(AnalyticsModel.TimeRange range) {
        Instant to = range != null && range.toIso() != null ? Instant.parse(range.toIso()) : Instant.now();
        Instant from = range != null && range.fromIso() != null
                ? Instant.parse(range.fromIso())
                : to.minus(Duration.ofHours(
                        range != null && range.rangeHours() != null
                                ? clamp(range.rangeHours(), 1, MAX_WINDOW_HOURS)
                                : 24));
        if (!from.isBefore(to)) throw new ServiceException("window from must be before to");
        if (Duration.between(from, to).toHours() > MAX_WINDOW_HOURS)
            throw new ServiceException("window span exceeds %d hours".formatted(MAX_WINDOW_HOURS));
        return new TimeWindow(from, to);
    }

    private Mono<List<SeriesKey>> resolveSeries(Long tenantId, List<AnalyticsModel.SeriesSelector> selectors) {
        if (selectors == null || selectors.isEmpty())
            return Mono.error(new ServiceException("at least one series selector is required"));
        if (selectors.size() > MAX_SERIES_PER_CALL)
            return Mono.error(new ServiceException("at most %d series per call".formatted(MAX_SERIES_PER_CALL)));
        return Flux.fromIterable(selectors)
                .concatMap(selector -> resolveOne(tenantId, selector))
                .collectList();
    }

    private Mono<SeriesKey> resolveOne(Long tenantId, AnalyticsModel.SeriesSelector selector) {
        if (selector == null) return Mono.error(new ServiceException("series selector is required"));
        Mono<Long> device = selector.deviceId() != null
                ? Mono.just(selector.deviceId())
                : selector.deviceName() == null || selector.deviceName().isBlank()
                        ? Mono.empty()
                        : uniqueDevice(tenantId, selector.deviceName());
        return device.switchIfEmpty(Mono.error(new ServiceException("series needs deviceId or deviceName")))
                .flatMap(deviceId -> {
                    Mono<Long> point = selector.pointId() != null
                            ? Mono.just(selector.pointId())
                            : selector.pointName() == null
                                            || selector.pointName().isBlank()
                                    ? Mono.empty()
                                    : uniquePoint(tenantId, deviceId, selector.pointName());
                    return point.switchIfEmpty(Mono.error(new ServiceException("series needs pointId or pointName")))
                            .map(pointId -> new SeriesKey(tenantId, deviceId, pointId));
                });
    }

    private Mono<Long> uniqueDevice(Long tenantId, String name) {
        FacadeDeviceOffsetQuery query =
                new FacadeDeviceOffsetQuery(tenantId, name, null, null, null, null, null, null, null, 0, 50, List.of());
        return deviceFacade
                .listReactive(query)
                .map(OffsetPage::items)
                .map(items -> items.stream()
                        .filter(item -> name.equals(item.getDeviceName()))
                        .toList())
                .flatMap(items -> items.size() == 1
                        ? Mono.just(items.getFirst().getId())
                        : Mono.error(new ServiceException(
                                "device name '%s' matched %d devices".formatted(name, items.size()))));
    }

    private Mono<Long> uniquePoint(Long tenantId, Long deviceId, String name) {
        FacadePointOffsetQuery query = new FacadePointOffsetQuery(
                tenantId, name, null, null, null, null, null, null, null, null, deviceId, 0, 50, List.of());
        return pointFacade
                .listReactive(query)
                .map(OffsetPage::items)
                .map(items -> items.stream()
                        .filter(item -> name.equals(item.getPointName()))
                        .toList())
                .flatMap(items -> items.size() == 1
                        ? Mono.just(items.getFirst().getId())
                        : Mono.error(new ServiceException(
                                "point name '%s' matched %d points".formatted(name, items.size()))));
    }

    private Mono<Map<SeriesKey, AnalyticsModel.SeriesRef>> refs(Long tenantId, List<SeriesKey> series) {
        return Flux.fromIterable(series)
                .concatMap(key -> ref(tenantId, key).map(value -> Map.entry(key, value)))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue, LinkedHashMap::new);
    }

    private Mono<AnalyticsModel.SeriesRef> ref(Long tenantId, SeriesKey key) {
        Mono<String> device = deviceFacade
                .getByIdReactive(tenantId, key.deviceId())
                .map(FacadeDeviceBO::getDeviceName)
                .defaultIfEmpty(String.valueOf(key.deviceId()));
        Mono<String> point = pointFacade
                .getByIdReactive(tenantId, key.pointId())
                .map(FacadePointBO::getPointName)
                .defaultIfEmpty(String.valueOf(key.pointId()));
        return Mono.zip(device, point)
                .map(names -> new AnalyticsModel.SeriesRef(
                        key.deviceId(), key.pointId(), names.getT1() + "/" + names.getT2()));
    }

    private Mono<String> entityLabel(Long tenantId, GroupDimension dimension, long id) {
        return switch (dimension) {
            case DEVICE ->
                deviceFacade
                        .getByIdReactive(tenantId, id)
                        .map(FacadeDeviceBO::getDeviceName)
                        .defaultIfEmpty(String.valueOf(id));
            case POINT ->
                pointFacade
                        .getByIdReactive(tenantId, id)
                        .map(FacadePointBO::getPointName)
                        .defaultIfEmpty(String.valueOf(id));
            case DRIVER ->
                driverFacade
                        .getByIdReactive(tenantId, id)
                        .map(driver -> driver.getDriverName())
                        .defaultIfEmpty(String.valueOf(id));
        };
    }

    private Mono<Map<SeriesKey, List<PointValueSample>>> pullPerSeries(List<SeriesKey> series, TimeWindow window) {
        return Flux.fromIterable(series)
                .concatMap(key -> history(key, window).collectList().map(samples -> Map.entry(key, samples)))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue, LinkedHashMap::new);
    }

    private Flux<PointValueSample> history(SeriesKey key, TimeWindow window) {
        return tsdbStore
                .history(SeriesFilter.of(key), window, null, HISTORY_PAGE, DEADLINE)
                .expand(page -> page.nextCursor() == null
                        ? Mono.empty()
                        : tsdbStore.history(SeriesFilter.of(key), window, page.nextCursor(), HISTORY_PAGE, DEADLINE))
                .concatMapIterable(CursorPage::items)
                .take(MAX_SAMPLES_PER_PULL);
    }

    private Mono<Long> countTenantPoints(Long tenantId) {
        return pointFacade
                .listReactive(new FacadePointOffsetQuery(tenantId, 0, 1))
                .map(OffsetPage::total);
    }
}
