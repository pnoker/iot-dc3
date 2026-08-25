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

package io.github.pnoker.common.tsdb.model;

import java.time.Instant;
import java.util.List;

/**
 * Time-series domain model of the port (S1–S19 of docs/design/tsdb-abstraction.md).
 * Pure Java, zero store dependencies; timestamps are epoch-micro {@link Instant}s.
 *
 * @author pnoker
 * @since 2026.8.20
 */
public final class TsdbModel {

    private TsdbModel() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * S1: series identity — platform numeric IDs; names enriched at the app layer.
     *
     * @param tenantId tenant scope of the series
     * @param deviceId device owning the point
     * @param pointId  point of the series
     */
    public record SeriesKey(long tenantId, long deviceId, long pointId) {
    }

    /**
     * Unified read filter (S14): a non-empty series list selects exactly those series;
     * an empty list is a tenant-wide scan (capability {@code tenantWideScan}). The
     * tenant id is always present — tenant isolation is a hard constraint (S11).
     *
     * @param tenantId tenant scope of the filter
     * @param series   selected series; empty means tenant-wide
     */
    public record SeriesFilter(long tenantId, List<SeriesKey> series) {

        /**
         * Filter for exactly one series; the tenant id comes from that series.
         *
         * @param single the only series to select
         * @return a single-series filter
         */
        public static SeriesFilter of(SeriesKey single) {
            return new SeriesFilter(single.tenantId(), List.of(single));
        }

        /**
         * Filter for several series; the tenant id is taken from the first series,
         * so callers must supply same-tenant series only.
         *
         * @param series non-empty, same-tenant series to select
         * @return a multi-series filter
         */
        public static SeriesFilter of(List<SeriesKey> series) {
            if (series == null || series.isEmpty()) {
                throw new IllegalArgumentException("series filter needs at least one series");
            }
            return new SeriesFilter(series.get(0).tenantId(), List.copyOf(series));
        }

        /**
         * Tenant-wide scan filter; requires the {@code tenantWideScan} capability.
         *
         * @param tenantId tenant to scan
         * @return a tenant-wide filter
         */
        public static SeriesFilter tenantWide(long tenantId) {
            return new SeriesFilter(tenantId, List.of());
        }

        /**
         * Whether this filter is a tenant-wide scan instead of an explicit selection.
         *
         * @return true when this filter selects the whole tenant instead of explicit series
         */
        public boolean tenantWide() {
            return series.isEmpty();
        }
    }

    /**
     * S2: one stored sample. {@code deviceTime} is the device acquisition time
     * (create_time), {@code receiveTime} the server receive time (operate_time, S9);
     * {@code numericValue} is the numeric projection of {@code calValue} (null for
     * non-numeric payloads); {@code quality} is the S17 OPC-UA-style quality code
     * (0 = GOOD).
     *
     * @param series        series identity
     * @param deviceTime    device acquisition time
     * @param receiveTime   server receive time
     * @param rawValue      raw payload as received
     * @param calValue      calibrated/converted value
     * @param numericValue  numeric projection of calValue, null for non-numeric payloads
     * @param quality       S17 quality code, 0 = GOOD
     * @param messageId     immutable message identity for idempotent ingest
     * @param schemaVersion wire schema version
     * @param driverNode    driver runtime node that produced the sample
     * @param sequence      per-node sequence number
     * @param fencingToken  ownership fencing token of the producing node
     * @param driverId      logical driver id
     */
    public record PointValueSample(
            SeriesKey series,
            Instant deviceTime,
            Instant receiveTime,
            String rawValue,
            String calValue,
            Double numericValue,
            int quality,
            String messageId,
            int schemaVersion,
            String driverNode,
            long sequence,
            long fencingToken,
            long driverId) {

        /**
         * Test/convenience sample: receive time = device time + 5ms, numeric payload,
         * GOOD quality, deterministic message id, schema v1.
         *
         * @param series     series identity
         * @param deviceTime device acquisition time
         * @param value      numeric value
         * @return a ready-to-use sample
         */
        public static PointValueSample simple(SeriesKey series, Instant deviceTime, double value) {
            return new PointValueSample(series, deviceTime, deviceTime.plusMillis(5),
                    String.valueOf(value), String.valueOf(value), value, 0,
                    series.tenantId() + "-" + series.deviceId() + "-" + series.pointId() + "-" + deviceTime,
                    1, "tck", 1, 1, 1);
        }
    }

