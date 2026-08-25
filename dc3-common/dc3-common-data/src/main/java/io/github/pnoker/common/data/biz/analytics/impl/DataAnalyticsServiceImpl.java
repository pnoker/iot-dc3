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
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceQuery;
import io.github.pnoker.common.facade.entity.query.FacadePointQuery;
import io.github.pnoker.common.tsdb.model.TsdbModel.AggregateFunction;
import io.github.pnoker.common.tsdb.model.TsdbModel.BucketAggregate;
import io.github.pnoker.common.tsdb.model.TsdbModel.CorrelationResult;
import io.github.pnoker.common.tsdb.model.TsdbModel.CursorPage;
import io.github.pnoker.common.tsdb.model.TsdbModel.DimensionCount;
import io.github.pnoker.common.tsdb.model.TsdbModel.GroupDimension;
import io.github.pnoker.common.tsdb.model.TsdbModel.PointValueSample;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesCount;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesFilter;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesKey;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesLastSeen;
import io.github.pnoker.common.tsdb.model.TsdbModel.TimeWindow;
import io.github.pnoker.common.tsdb.model.TsdbModel.TsdbDeadline;
import io.github.pnoker.common.tsdb.spi.TsdbStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

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

/**
 * Default analytics facade: composes TSDB port primitives with relational
 * metadata resolution. Every method re-validates the tenant scope (S11 hard
 * constraint — the facade is the second gate behind the controller), resolves
 * names through facades with candidate-surfacing errors, and bounds its scan
 * volume; anything computed from a bounded subset says so in {@code degradation}.
 *
 * @author pnoker
 * @since 2026.8.21
 */
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

    private final TsdbStore tsdbStore;

    private final PointValueLatestService pointValueLatestService;

    private final PointValueSampleConverter converter;

    private final DeviceFacade deviceFacade;

    private final PointFacade pointFacade;

    private final DriverFacade driverFacade;

    // ===== query_latest =====

    @Override
    public AnalyticsModel.LatestValuesResponse queryLatest(Long tenantId, AnalyticsModel.QueryLatestRequest request) {
        requireTenant(tenantId);
        List<SeriesKey> series = resolveSeries(tenantId, request == null ? null : request.series());

        Map<Long, List<Long>> pointsByDevice = series.stream().collect(Collectors.groupingBy(
                SeriesKey::deviceId, LinkedHashMap::new, Collectors.mapping(SeriesKey::pointId, Collectors.toList())));
        List<AnalyticsModel.LatestItem> items = new ArrayList<>(series.size());
        pointsByDevice.forEach((deviceId, pointIds) -> {
            for (PointValueBO latest : pointValueLatestService.listLatest(tenantId, deviceId, pointIds)) {
                items.add(new AnalyticsModel.LatestItem(refOf(tenantId, latest.getDeviceId(), latest.getPointId()),
                        latest.getRawValue(), latest.getCalValue(), latest.getNumValue(),
                        Objects.nonNull(latest.getNumValue()) ? "NUMERIC" : "STRING",
                        Objects.toString(latest.getCreateTime(), null), true));
            }
        });
        // A resolved series without a projection row simply has no reading yet.
        Map<String, AnalyticsModel.LatestItem> byLabel = new LinkedHashMap<>();
        items.forEach(item -> byLabel.put(label(item.series()), item));
        for (SeriesKey key : series) {
            String label = label(refOf(tenantId, key.deviceId(), key.pointId()));
            byLabel.putIfAbsent(label, new AnalyticsModel.LatestItem(refOf(tenantId, key.deviceId(), key.pointId()),
                    null, null, null, null, null, false));
        }

        String conclusion = byLabel.values().stream()
                .filter(AnalyticsModel.LatestItem::hasValue)
                .map(item -> "%s = %s (at %s)".formatted(label(item.series()),
                        Objects.requireNonNullElse(item.calValue(), item.rawValue()), item.createTime()))
                .collect(Collectors.joining("; ", "Latest values: ", ""));
        return new AnalyticsModel.LatestValuesResponse(conclusion, items.size(), null, List.copyOf(byLabel.values()));
    }

    // ===== query_history =====

    @Override
    public AnalyticsModel.HistoryResponse queryHistory(Long tenantId, AnalyticsModel.QueryHistoryRequest request) {
        requireTenant(tenantId);
        List<SeriesKey> series = resolveSeries(tenantId, request == null ? null : request.series());
        TimeWindow window = resolveWindow(request == null ? null : request.window());
        int maxPoints = clamp(request == null || request.maxPoints() == null ? 200 : request.maxPoints(), 1, 1000);
        boolean m4 = Objects.nonNull(request) && "M4".equalsIgnoreCase(request.mode());

        Map<String, List<AnalyticsModel.HistoryPoint>> out = new LinkedHashMap<>();
        long sampleCount = 0;
        String degradation = null;
        if (m4) {
            Duration bucketWidth = windowLength(window).dividedBy(maxPoints);
            Map<SeriesKey, List<BucketAggregate>> minBuckets = tsdbStore.bucketedAggregate(
                    SeriesFilter.of(series), AggregateFunction.MIN, window, bucketWidth, null, DEADLINE);
            Map<SeriesKey, List<BucketAggregate>> maxBuckets = tsdbStore.bucketedAggregate(
                    SeriesFilter.of(series), AggregateFunction.MAX, window, bucketWidth, null, DEADLINE);
            Map<SeriesKey, List<BucketAggregate>> firstBuckets = tsdbStore.bucketedAggregate(
                    SeriesFilter.of(series), AggregateFunction.FIRST, window, bucketWidth, null, DEADLINE);
            Map<SeriesKey, List<BucketAggregate>> lastBuckets = tsdbStore.bucketedAggregate(
                    SeriesFilter.of(series), AggregateFunction.LAST, window, bucketWidth, null, DEADLINE);
            for (SeriesKey key : series) {
                List<AnalyticsModel.HistoryPoint> points = new ArrayList<>();
                for (int i = 0; i < minBuckets.getOrDefault(key, List.of()).size(); i++) {
                    BucketAggregate min = minBuckets.get(key).get(i);
                    points.add(new AnalyticsModel.HistoryPoint(min.bucketStart().toString(), null,
                            value(firstBuckets, key, i), min.value(), value(maxBuckets, key, i),
                            value(lastBuckets, key, i)));
                }
                out.put(label(refOf(tenantId, key.deviceId(), key.pointId())), points);
                sampleCount += points.size();
            }
        } else {
            CursorPage<PointValueSample> page = tsdbStore.history(SeriesFilter.of(series), window, null,
                    Math.min(maxPoints * series.size(), MAX_SAMPLES_PER_PULL), DEADLINE);
            for (PointValueSample sample : page.items()) {
                out.computeIfAbsent(label(refOf(tenantId, sample.series().deviceId(), sample.series().pointId())),
                                key -> new ArrayList<>())
                        .add(new AnalyticsModel.HistoryPoint(sample.deviceTime().toString(), sample.numericValue(),
                                null, null, null, null));
            }
            sampleCount = page.items().size();
            if (Objects.nonNull(page.nextCursor())) {
                degradation = "raw history capped at the newest %d samples across the requested series; narrow the window or switch to M4"
                        .formatted(page.items().size());
            }
        }

        String conclusion = "history: " + out.entrySet().stream()
                .map(entry -> "%s (%d points)".formatted(entry.getKey(), entry.getValue().size()))
                .collect(Collectors.joining(", "));
        return new AnalyticsModel.HistoryResponse(conclusion, sampleCount, degradation, out);
    }

    // ===== compute_stats =====

    @Override
    public AnalyticsModel.StatsResponse computeStats(Long tenantId, AnalyticsModel.ComputeStatsRequest request) {
        requireTenant(tenantId);
        List<SeriesKey> series = resolveSeries(tenantId, request == null ? null : request.series());
        TimeWindow window = resolveWindow(request == null ? null : request.window());
        List<Double> percentiles = CollectionUtils.isEmpty(request == null ? null : request.percentiles())
                ? List.of(0.5, 0.95) : request.percentiles().stream().distinct().toList();

        Map<String, AnalyticsModel.StatItem> stats = new LinkedHashMap<>();
        String degradation = null;
        long total = 0;
        for (Map.Entry<SeriesKey, List<PointValueSample>> entry : pullPerSeries(series, window).entrySet()) {
            SeriesKey key = entry.getKey();
            List<Double> values = entry.getValue().stream()
                    .map(PointValueSample::numericValue).filter(Objects::nonNull).sorted().toList();
            if (values.isEmpty()) {
                stats.put(label(refOf(tenantId, key.deviceId(), key.pointId())),
                        new AnalyticsModel.StatItem(refOf(tenantId, key.deviceId(), key.pointId()),
                                null, null, null, null, entry.getValue().size(), Map.of()));
                continue;
            }
            double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
            double variance = values.stream().mapToDouble(v -> (v - mean) * (v - mean)).sum() / values.size();
            Map<Double, Double> percentileValues = new LinkedHashMap<>();
            percentiles.forEach(p -> percentileValues.put(p, percentile(values, p)));
            stats.put(label(refOf(tenantId, key.deviceId(), key.pointId())),
                    new AnalyticsModel.StatItem(refOf(tenantId, key.deviceId(), key.pointId()),
                            round(mean), round(Math.sqrt(variance)), values.getFirst(), values.getLast(),
                            entry.getValue().size(), percentileValues));
            total += entry.getValue().size();
            if (entry.getValue().size() >= MAX_SAMPLES_PER_PULL) {
                degradation = "stats computed from the newest %d samples per series".formatted(MAX_SAMPLES_PER_PULL);
            }
        }

        String conclusion = stats.entrySet().stream()
                .map(entry -> "%s: mean=%s n=%d".formatted(entry.getKey(), entry.getValue().mean(),
                        entry.getValue().count()))
                .collect(Collectors.joining("; ", "Stats: ", ""));
        return new AnalyticsModel.StatsResponse(conclusion, total, degradation, stats);
    }

    // ===== compare_periods =====

    @Override
    public AnalyticsModel.CompareResponse comparePeriods(Long tenantId, AnalyticsModel.ComparePeriodsRequest request) {
        requireTenant(tenantId);
        List<SeriesKey> series = resolveSeries(tenantId, request == null ? null : request.series());
        TimeWindow current = resolveWindow(request == null ? null : request.current());
        TimeWindow baseline = resolveWindow(request == null ? null : request.baseline());

        Map<SeriesKey, io.github.pnoker.common.tsdb.model.TsdbModel.WindowAggregate> currentAgg =
                tsdbStore.aggregate(SeriesFilter.of(series), AggregateFunction.AVG, current, null, DEADLINE);
        Map<SeriesKey, io.github.pnoker.common.tsdb.model.TsdbModel.WindowAggregate> baselineAgg =
                tsdbStore.aggregate(SeriesFilter.of(series), AggregateFunction.AVG, baseline, null, DEADLINE);

        Map<String, AnalyticsModel.CompareItem> comparisons = new LinkedHashMap<>();
        for (SeriesKey key : series) {
            io.github.pnoker.common.tsdb.model.TsdbModel.WindowAggregate c = currentAgg.get(key);
            io.github.pnoker.common.tsdb.model.TsdbModel.WindowAggregate b = baselineAgg.get(key);
            AnalyticsModel.SeriesRef ref = refOf(tenantId, key.deviceId(), key.pointId());
            if (Objects.isNull(c) || Objects.isNull(b) || Objects.isNull(c.value()) || Objects.isNull(b.value())) {
                comparisons.put(label(ref), new AnalyticsModel.CompareItem(ref,
                        Objects.isNull(c) ? null : c.value(), Objects.isNull(b) ? null : b.value(),
                        null, null, Objects.isNull(c) ? 0 : c.sampleCount(), Objects.isNull(b) ? 0 : b.sampleCount()));
                continue;
            }
            double delta = c.value() - b.value();
            comparisons.put(label(ref), new AnalyticsModel.CompareItem(ref, c.value(), b.value(), round(delta),
                    b.value() == 0 ? null : round(delta / Math.abs(b.value()) * 100),
                    c.sampleCount(), b.sampleCount()));
        }

        String conclusion = comparisons.entrySet().stream()
                .map(entry -> "%s: %s → %s (%s%%)".formatted(entry.getKey(), entry.getValue().baseline(),
                        entry.getValue().current(), Objects.toString(entry.getValue().pctChange(), "n/a")))
                .collect(Collectors.joining("; ", "Period comparison (baseline → current): ", ""));
        return new AnalyticsModel.CompareResponse(conclusion,
                comparisons.values().stream().mapToLong(c -> c.currentCount() + c.baselineCount()).sum(),
                null, comparisons);
    }

    // ===== rank_entities =====

    @Override
    public AnalyticsModel.RankResponse rankEntities(Long tenantId, AnalyticsModel.RankEntitiesRequest request) {
        requireTenant(tenantId);
        GroupDimension dimension = dimensionOf(request == null ? null : request.dimension());
        String metric = Objects.isNull(request) || Objects.isNull(request.metric()) ? "ACTIVITY"
                : request.metric().toUpperCase();
        TimeWindow window = resolveWindow(request == null ? null : request.window());
        int limit = clamp(request == null || request.limit() == null ? 10 : request.limit(), 1, 50);

        List<AnalyticsModel.RankItem> ranked;
        if ("ACTIVITY".equals(metric)) {
            List<DimensionCount> counts;
            try {
                counts = tsdbStore.countByDimension(tenantId, window, dimension, limit, DEADLINE);
            } catch (UnsupportedOperationException e) {
                // Structured refusal the caller (MCP tool) can act on — a raw
                // 500 hides the store capability gap behind a stack trace.
                throw new ServiceException("The selected tsdb store cannot group by " + dimension
                        + " — pick another dimension", e);
            }
            ranked = counts.stream().map(count -> new AnalyticsModel.RankItem(
                    String.valueOf(count.entityId()), labelOfEntity(tenantId, dimension, count.entityId()),
                    count.count(), null)).toList();
        } else {
            AggregateFunction fn = switch (metric) {
                case "MEAN" -> AggregateFunction.AVG;
                case "MAX" -> AggregateFunction.MAX;
                case "MIN" -> AggregateFunction.MIN;
                default -> throw new ServiceException("Unsupported rank metric: " + metric
                        + " (expect ACTIVITY / MEAN / MAX / MIN)");
            };
            Map<SeriesKey, io.github.pnoker.common.tsdb.model.TsdbModel.WindowAggregate> aggregates =
                    tsdbStore.aggregate(SeriesFilter.tenantWide(tenantId), fn, window, null, DEADLINE);
            Comparator<Map.Entry<SeriesKey, io.github.pnoker.common.tsdb.model.TsdbModel.WindowAggregate>> order =
                    Comparator.comparing(entry -> Objects.requireNonNullElse(entry.getValue().value(), Double.NEGATIVE_INFINITY),
                            Comparator.reverseOrder());
            ranked = aggregates.entrySet().stream().sorted(order).limit(limit)
                    .map(entry -> new AnalyticsModel.RankItem(
                            String.valueOf(dimension == GroupDimension.DEVICE ? entry.getKey().deviceId()
                                    : entry.getKey().pointId()),
                            label(refOf(tenantId, entry.getKey().deviceId(), entry.getKey().pointId())),
                            entry.getValue().sampleCount(), round(entry.getValue().value())))
                    .toList();
        }

        String conclusion = "top " + ranked.size() + " by " + metric.toLowerCase() + ": "
                + ranked.stream().map(item -> "%s (%s)".formatted(Objects.requireNonNullElse(item.label(),
                        item.entityId()), Objects.requireNonNullElse(item.metricValue(), item.count())))
                .collect(Collectors.joining(", "));
        return new AnalyticsModel.RankResponse(conclusion,
                ranked.stream().mapToLong(AnalyticsModel.RankItem::count).sum(), null, ranked);
    }

    // ===== trend_analysis =====

    @Override
    public AnalyticsModel.TrendResponse trendAnalysis(Long tenantId, AnalyticsModel.TrendAnalysisRequest request) {
        requireTenant(tenantId);
        List<SeriesKey> series = resolveSeries(tenantId, request == null ? null : request.series());
        TimeWindow window = resolveWindow(request == null ? null : request.window());
        int buckets = clamp(request == null || request.buckets() == null ? 50 : request.buckets(), 2, 200);
        Duration bucketWidth = windowLength(window).dividedBy(buckets);

        Map<SeriesKey, List<BucketAggregate>> bucketed = tsdbStore.bucketedAggregate(
                SeriesFilter.of(series), AggregateFunction.AVG, window, bucketWidth, null, DEADLINE);
        Map<String, AnalyticsModel.TrendItem> trends = new LinkedHashMap<>();
        for (SeriesKey key : series) {
            List<Double> values = bucketed.getOrDefault(key, List.of()).stream()
                    .map(BucketAggregate::value).filter(Objects::nonNull).toList();
            if (values.size() < 2) {
                trends.put(label(refOf(tenantId, key.deviceId(), key.pointId())),
                        new AnalyticsModel.TrendItem(refOf(tenantId, key.deviceId(), key.pointId()),
                                0, values.isEmpty() ? 0 : values.getFirst(), values.isEmpty() ? 0 : values.getLast(),
                                0, values.size()));
                continue;
            }
            double slope = leastSquaresSlope(values);
            double totalPct = values.getFirst() == 0 ? 0
                    : (values.getLast() - values.getFirst()) / Math.abs(values.getFirst()) * 100;
            trends.put(label(refOf(tenantId, key.deviceId(), key.pointId())),
                    new AnalyticsModel.TrendItem(refOf(tenantId, key.deviceId(), key.pointId()), round(slope),
                            values.getFirst(), values.getLast(), round(totalPct), values.size()));
        }

        String conclusion = trends.entrySet().stream()
                .map(entry -> "%s %s (%.1f%% over %d buckets)".formatted(entry.getKey(),
                        entry.getValue().slopePerBucket() >= 0 ? "rising" : "falling",
                        entry.getValue().totalChangePct(), entry.getValue().bucketCount()))
                .collect(Collectors.joining("; ", "Trends: ", ""));
        return new AnalyticsModel.TrendResponse(conclusion,
                trends.values().stream().mapToLong(AnalyticsModel.TrendItem::bucketCount).sum(), null, trends);
    }

    // ===== threshold_report =====

    @Override
    public AnalyticsModel.ThresholdResponse thresholdReport(Long tenantId, AnalyticsModel.ThresholdReportRequest request) {
        requireTenant(tenantId);
        List<SeriesKey> series = resolveSeries(tenantId, request == null ? null : request.series());
        TimeWindow window = resolveWindow(request == null ? null : request.window());
        boolean greater = Objects.isNull(request) || Objects.isNull(request.operator())
                || "GREATER".equalsIgnoreCase(request.operator());
        double threshold = Objects.requireNonNull(Objects.isNull(request) ? null : request.threshold(),
                "threshold is required");

        Map<String, AnalyticsModel.ThresholdItem> report = new LinkedHashMap<>();
        String degradation = null;
        for (Map.Entry<SeriesKey, List<PointValueSample>> entry : pullPerSeries(series, window).entrySet()) {
            SeriesKey key = entry.getKey();
            List<PointValueSample> samples = entry.getValue().stream()
                    .filter(sample -> Objects.nonNull(sample.numericValue()))
                    .sorted(Comparator.comparing(PointValueSample::deviceTime)).toList();
            List<AnalyticsModel.ThresholdInterval> intervals = new ArrayList<>();
            Instant open = null;
            Instant last = null;
            long exceed = 0;
            Double peak = null;
            for (PointValueSample sample : samples) {
                boolean hit = greater ? sample.numericValue() > threshold : sample.numericValue() < threshold;
                if (hit) {
                    exceed++;
                    peak = Objects.isNull(peak) ? sample.numericValue()
                            : (greater ? Math.max(peak, sample.numericValue()) : Math.min(peak, sample.numericValue()));
                    if (Objects.isNull(open)) {
                        open = sample.deviceTime();
                    }
                    last = sample.deviceTime();
                } else if (Objects.nonNull(open)) {
                    intervals.add(new AnalyticsModel.ThresholdInterval(open.toString(), sample.deviceTime().toString()));
                    open = null;
                }
            }
            if (Objects.nonNull(open) && Objects.nonNull(last)) {
                intervals.add(new AnalyticsModel.ThresholdInterval(open.toString(), last.toString()));
            }
            long totalSeconds = intervals.stream()
                    .mapToLong(interval -> Duration.between(Instant.parse(interval.from()), Instant.parse(interval.to())).toSeconds())
                    .sum();
            if (intervals.size() > MAX_THRESHOLD_INTERVALS) {
                intervals = new ArrayList<>(intervals.subList(0, MAX_THRESHOLD_INTERVALS));
                degradation = "interval list capped at %d per series".formatted(MAX_THRESHOLD_INTERVALS);
            }
            report.put(label(refOf(tenantId, key.deviceId(), key.pointId())),
                    new AnalyticsModel.ThresholdItem(refOf(tenantId, key.deviceId(), key.pointId()),
                            exceed, totalSeconds, peak, intervals));
            if (samples.size() >= MAX_SAMPLES_PER_PULL) {
                degradation = "report computed from the newest %d samples per series".formatted(MAX_SAMPLES_PER_PULL);
            }
        }

        String conclusion = report.entrySet().stream()
                .map(entry -> "%s: %d samples %s %s over %ds".formatted(entry.getKey(), entry.getValue().exceedCount(),
                        greater ? ">" : "<", threshold, entry.getValue().totalSeconds()))
                .collect(Collectors.joining("; ", "Threshold report: ", ""));
        return new AnalyticsModel.ThresholdResponse(conclusion,
                report.values().stream().mapToLong(AnalyticsModel.ThresholdItem::exceedCount).sum(),
                degradation, report);
    }

    // ===== correlate =====

    @Override
    public AnalyticsModel.CorrelationResponse correlate(Long tenantId, AnalyticsModel.CorrelateRequest request) {
        requireTenant(tenantId);
        SeriesKey a = resolveSeries(tenantId, List.of(Objects.requireNonNull(Objects.requireNonNull(
                request, "request is required").seriesA(), "seriesA is required"))).getFirst();
        SeriesKey b = resolveSeries(tenantId, List.of(Objects.requireNonNull(request.seriesB(),
                "seriesB is required"))).getFirst();
        TimeWindow window = resolveWindow(request.window());
        long alignSeconds = clamp(request.alignBucketSeconds() == null ? 300 : request.alignBucketSeconds(), 10, 86400);
        Duration alignBucket = Duration.ofSeconds(alignSeconds);

        String method;
        double pearson;
        long alignedBuckets;
        if (tsdbStore.capabilities().correlation()) {
            CorrelationResult result = tsdbStore.correlation(a, b, window, alignBucket, DEADLINE);
            pearson = result.pearson();
            alignedBuckets = result.alignedBuckets();
            method = "STORE";
        } else {
            // Facade fallback (§9.7): aligned bucket averages pulled per series,
            // Pearson computed here — M4-level volume, feasible by design.
            Map<Long, Double> bucketsA = bucketAverages(a, window, alignBucket);
            Map<Long, Double> bucketsB = bucketAverages(b, window, alignBucket);
            List<double[]> aligned = bucketsA.entrySet().stream()
                    .filter(entry -> bucketsB.containsKey(entry.getKey()))
                    .map(entry -> new double[]{entry.getValue(), bucketsB.get(entry.getKey())})
                    .toList();
            pearson = pearson(aligned);
            alignedBuckets = aligned.size();
            method = "FACADE";
        }

        String strength = Math.abs(pearson) >= 0.9 ? "very strong" : Math.abs(pearson) >= 0.7 ? "strong"
                : Math.abs(pearson) >= 0.4 ? "moderate" : Math.abs(pearson) >= 0.2 ? "weak" : "negligible";
        String direction = pearson >= 0 ? "positive" : "negative";
        String conclusion = "Pearson r=%.4f (%s %s) over %d aligned %ds buckets".formatted(
                pearson, strength, direction, alignedBuckets, alignSeconds);
        return new AnalyticsModel.CorrelationResponse(conclusion, alignedBuckets,
                "FACADE".equals(method) ? "computed from bucketed pulls (store has no SQL-side correlation)" : null,
                round(pearson), alignedBuckets, method);
    }

    // ===== data_quality_report =====

    @Override
    public AnalyticsModel.QualityResponse qualityReport(Long tenantId, AnalyticsModel.QualityReportRequest request) {
        requireTenant(tenantId);
        TimeWindow window = resolveWindow(request == null ? null : request.window());
        int silentMinutes = clamp(request == null || request.silentMinutes() == null ? 30 : request.silentMinutes(),
                5, 24 * 60);

        List<SeriesLastSeen> seen = tsdbStore.lastSeenPerSeries(tenantId, window, DEADLINE);
        Instant silentSince = Instant.now().minus(Duration.ofMinutes(silentMinutes));
        List<AnalyticsModel.SilentItem> silent = seen.stream()
                .filter(row -> Objects.nonNull(row.lastSeen()) && row.lastSeen().isBefore(silentSince))
                .map(row -> new AnalyticsModel.SilentItem(
                        refOf(tenantId, row.series().deviceId(), row.series().pointId()),
                        row.lastSeen().toString()))
                .toList();

        // Quality-code distribution from one bounded tenant-wide pull — a sample
        // census, not a full scan; the degradation note says so when it truncates.
        CursorPage<PointValueSample> probe = tsdbStore.history(SeriesFilter.tenantWide(tenantId), window, null,
                MAX_SAMPLES_PER_PULL, DEADLINE);
        Map<String, Long> quality = new TreeMap<>(probe.items().stream().collect(Collectors.groupingBy(
                sample -> String.valueOf(sample.quality()), Collectors.counting())));
        String degradation = Objects.nonNull(probe.nextCursor())
                ? "quality distribution sampled from the newest %d samples".formatted(probe.items().size()) : null;

        long totalPoints = countTenantPoints(tenantId);
        double coverage = totalPoints == 0 ? 0 : Math.min(100, round(seen.size() * 100.0 / totalPoints));
        String conclusion = "%d of %d points reported in the window (%.1f%% coverage), %d silent for over %d minutes"
                .formatted(seen.size(), totalPoints, coverage, silent.size(), silentMinutes);
        return new AnalyticsModel.QualityResponse(conclusion, probe.items().size(), degradation,
                seen.size(), totalPoints, coverage, silent, quality);
    }

    // ===== shared plumbing =====

    private static void requireTenant(Long tenantId) {
        if (Objects.isNull(tenantId) || tenantId <= 0) {
            throw new ServiceException("Analytics requires an authenticated tenant context");
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.clamp(value, min, max);
    }

    private static long clamp(Long value, long min, long max) {
        return Objects.isNull(value) ? min : Math.clamp(value, min, max);
    }

    private TimeWindow resolveWindow(AnalyticsModel.TimeRange range) {
        Instant to = Objects.nonNull(range) && Objects.nonNull(range.toIso()) ? Instant.parse(range.toIso())
                : Instant.now();
        Instant from = Objects.nonNull(range) && Objects.nonNull(range.fromIso()) ? Instant.parse(range.fromIso())
                : to.minus(Duration.ofHours(Objects.nonNull(range) && Objects.nonNull(range.rangeHours())
                ? clamp(range.rangeHours(), 1, MAX_WINDOW_HOURS) : 24));
        if (!from.isBefore(to)) {
            throw new ServiceException("window from must be before to");
        }
        if (Duration.between(from, to).toHours() > MAX_WINDOW_HOURS) {
            throw new ServiceException("window span exceeds %d hours; narrow the window or lower the granularity"
                    .formatted(MAX_WINDOW_HOURS));
        }
        return new TimeWindow(from, to);
    }

    private static Duration windowLength(TimeWindow window) {
        return Duration.between(window.from(), window.toExclusive());
    }

    /**
     * Resolve selectors to series keys. Ids win when present; names resolve through
     * the facades and must be unambiguous — ambiguous or unknown names throw with
     * the candidate list so the AI caller can disambiguate on the next turn.
     */
    private List<SeriesKey> resolveSeries(Long tenantId, List<AnalyticsModel.SeriesSelector> selectors) {
        if (CollectionUtils.isEmpty(selectors)) {
            throw new ServiceException("at least one series selector is required");
        }
        if (selectors.size() > MAX_SERIES_PER_CALL) {
            throw new ServiceException("at most %d series per call, got %d".formatted(MAX_SERIES_PER_CALL,
                    selectors.size()));
        }
        List<SeriesKey> series = new ArrayList<>(selectors.size());
        for (AnalyticsModel.SeriesSelector selector : selectors) {
            series.add(resolveOne(tenantId, selector));
        }
        return series;
    }

    private SeriesKey resolveOne(Long tenantId, AnalyticsModel.SeriesSelector selector) {
        Long deviceId = selector.deviceId();
        Long pointId = selector.pointId();
        if (Objects.isNull(deviceId) && Objects.nonNull(selector.deviceName()) && !selector.deviceName().isBlank()) {
            deviceId = uniqueDevice(tenantId, selector.deviceName());
        }
        if (Objects.isNull(pointId) && Objects.nonNull(selector.pointName()) && !selector.pointName().isBlank()) {
            pointId = uniquePoint(tenantId, deviceId, selector.pointName());
        }
        if (Objects.isNull(deviceId) || Objects.isNull(pointId)) {
            throw new ServiceException("series needs deviceId+pointId (ids or unambiguous names): device=%s, point=%s"
                    .formatted(selector.deviceName(), selector.pointName()));
        }
        return new SeriesKey(tenantId, deviceId, pointId);
    }

    private Long uniqueDevice(Long tenantId, String deviceName) {
        FacadeDeviceQuery query = FacadeDeviceQuery.builder().tenantId(tenantId).deviceName(deviceName).build();
        List<FacadeDeviceBO> matches = deviceFacade.listByPage(query).getRecords().stream()
                .filter(device -> deviceName.equals(device.getDeviceName())).toList();
        if (matches.size() != 1) {
            throw new ServiceException("device name '%s' matched %d devices %s — pass deviceId"
                    .formatted(deviceName, matches.size(),
                            matches.stream().map(d -> d.getId() + ":" + d.getDeviceName()).toList()));
        }
        return matches.getFirst().getId();
    }

    private Long uniquePoint(Long tenantId, Long deviceId, String pointName) {
        FacadePointQuery.FacadePointQueryBuilder builder = FacadePointQuery.builder()
                .tenantId(tenantId).pointName(pointName);
        if (Objects.nonNull(deviceId)) {
            builder.deviceId(deviceId);
        }
        List<FacadePointBO> matches = pointFacade.listByPage(builder.build()).getRecords().stream()
                .filter(point -> pointName.equals(point.getPointName())).toList();
        if (matches.size() != 1) {
            throw new ServiceException("point name '%s' matched %d points %s — pass pointId"
                    .formatted(pointName, matches.size(),
                            matches.stream().map(p -> p.getId() + ":" + p.getPointName()).toList()));
        }
        return matches.getFirst().getId();
    }

    /**
     * Bounded per-series sample pull inside the window, newest-first scan pages.
     */
    private Map<SeriesKey, List<PointValueSample>> pullPerSeries(List<SeriesKey> series, TimeWindow window) {
        Map<SeriesKey, List<PointValueSample>> out = new LinkedHashMap<>();
        for (SeriesKey key : series) {
            List<PointValueSample> collected = new ArrayList<>();
            CursorPage<PointValueSample> page = tsdbStore.history(SeriesFilter.of(key), window, null,
                    HISTORY_PAGE, DEADLINE);
            collected.addAll(page.items());
            while (Objects.nonNull(page.nextCursor()) && collected.size() < MAX_SAMPLES_PER_PULL) {
                page = tsdbStore.history(SeriesFilter.of(key), window, page.nextCursor(),
                        Math.min(HISTORY_PAGE, MAX_SAMPLES_PER_PULL - collected.size()), DEADLINE);
                collected.addAll(page.items());
            }
            if (collected.size() > MAX_SAMPLES_PER_PULL) {
                collected = new ArrayList<>(collected.subList(0, MAX_SAMPLES_PER_PULL));
            }
            out.put(key, collected);
        }
        return out;
    }

    private Map<Long, Double> bucketAverages(SeriesKey series, TimeWindow window, Duration bucketWidth) {
        Map<Long, Double> out = new TreeMap<>();
        for (BucketAggregate bucket : tsdbStore.bucketedAggregate(SeriesFilter.of(series), AggregateFunction.AVG,
                window, bucketWidth, null, DEADLINE).getOrDefault(series, List.of())) {
            if (Objects.nonNull(bucket.value())) {
                out.put(bucket.bucketStart().toEpochMilli(), bucket.value());
            }
        }
        return out;
    }

    private AnalyticsModel.SeriesRef refOf(Long tenantId, Long deviceId, Long pointId) {
        String deviceName = null;
        String pointName = null;
        if (Objects.nonNull(deviceId)) {
            FacadeDeviceBO device = deviceFacade.getById(tenantId, deviceId);
            deviceName = Objects.isNull(device) ? null : device.getDeviceName();
        }
        if (Objects.nonNull(pointId)) {
            FacadePointBO point = pointFacade.getById(tenantId, pointId);
            pointName = Objects.isNull(point) ? null : point.getPointName();
        }
        return new AnalyticsModel.SeriesRef(deviceId, pointId,
                (Objects.requireNonNullElse(deviceName, String.valueOf(deviceId))) + "/"
                        + Objects.requireNonNullElse(pointName, String.valueOf(pointId)));
    }

    private String labelOfEntity(Long tenantId, GroupDimension dimension, long entityId) {
        return switch (dimension) {
            case DEVICE -> Objects.nonNull(deviceFacade.getById(tenantId, entityId))
                    ? deviceFacade.getById(tenantId, entityId).getDeviceName() : String.valueOf(entityId);
            case POINT -> Objects.nonNull(pointFacade.getById(tenantId, entityId))
                    ? pointFacade.getById(tenantId, entityId).getPointName() : String.valueOf(entityId);
            case DRIVER -> Objects.nonNull(driverFacade.getById(tenantId, entityId))
                    ? driverFacade.getById(tenantId, entityId).getDriverName() : String.valueOf(entityId);
        };
    }

    private long countTenantPoints(Long tenantId) {
        io.github.pnoker.common.entity.common.Pages pages = new io.github.pnoker.common.entity.common.Pages();
        pages.setCurrent(1);
        pages.setSize(1L);
        FacadePage<FacadePointBO> page = pointFacade.listByPage(
                FacadePointQuery.builder().tenantId(tenantId).page(pages).build());
        return page.getTotal();
    }

    private static Double value(Map<SeriesKey, List<BucketAggregate>> buckets, SeriesKey key, int index) {
        List<BucketAggregate> list = buckets.get(key);
        return Objects.isNull(list) || index >= list.size() ? null : list.get(index).value();
    }

    private static double percentile(List<Double> sorted, double p) {
        if (sorted.isEmpty()) {
            return Double.NaN;
        }
        double position = p * (sorted.size() - 1);
        int lower = (int) Math.floor(position);
        int upper = Math.min(lower + 1, sorted.size() - 1);
        double fraction = position - lower;
        return round(sorted.get(lower) + (sorted.get(upper) - sorted.get(lower)) * fraction);
    }

    private static double leastSquaresSlope(List<Double> values) {
        int n = values.size();
        double meanX = (n - 1) / 2.0;
        double meanY = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double num = 0;
        double den = 0;
        for (int i = 0; i < n; i++) {
            num += (i - meanX) * (values.get(i) - meanY);
            den += (i - meanX) * (i - meanX);
        }
        return den == 0 ? 0 : num / den;
    }

    private static double pearson(List<double[]> pairs) {
        if (pairs.size() < 2) {
            return 0;
        }
        double meanX = pairs.stream().mapToDouble(p -> p[0]).average().orElse(0);
        double meanY = pairs.stream().mapToDouble(p -> p[1]).average().orElse(0);
        double num = 0;
        double dx = 0;
        double dy = 0;
        for (double[] pair : pairs) {
            num += (pair[0] - meanX) * (pair[1] - meanY);
            dx += (pair[0] - meanX) * (pair[0] - meanX);
            dy += (pair[1] - meanY) * (pair[1] - meanY);
        }
        return dx == 0 || dy == 0 ? 0 : num / Math.sqrt(dx * dy);
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private static Double round(Double value) {
        return Objects.isNull(value) ? null : round(value.doubleValue());
    }

    private static String label(AnalyticsModel.SeriesRef ref) {
        return ref.label();
    }

    private static GroupDimension dimensionOf(String dimension) {
        if (Objects.isNull(dimension)) {
            return GroupDimension.DEVICE;
        }
        try {
            return GroupDimension.valueOf(dimension.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ServiceException("Unsupported dimension: " + dimension + " (expect DEVICE / POINT / DRIVER)");
        }
    }
}
