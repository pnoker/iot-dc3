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
package io.github.pnoker.common.data.entity.vo.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/**
 * Request/response shapes of the AI analytics facet (docs/design/tsdb-abstraction.md
 * §9.7). Deliberately coarse-grained: an LLM picks a tool and fills typed fields,
 * and every response leads with a self-contained human-readable {@code conclusion}
 * plus the sample count it rests on — never a bare sample page. {@code degradation}
 * is non-null when the result was computed from a bounded subset or a store-side
 * shortcut was unavailable, so callers can state their confidence.
 *
 * @author pnoker
 * @since 2026.8.21
 */
public final class AnalyticsModel {

    private AnalyticsModel() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * One series addressed either by ids or by names — ids win when both are
     * present; ambiguous or unknown names surface the candidates in the error.
     */
    public record SeriesSelector(
            @Schema(description = "Device id; takes precedence over deviceName when both are given", example = "1024")
            Long deviceId,

            @Schema(description = "Point id; takes precedence over pointName when both are given", example = "2048")
            Long pointId,

            @Schema(
                    description = "Exact device name; must resolve to exactly one device of the tenant",
                    example = "boiler-1")
            String deviceName,

            @Schema(
                    description =
                            "Exact point name; must resolve to exactly one point (scoped to the device when deviceId is given)",
                    example = "temp")
            String pointName) {}

    /**
     * Window as an ISO-8601 instant pair, or {@code rangeHours} back from now
     * when the pair is absent. An empty range defaults to the last 24 hours.
     */
    public record TimeRange(
            @Schema(description = "Inclusive window start, ISO-8601 instant", example = "2026-08-20T00:00:00Z")
            String fromIso,

            @Schema(
                    description = "Exclusive window end, ISO-8601 instant; defaults to now",
                    example = "2026-08-21T00:00:00Z")
            String toIso,

            @Schema(
                    description =
                            "Relative window length in hours back from now (1..2160); ignored when fromIso is given",
                    example = "24")
            Long rangeHours) {}

    // ===== requests =====

    /**
     * Body of {@code POST /analytics/query_latest}: current values of one or more series.
     */
    public record QueryLatestRequest(
            @Schema(description = "Series to read; at most 20 per call")
            List<SeriesSelector> series) {}

    /**
     * Body of {@code POST /analytics/query_history}: RAW mode returns the newest samples (bounded);
     * M4 mode returns per-bucket first/min/max/last for chart-grade rendering.
     */
    public record QueryHistoryRequest(
            @Schema(description = "Series to read; at most 20 per call")
            List<SeriesSelector> series,

            @Schema(description = "Time window; defaults to the last 24 hours")
            TimeRange window,

            @Schema(
                    description =
                            "Rendering mode: RAW returns newest samples, M4 returns per-bucket first/min/max/last",
                    example = "M4",
                    allowableValues = {"RAW", "M4"})
            String mode,

            @Schema(
                    description =
                            "Target point count per series (1..1000); RAW caps the total pull, M4 sizes the buckets",
                    example = "200")
            Integer maxPoints) {}

    /**
     * Body of {@code POST /analytics/compute_stats}: statistical profile request with optional
     * percentiles (defaults to 0.5 and 0.95).
     */
    public record ComputeStatsRequest(
            @Schema(description = "Series to profile; at most 20 per call")
            List<SeriesSelector> series,

            @Schema(description = "Time window; defaults to the last 24 hours")
            TimeRange window,

            @Schema(
                    description = "Percentiles in [0,1] to compute per series; defaults to 0.5 and 0.95",
                    example = "[0.5, 0.99]")
            List<Double> percentiles) {}

    /**
     * Body of {@code POST /analytics/compare_periods}: same series across two windows.
     */
    public record ComparePeriodsRequest(
            @Schema(description = "Series to compare; at most 20 per call")
            List<SeriesSelector> series,

            @Schema(description = "Current window (e.g. this week)")
            TimeRange current,

            @Schema(description = "Baseline window (e.g. last week)")
            TimeRange baseline) {}