    /**
     * S6/S15 aggregate functions. AVG/MIN/MAX/SUM/COUNT skip NULL numerics; FIRST/LAST
     * form the M4 rendering quadruple with MIN/MAX; PERCENTILE is capability-gated.
     */
    public enum AggregateFunction {
        /** Arithmetic mean over the window. */
        AVG,
        /** Minimum value in the window. */
        MIN,
        /** Maximum value in the window. */
        MAX,
        /** Sum over the window. */
        SUM,
        /** Row count over the window. */
        COUNT,
        /** First sample in the window. */
        FIRST,
        /** Last sample in the window. */
        LAST,
        /** Percentile, p supplied per call and capability-gated. */
        PERCENTILE
    }

    /**
     * Half-open time window [from, toExclusive).
     *
     * @param from        inclusive start
     * @param toExclusive exclusive end
     */
    public record TimeWindow(Instant from, Instant toExclusive) {
        /** Rejects empty or reversed windows. */
        public TimeWindow {
            if (!from.isBefore(toExclusive)) {
                throw new IllegalArgumentException("window from must be before toExclusive");
            }
        }
    }

    /**
     * S5: descending page anchor — (deviceTime, messageId) tuple; null = start from newest.
     *
     * @param deviceTime anchor device time
     * @param messageId  anchor message id
     */
    public record Cursor(Instant deviceTime, String messageId) {
    }

    /**
     * Descending cursor page: items plus the next anchor (null = exhausted).
     *
     * @param items      page items
     * @param nextCursor next page anchor, null when exhausted
     * @param <T>        item type
     */
    public record CursorPage<T>(List<T> items, Cursor nextCursor) {
    }

    /**
     * S6 single-window result over numericValue plus the raw sample count.
     *
     * @param value       aggregate value
     * @param sampleCount raw samples aggregated
     */
    public record WindowAggregate(Double value, long sampleCount) {
    }

    /**
     * S7 one bucket of a bucketed aggregate.
     *
     * @param bucketStart bucket start time
     * @param value       aggregate value
     * @param sampleCount raw samples in the bucket
     */
    public record BucketAggregate(Instant bucketStart, Double value, long sampleCount) {
    }

    /**
     * S13-② grouped count row.
     *
     * @param dimension grouping dimension
     * @param entityId  entity id within the dimension
     * @param count     sample count
     */
    public record DimensionCount(GroupDimension dimension, long entityId, long count) {
    }

    /**
     * S13-⑤ per-series count row — grouped by the full series identity
     * (tenant, device, point), unlike {@link DimensionCount}'s single-column
     * grouping. Surfaced by the manager topology volume query: a point shared
     * by several devices reports a different volume per device, which
     * single-dimension counts cannot reconstruct.
     *
     * @param series series identity
     * @param count  sample count
     */
    public record SeriesCount(SeriesKey series, long count) {
    }

    /**
     * S13-② grouping dimensions (the dashboard's whitelisted set).
     */
    public enum GroupDimension {
        /** Group by device. */
        DEVICE,
        /** Group by point. */
        POINT,
        /** Group by driver. */
        DRIVER
    }

    /**
     * S13-③ per-series last sample time inside a window.
     *
     * @param series   series identity
     * @param lastSeen newest sample time in the window
     */
    public record SeriesLastSeen(SeriesKey series, Instant lastSeen) {
    }

    /**
     * S13-④ latency histogram bin over receiveTime−deviceTime milliseconds.
     *
     * @param fromMsInclusive bin start in milliseconds
     * @param toMsExclusive   bin end in milliseconds
     * @param count           samples in the bin
     */
    public record LatencyBin(long fromMsInclusive, long toMsExclusive, long count) {
    }

    /**
     * S19 aligned-bucket Pearson correlation.
     *
     * @param pearson       correlation coefficient in [-1,1]
     * @param alignedBuckets buckets used after alignment
     */
    public record CorrelationResult(double pearson, long alignedBuckets) {
    }

    /**
     * S18 read deadline; expiry raises {@link TsdbQueryTimeout}.
     *
     * @param maxWait maximum wait allowance
     */
    public record TsdbDeadline(java.time.Duration maxWait) {

        /**
         * Deadline from a whole-second allowance.
         *
         * @param seconds allowance in whole seconds
         * @return a deadline expiring after the allowance
         */
        public static TsdbDeadline ofSeconds(long seconds) {
            return new TsdbDeadline(java.time.Duration.ofSeconds(seconds));
        }
    }

    /** S18/S6 read timeout signal — the port's runaway-scan guard. */
    public static final class TsdbQueryTimeout extends RuntimeException {

        /**
         * Timeout with a plain reason.
         *
         * @param message timeout reason
         */
        public TsdbQueryTimeout(String message) {
            super(message);
        }

        /**
         * Timeout caused by an underlying failure.
         *
         * @param message timeout reason
         * @param cause   underlying failure
         */
        public TsdbQueryTimeout(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
