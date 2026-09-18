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
package io.github.pnoker.common.tsdb.spi;

import io.github.pnoker.common.tsdb.model.TsdbModel.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * S19-final store SPI (docs/design/tsdb-abstraction.md §6). One implementation per
 * store behind {@code dc3.tsdb.type}; the write orchestration (schema validation,
 * ingest idempotency window, {@code dc3_point_latest} relational upsert) stays in the
 * data center — this is the storage boundary only.
 *
 * @author pnoker
 * @since 2026.8.20
 */
public interface TsdbStore {

    /**
     * Port-level entry validation of the {@code percentile} argument of
     * {@link #aggregate} and {@link #bucketedAggregate}: PERCENTILE requires a
     * finite p in [0, 1] (adapters inline p into store SQL and must never see
     * NaN or infinities); every other function requires null. Adapters call
     * this first so the guarantee holds regardless of the caller.
     *
     * @param fn         aggregate function of the call
     * @param percentile the p in [0, 1] for PERCENTILE, null otherwise
     * @return the validated percentile, null unless {@code fn} is PERCENTILE
     * @throws IllegalArgumentException when the contract above is violated
     */
    static Double validatePercentile(AggregateFunction fn, Double percentile) {
        if (Objects.isNull(percentile)) {
            if (fn == AggregateFunction.PERCENTILE) {
                throw new IllegalArgumentException("percentile p in [0,1] is required for " + fn);
            }
            return null;
        }
        if (fn != AggregateFunction.PERCENTILE) {
            throw new IllegalArgumentException("percentile must be null for " + fn);
        }
        if (percentile.isNaN() || Double.isInfinite(percentile) || percentile < 0d || percentile > 1d) {
            throw new IllegalArgumentException("percentile p must be a finite value in [0,1], got: " + percentile);
        }
        return percentile;
    }

    /**
     * Store identifier matching the {@code dc3.tsdb.type} selection value.
     *
     * @return the selection value this adapter binds to
     */
    String type();

    /**
     * Adapter capability declaration (§8 of the design); printed by the startup negotiation log.
     *
     * @return the declared capability set
     */
    TsdbCapabilities capabilities();

    // ===== writes =====

    /**
     * S2/S3: batch append; store-level upsert on (series, deviceTime) with the
     * adapter's declared duplicate policy (last-write-wins unless stated otherwise).
     * Idempotent for the same batch; whole-batch success or whole-batch failure —
     * no partial acceptance. Batches larger than {@code maxAppendBatch} are chunked
     * by the port facade before reaching here.
     *
     * @param samples samples to append, never empty
     * @return accepted sample count (best-effort per store)
     */
    Mono<Integer> append(List<PointValueSample> samples);

    // ===== reads (unified filter: single series / multi series / tenant-wide) =====

    /**
     * S4/S14: per series the newest {@code limit} samples, newest first. Tenant-wide
     * filters are supported only when {@code capabilities().tenantWideScan()}.
     *
     * @param filter   series selection
     * @param limit    samples per series, newest first
     * @param deadline read deadline
     * @return samples grouped by series
     */
    Mono<Map<SeriesKey, List<PointValueSample>>> last(SeriesFilter filter, int limit, TsdbDeadline deadline);

    /**
     * S5/S14: one descending cursor page over the filter's series inside the window;
     * {@code cursor == null} starts from the newest. Cursor is the global
     * (deviceTime, series, messageId) tuple across the whole series set.
     *
     * @param filter   series selection
     * @param window   half-open time window
     * @param cursor   page anchor, null starts from the newest
     * @param pageSize page size
     * @param deadline read deadline
     * @return one descending page
     */
    Mono<CursorPage<PointValueSample>> history(
            SeriesFilter filter, TimeWindow window, Cursor cursor, int pageSize, TsdbDeadline deadline);

    /**
     * S6/S15: single-window aggregate per series (NULL-skipping for AVG/MIN/MAX/SUM;
     * COUNT counts every row). {@code percentile} is the p in [0,1] for
     * {@code AggregateFunction.PERCENTILE}, null otherwise.
     *
     * @param filter     series selection
     * @param fn         aggregate function
     * @param window     half-open time window
     * @param percentile the p in [0,1] for PERCENTILE, null otherwise
     * @param deadline   read deadline
     * @return aggregate grouped by series
     */
    Mono<Map<SeriesKey, WindowAggregate>> aggregate(
            SeriesFilter filter, AggregateFunction fn, TimeWindow window, Double percentile, TsdbDeadline deadline);

    /**
     * S7/S15/S16: per-bucket aggregates over the window, buckets ascending, per series.
     * Empty buckets are zero-filled when {@code capabilities().gapFill()} else omitted.
     * Rollup-transparent: adapters serve from the coarsest materialized tier whose
     * width satisfies {@code bucketWidth} when {@code rollupSupport} is not NONE.
     *
     * @param filter      series selection
     * @param fn          aggregate function
     * @param window      half-open time window
     * @param bucketWidth bucket width
     * @param percentile  the p in [0,1] for PERCENTILE, null otherwise
     * @param deadline    read deadline
     * @return per-bucket aggregates, buckets ascending, per series
     */
    Mono<Map<SeriesKey, List<BucketAggregate>>> bucketedAggregate(
            SeriesFilter filter,
            AggregateFunction fn,
            TimeWindow window,
            Duration bucketWidth,
            Double percentile,
            TsdbDeadline deadline);