    /**
     * Body of {@code POST /analytics/rank_entities}: ranking by ACTIVITY (sample count) or by
     * MEAN / MAX / MIN over each entity's series.
     */
    public record RankEntitiesRequest(
            @Schema(
                    description = "Grouping dimension of the ranking",
                    example = "DEVICE",
                    allowableValues = {"DEVICE", "POINT", "DRIVER"})
            String dimension,

            @Schema(
                    description =
                            "Ranking metric: ACTIVITY (sample count) or MEAN / MAX / MIN over each entity's series",
                    example = "ACTIVITY",
                    allowableValues = {"ACTIVITY", "MEAN", "MAX", "MIN"})
            String metric,

            @Schema(description = "Time window; defaults to the last 24 hours")
            TimeRange window,

            @Schema(description = "Top-N size (1..50); defaults to 10", example = "10")
            Integer limit) {}

    /**
     * Body of {@code POST /analytics/trend_analysis}: per-series least-squares trend over bucket
     * averages; {@code buckets} clamped to [2, 200].
     */
    public record TrendAnalysisRequest(
            @Schema(description = "Series to analyze; at most 20 per call")
            List<SeriesSelector> series,

            @Schema(description = "Time window; defaults to the last 24 hours")
            TimeRange window,

            @Schema(description = "Bucket count for the regression (2..200); defaults to 50", example = "50")
            Integer buckets) {}

    /**
     * Body of {@code POST /analytics/threshold_report}: exceedance report against a threshold
     * with a GREATER / LESS operator.
     */
    public record ThresholdReportRequest(
            @Schema(description = "Series to report; at most 20 per call")
            List<SeriesSelector> series,

            @Schema(description = "Time window; defaults to the last 24 hours")
            TimeRange window,

            @Schema(
                    description = "Comparison operator against the threshold",
                    example = "GREATER",
                    allowableValues = {"GREATER", "LESS"})
            String operator,

            @Schema(description = "Threshold value samples are compared against", example = "80.0")
            Double threshold) {}

    /**
     * Body of {@code POST /analytics/correlate}: Pearson correlation between two series over
     * aligned buckets (default 300 s).
     */
    public record CorrelateRequest(
            @Schema(description = "First series of the pair")
            SeriesSelector seriesA,

            @Schema(description = "Second series of the pair")
            SeriesSelector seriesB,

            @Schema(description = "Time window; defaults to the last 24 hours")
            TimeRange window,

            @Schema(
                    description = "Bucket length in seconds for alignment (10..86400); defaults to 300",
                    example = "300")
            Long alignBucketSeconds) {}

    /**
     * Body of {@code POST /analytics/data_quality_report}: tenant-level coverage, silence and
     * quality-code census.
     */
    public record QualityReportRequest(
            @Schema(description = "Time window; defaults to the last 24 hours")
            TimeRange window,

            @Schema(
                    description =
                            "Minutes of inactivity after which a series counts as silent (5..1440); defaults to 30",
                    example = "30")
            Integer silentMinutes) {}

    // ===== response atoms =====

    /**
     * Response atom: the series identity plus a human-readable deviceName/pointName label.
     */
    public record SeriesRef(
            @Schema(description = "Device id of the series") Long deviceId,
            @Schema(description = "Point id of the series") Long pointId,

            @Schema(description = "Human-readable label deviceName/pointName")
            String label) {}

    // ===== responses =====

    /**
     * Response of {@code query_latest}: conclusion, degradation note and one item per series.
     */
    public record LatestValuesResponse(
            @Schema(description = "Self-contained summary of the latest values")
            String conclusion,

            @Schema(description = "Number of value rows behind the conclusion")
            long sampleCount,

            @Schema(description = "Present when the result is computed from a bounded subset")
            String degradation,

            @Schema(description = "Latest value per requested series")
            List<LatestItem> values) {}

    /**
     * One series' current value; {@code hasValue} is false when no reading exists yet.
     */
    public record LatestItem(
            @Schema(description = "Series the value belongs to")
            SeriesRef series,

            @Schema(description = "Raw value as captured from the device")
            String rawValue,

            @Schema(description = "Calculated/transformed value")
            String calValue,

            @Schema(description = "Numeric projection, null for non-numeric values")
            Double numericValue,

            @Schema(description = "NUMERIC when the value parses as a double, STRING otherwise")
            String valueType,

            @Schema(description = "Device acquisition time") String createTime,

            @Schema(description = "False when the series has no reading yet")
            boolean hasValue) {}

