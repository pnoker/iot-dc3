/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * ~
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * ~
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.tsdb.spi;

import io.github.pnoker.common.tsdb.model.TsdbModel.AggregateFunction;
import io.github.pnoker.common.tsdb.model.TsdbModel.BucketAggregate;
import io.github.pnoker.common.tsdb.model.TsdbModel.CorrelationResult;
import io.github.pnoker.common.tsdb.model.TsdbModel.Cursor;
import io.github.pnoker.common.tsdb.model.TsdbModel.CursorPage;
import io.github.pnoker.common.tsdb.model.TsdbModel.DimensionCount;
import io.github.pnoker.common.tsdb.model.TsdbModel.GroupDimension;
import io.github.pnoker.common.tsdb.model.TsdbModel.LatencyBin;
import io.github.pnoker.common.tsdb.model.TsdbModel.PointValueSample;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesCount;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesFilter;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesKey;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesLastSeen;
import io.github.pnoker.common.tsdb.model.TsdbModel.TimeWindow;
import io.github.pnoker.common.tsdb.model.TsdbModel.TsdbDeadline;
import io.github.pnoker.common.tsdb.model.TsdbModel.WindowAggregate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

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

    String type();

    TsdbCapabilities capabilities();

    // ===== 写入 =====

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
    int append(List<PointValueSample> samples);

    // ===== 读取（统一过滤器：单序列 / 多序列 / 全租户） =====

    /**
     * S4/S14: per series the newest {@code limit} samples, newest first. Tenant-wide
     * filters are supported only when {@code capabilities().tenantWideScan()}.
     *
     * @return samples grouped by series
     */
    Map<SeriesKey, List<PointValueSample>> last(SeriesFilter filter, int limit, TsdbDeadline deadline);

    /**
     * S5/S14: one descending cursor page over the filter's series inside the window;
     * {@code cursor == null} starts from the newest. Cursor is the global
     * (deviceTime, messageId) tuple across the whole series set.
     */
    CursorPage<PointValueSample> history(SeriesFilter filter, TimeWindow window,
                                         Cursor cursor, int pageSize, TsdbDeadline deadline);

    /**
     * S6/S15: single-window aggregate per series (NULL-skipping for AVG/MIN/MAX/SUM;
     * COUNT counts every row). {@code percentile} is the p in [0,1] for
     * {@code AggregateFunction.PERCENTILE}, null otherwise.
     *
     * @return aggregate grouped by series
     */
    Map<SeriesKey, WindowAggregate> aggregate(SeriesFilter filter, AggregateFunction fn,
                                              TimeWindow window, Double percentile, TsdbDeadline deadline);

    /**
     * S7/S15/S16: per-bucket aggregates over the window, buckets ascending, per series.
     * Empty buckets are zero-filled when {@code capabilities().gapFill()} else omitted.
     * Rollup-transparent: adapters serve from the coarsest materialized tier whose
     * width satisfies {@code bucketWidth} when {@code rollupSupport} is not NONE.
     */
    Map<SeriesKey, List<BucketAggregate>> bucketedAggregate(SeriesFilter filter,
                                                            AggregateFunction fn, TimeWindow window,
                                                            Duration bucketWidth, Double percentile,
                                                            TsdbDeadline deadline);

    /** S8: sample count inside the window for the filter (all three scopes). */
    long count(SeriesFilter filter, TimeWindow window, TsdbDeadline deadline);

    // ===== S13：租户级分析面（tenantWideAnalytics 能力门控） =====

    /** S13-①: tenant-wide time-bucketed COUNT, single stream, buckets ascending. */
    List<BucketAggregate> bucketedCount(long tenantId, TimeWindow window,
                                        Duration bucketWidth, TsdbDeadline deadline);

    /** S13-②: tenant-wide grouped counts, descending, top {@code limit}. */
    List<DimensionCount> countByDimension(long tenantId, TimeWindow window,
                                          GroupDimension dimension, int limit, TsdbDeadline deadline);

    /**
     * S13-⑤: per-series counts (grouped by tenant, device, point) inside the
     * window — the exact grain the manager topology volume view needs.
     */
    List<SeriesCount> seriesCounts(long tenantId, TimeWindow window, TsdbDeadline deadline);

    /** S13-③: every series with samples in the window plus its newest sample time. */
    List<SeriesLastSeen> lastSeenPerSeries(long tenantId, TimeWindow window, TsdbDeadline deadline);

    /**
     * S13-④: receive-latency histogram over {@code receiveTime − deviceTime}
     * milliseconds using the caller's bin edges (capability {@code latencyHistogram}).
     */
    List<LatencyBin> latencyHistogram(long tenantId, TimeWindow window,
                                      List<Long> binEdgesMs, TsdbDeadline deadline);

    // ===== 运维 =====

    /** S18: series with samples in the window (migration CLI, coverage audits). */
    List<SeriesKey> listSeries(long tenantId, TimeWindow window, TsdbDeadline deadline);

    /** S10: capability-gated time-range delete (tenant offboarding). */
    void deleteRange(SeriesKey series, TimeWindow window);

    /** S19: aligned-bucket Pearson correlation between two series
     *  (capability {@code correlation}); facades without store support compute from
     *  bucketed pulls themselves. */
    CorrelationResult correlation(SeriesKey a, SeriesKey b, TimeWindow window,
                                  Duration alignBucket, TsdbDeadline deadline);

    /**
     * Adapter capability declaration (§8 of the design). The startup negotiation log
     * prints this row, mirroring the MQ port.
     *
     * @param gapFill               zero-fill empty buckets
     * @param tenantWideScan        series-empty history/aggregate/count/last
     * @param tenantWideAnalytics   S13 facet
     * @param latencyHistogram      S13-④ store-side
     * @param percentile            S15 PERCENTILE
     * @param rollupSupport         S16 tiered-rollup mode
     * @param maxAppendBatch        S18 chunking threshold
     * @param deleteRange           S10
     * @param ordering              NONE | PER_SERIES
     * @param precision             native timestamp precision
     * @param backfill              out-of-order/late writes accepted
     * @param correlation           S19 store-side correlation
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
            boolean correlation) {
    }

    enum RollupSupport {NATIVE, MANUAL, NONE}

    enum OrderingGuarantee {NONE, PER_SERIES}

    enum Precision {MICRO, MILLI, NANO}
}