    /**
     * S8: sample count inside the window for the filter (all three scopes).
     *
     * @param filter   series selection
     * @param window   half-open time window
     * @param deadline read deadline
     * @return sample count inside the window
     */
    Mono<Long> count(SeriesFilter filter, TimeWindow window, TsdbDeadline deadline);

    // ===== S13: tenant-level analytics (gated by the tenantWideAnalytics capability) =====

    /**
     * S13-①: tenant-wide time-bucketed COUNT, single stream, buckets ascending.
     *
     * @param tenantId    tenant scope
     * @param window      half-open time window
     * @param bucketWidth bucket width
     * @param deadline    read deadline
     * @return time-bucketed counts, buckets ascending
     */
    Mono<List<BucketAggregate>> bucketedCount(
            long tenantId, TimeWindow window, Duration bucketWidth, TsdbDeadline deadline);

    /**
     * S13-②: tenant-wide grouped counts, descending, top {@code limit}.
     *
     * @param tenantId  tenant scope
     * @param window    half-open time window
     * @param dimension grouping dimension
     * @param limit     top-N
     * @param deadline  read deadline
     * @return grouped counts, descending
     */
    Mono<List<DimensionCount>> countByDimension(
            long tenantId, TimeWindow window, GroupDimension dimension, int limit, TsdbDeadline deadline);

    /**
     * S13-⑤: per-series counts (grouped by tenant, device, point) inside the
     * window — the exact grain the manager topology volume view needs.
     *
     * @param tenantId tenant scope
     * @param window   half-open time window
     * @param deadline read deadline
     * @return per-series counts
     */
    Flux<SeriesCount> seriesCounts(long tenantId, TimeWindow window, TsdbDeadline deadline);

    /**
     * S13-③: every series with samples in the window plus its newest sample time.
     *
     * @param tenantId tenant scope
     * @param window   half-open time window
     * @param deadline read deadline
     * @return one row per series with samples in the window
     */
    Mono<List<SeriesLastSeen>> lastSeenPerSeries(long tenantId, TimeWindow window, TsdbDeadline deadline);

    /**
     * S13-④: receive-latency histogram over {@code receiveTime − deviceTime}
     * milliseconds using the caller's bin edges (capability {@code latencyHistogram}).
     *
     * @param tenantId   tenant scope
     * @param window     half-open time window
     * @param binEdgesMs caller-supplied bin edges in milliseconds
     * @param deadline   read deadline
     * @return latency histogram bins
     */
    Mono<List<LatencyBin>> latencyHistogram(
            long tenantId, TimeWindow window, List<Long> binEdgesMs, TsdbDeadline deadline);

    // ===== operations =====

    /**
     * S18: series with samples in the window (migration CLI, coverage audits).
     *
     * @param tenantId tenant scope
     * @param window   half-open time window
     * @param deadline read deadline
     * @return series with samples in the window
     */
    Mono<List<SeriesKey>> listSeries(long tenantId, TimeWindow window, TsdbDeadline deadline);

    /**
     * S10: capability-gated time-range delete (tenant offboarding).
     *
     * @param series target series
     * @param window half-open time window
     */
    Mono<Void> deleteRange(SeriesKey series, TimeWindow window);

    /**
     * S19: aligned-bucket Pearson correlation between two series
     * (capability {@code correlation}); facades without store support compute from
     * bucketed pulls themselves.
     *
     * @param a           first series
     * @param b           second series
     * @param window      half-open time window
     * @param alignBucket alignment bucket width
     * @param deadline    read deadline
     * @return aligned-bucket Pearson correlation
     */
    Mono<CorrelationResult> correlation(
            SeriesKey a, SeriesKey b, TimeWindow window, Duration alignBucket, TsdbDeadline deadline);

    /**
     * S16 tiered-rollup support levels.
     */
    enum RollupSupport {
        /**
         * Store-side rollup tiers.
         */
        NATIVE,
        /**
         * Rollup maintained by the platform on top of the store.
         */
        MANUAL,
        /**
         * No rollup support.
         */
        NONE
    }

    /**
     * S2/S8 result ordering guarantees.
     */
    enum OrderingGuarantee {
        /**
         * No ordering guarantee.
         */
        NONE,
        /**
         * Samples ordered within each series.
         */
        PER_SERIES
    }

    /**
     * Native timestamp precision of the store.
     */
    enum Precision {
        /**
         * Microsecond precision.
         */
        MICRO,
        /**
         * Millisecond precision.
         */
        MILLI,
        /**
         * Nanosecond precision.
         */
        NANO
    }

    /**
     * Adapter capability declaration (§8 of the design). The startup negotiation log
     * prints this row, mirroring the MQ port.
     *
     * @param gapFill             zero-fill empty buckets
     * @param tenantWideScan      series-empty history/aggregate/count/last
     * @param tenantWideAnalytics S13 facet
     * @param latencyHistogram    S13-④ store-side
     * @param percentile          S15 PERCENTILE
     * @param rollupSupport       S16 tiered-rollup mode
     * @param maxAppendBatch      S18 chunking threshold
     * @param deleteRange         S10
     * @param ordering            NONE | PER_SERIES
     * @param precision           native timestamp precision
     * @param backfill            out-of-order/late writes accepted
     * @param correlation         S19 store-side correlation
     */
    record TsdbCapabilities(
            boolean gapFill,
            boolean tenantWideScan,
            boolean tenantWideAnalytics,
            boolean latencyHistogram,
            boolean percentile,
            RollupSupport rollupSupport,
            int maxAppendBatch,
            boolean deleteRange,
            OrderingGuarantee ordering,
            Precision precision,
            boolean backfill,
            boolean correlation) {}
}