    /**
     * Response of {@code query_history}: points per series keyed by deviceName/pointName.
     */
    public record HistoryResponse(
            @Schema(description = "Self-contained summary of the returned history")
            String conclusion,

            @Schema(description = "Points behind the conclusion")
            long sampleCount,

            @Schema(description = "Present when the pull was capped")
            String degradation,

            @Schema(description = "Points per series, keyed by deviceName/pointName")
            Map<String, List<HistoryPoint>> series) {}

    /**
     * One history point — RAW carries {@code value}; M4 carries the first/min/max/last quadruple.
     */
    public record HistoryPoint(
            @Schema(description = "Sample time / bucket start, ISO-8601 instant")
            String time,

            @Schema(description = "Numeric value in RAW mode")
            Double value,

            @Schema(description = "First value of the bucket in M4 mode")
            Double first,

            @Schema(description = "Minimum of the bucket in M4 mode")
            Double min,

            @Schema(description = "Maximum of the bucket in M4 mode")
            Double max,

            @Schema(description = "Last value of the bucket in M4 mode")
            Double last) {}

    /**
     * Response of {@code compute_stats}: per-series statistical profile.
     */
    public record StatsResponse(
            @Schema(description = "Self-contained statistical summary")
            String conclusion,

            @Schema(description = "Samples behind the statistics")
            long sampleCount,

            @Schema(description = "Present when stats rest on a bounded subset")
            String degradation,

            @Schema(description = "Profile per series, keyed by deviceName/pointName")
            Map<String, StatItem> stats) {}

    /**
     * One series' profile: mean, population std-dev, extremes, count and requested percentiles.
     */
    public record StatItem(
            @Schema(description = "Series the profile belongs to")
            SeriesRef series,

            @Schema(description = "Arithmetic mean of numeric values")
            Double mean,

            @Schema(description = "Population standard deviation of numeric values")
            Double stdDev,

            @Schema(description = "Minimum numeric value") Double min,
            @Schema(description = "Maximum numeric value") Double max,

            @Schema(description = "Sample count inside the window")
            long count,

            @Schema(description = "Requested percentiles, keyed by the percentile in [0,1]")
            Map<Double, Double> percentiles) {}

    /**
     * Response of {@code compare_periods}: per-series current vs baseline averages.
     */
    public record CompareResponse(
            @Schema(description = "Self-contained comparison summary")
            String conclusion,

            @Schema(description = "Samples behind the comparison")
            long sampleCount,

            @Schema(description = "Present when a window had no usable samples")
            String degradation,

            @Schema(description = "Comparison per series, keyed by deviceName/pointName")
            Map<String, CompareItem> comparisons) {}

    /**
     * One series' comparison: current, baseline, delta and percentage change (null when the
     * baseline is zero).
     */
    public record CompareItem(
            @Schema(description = "Series the comparison belongs to")
            SeriesRef series,

            @Schema(description = "Current window average") Double current,
            @Schema(description = "Baseline window average") Double baseline,
            @Schema(description = "current − baseline") Double delta,

            @Schema(description = "Percentage change versus |baseline|; null when baseline is zero")
            Double pctChange,

            @Schema(description = "Sample count in the current window")
            long currentCount,

            @Schema(description = "Sample count in the baseline window")
            long baselineCount) {}

    /**
     * Response of {@code rank_entities}: ranked entities, best first.
     */
    public record RankResponse(
            @Schema(description = "Self-contained ranking summary")
            String conclusion,

            @Schema(description = "Samples behind the ranking")
            long sampleCount,

            @Schema(description = "Present when the ranking is bounded")
            String degradation,

            @Schema(description = "Ranked entities, best first")
            List<RankItem> ranked) {}

    /**
     * One ranked entity — {@code metricValue} is set for MEAN/MAX/MIN rankings and null for ACTIVITY.
     */
    public record RankItem(
            @Schema(description = "Entity id of the ranked entry")
            String entityId,

            @Schema(description = "Entity display name") String label,

            @Schema(description = "Sample count in the window")
            long count,

            @Schema(description = "Metric value for MEAN/MAX/MIN rankings; null for ACTIVITY")
            Double metricValue) {}

    /**
     * Response of {@code trend_analysis}: per-series trend verdicts.
     */
    public record TrendResponse(
            @Schema(description = "Self-contained trend summary")
            String conclusion,

            @Schema(description = "Buckets behind the trends")
            long sampleCount,

            @Schema(description = "Present when a trend rests on few buckets")
            String degradation,

            @Schema(description = "Trend per series, keyed by deviceName/pointName")
            Map<String, TrendItem> trends) {}

    /**
     * One series' trend: least-squares slope in value-per-bucket plus total percentage change.
     */
    public record TrendItem(
            @Schema(description = "Series the trend belongs to")
            SeriesRef series,

            @Schema(description = "Least-squares slope of bucket averages, in value per bucket")
            double slopePerBucket,

            @Schema(description = "First bucket average") double firstValue,
            @Schema(description = "Last bucket average") double lastValue,

            @Schema(description = "Percentage change from first to last bucket; 0 when the first bucket is zero")
            double totalChangePct,

            @Schema(description = "Buckets with data in the window")
            int bucketCount) {}

    /**
     * Response of {@code threshold_report}: per-series exceedance report.
     */
    public record ThresholdResponse(
            @Schema(description = "Self-contained threshold summary")
            String conclusion,

            @Schema(description = "Exceeding samples behind the report")
            long sampleCount,

            @Schema(description = "Present when intervals were capped or the pull bounded")
            String degradation,

            @Schema(description = "Report per series, keyed by deviceName/pointName")
            Map<String, ThresholdItem> report) {}

    /**
     * One series' exceedance summary: count, total seconds, peak value and merged intervals.
     */
    public record ThresholdItem(
            @Schema(description = "Series the report belongs to")
            SeriesRef series,

            @Schema(description = "Samples beyond the threshold")
            long exceedCount,

            @Schema(description = "Total seconds inside exceedance intervals")
            long totalSeconds,

            @Schema(description = "Most extreme value observed (max for GREATER, min for LESS)")
            Double peak,

            @Schema(description = "Merged exceedance intervals, at most 20 per series")
            List<ThresholdInterval> intervals) {}

    /**
     * One merged exceedance interval [from, to], ISO-8601 instants.
     */
    public record ThresholdInterval(
            @Schema(description = "Interval start, ISO-8601 instant")
            String from,

            @Schema(description = "Interval end, ISO-8601 instant")
            String to) {}

    /**
     * Response of {@code correlate}: Pearson coefficient, aligned-bucket count and the method
     * actually used (STORE vs FACADE).
     */
    public record CorrelationResponse(
            @Schema(description = "Self-contained correlation verdict")
            String conclusion,

            @Schema(description = "Aligned buckets behind the coefficient")
            long sampleCount,

            @Schema(description = "Present when computed from bucketed pulls instead of the store")
            String degradation,

            @Schema(description = "Pearson coefficient in [-1, 1]")
            double pearson,

            @Schema(description = "Number of aligned buckets")
            long alignedBuckets,

            @Schema(description = "STORE (SQL-side) or FACADE (bucketed pulls)")
            String method) {}

    /**
     * Response of {@code data_quality_report}: coverage, silent series and sampled quality-code
     * distribution.
     */
    public record QualityResponse(
            @Schema(description = "Self-contained quality summary")
            String conclusion,

            @Schema(description = "Samples in the quality census")
            long sampleCount,

            @Schema(description = "Present when the census was sampled")
            String degradation,

            @Schema(description = "Series with samples in the window")
            long seriesWithSamples,

            @Schema(description = "Total points of the tenant")
            long totalPoints,

            @Schema(description = "Reporting points as a share of all points, in percent")
            double coveragePct,

            @Schema(description = "Series silent beyond the configured threshold")
            List<SilentItem> silentSeries,

            @Schema(description = "Quality-code distribution of the sampled census, keyed by code")
            Map<String, Long> qualityDistribution) {}

    /**
     * One series that went silent, with its last sample time.
     */
    public record SilentItem(
            @Schema(description = "The silent series") SeriesRef series,

            @Schema(description = "Last sample time of the series, ISO-8601 instant")
            String lastSeen) {}
}
