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

    /** S1: series identity — platform numeric IDs; names enriched at the app layer. */
    public record SeriesKey(long tenantId, long deviceId, long pointId) {
    }

    /**
     * Unified read filter (S14): a non-empty series list selects exactly those series;
     * an empty list is a tenant-wide scan (capability {@code tenantWideScan}). The
     * tenant id is always present — tenant isolation is a hard constraint (S11).
     */
    public record SeriesFilter(long tenantId, List<SeriesKey> series) {

        public static SeriesFilter of(SeriesKey single) {
            return new SeriesFilter(single.tenantId(), List.of(single));
        }

        public static SeriesFilter of(List<SeriesKey> series) {
            if (series == null || series.isEmpty()) {
                throw new IllegalArgumentException("series filter needs at least one series");
            }
            return new SeriesFilter(series.get(0).tenantId(), List.copyOf(series));
        }

        public static SeriesFilter tenantWide(long tenantId) {
            return new SeriesFilter(tenantId, List.of());
        }

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

        public static PointValueSample simple(SeriesKey series, Instant deviceTime, double value) {
            return new PointValueSample(series, deviceTime, deviceTime.plusMillis(5),
                    String.valueOf(value), String.valueOf(value), value, 0,
                    series.tenantId() + "-" + series.deviceId() + "-" + series.pointId() + "-" + deviceTime,
                    1, "tck", 1, 1, 1);
        }
    }

    /** S6/S15 aggregate functions. AVG/MIN/MAX/SUM/COUNT skip NULL numerics; FIRST/LAST
     * form the M4 rendering quadruple with MIN/MAX; PERCENTILE is capability-gated. */
    public enum AggregateFunction {AVG, MIN, MAX, SUM, COUNT, FIRST, LAST, PERCENTILE}

    /** Half-open time window [from, toExclusive). */
    public record TimeWindow(Instant from, Instant toExclusive) {
        public TimeWindow {
            if (!from.isBefore(toExclusive)) {
                throw new IllegalArgumentException("window from must be before toExclusive");
            }
        }
    }

    /** S5: descending page anchor — (deviceTime, messageId) tuple; null = start from newest. */
    public record Cursor(Instant deviceTime, String messageId) {
    }

    /** Descending cursor page: items plus the next anchor (null = exhausted). */
    public record CursorPage<T>(List<T> items, Cursor nextCursor) {
    }

    /** S6 single-window result over numericValue plus the raw sample count. */
    public record WindowAggregate(Double value, long sampleCount) {
    }

    /** S7 one bucket of a bucketed aggregate. */
    public record BucketAggregate(Instant bucketStart, Double value, long sampleCount) {
    }

    /** S13-② grouped count row. */
    public record DimensionCount(GroupDimension dimension, long entityId, long count) {
    }

    /** S13-② grouping dimensions (the dashboard's whitelisted set). */
    public enum GroupDimension {DEVICE, POINT, DRIVER}

    /** S13-③ per-series last sample time inside a window. */
    public record SeriesLastSeen(SeriesKey series, Instant lastSeen) {
    }

    /** S13-④ latency histogram bin over receiveTime−deviceTime milliseconds. */
    public record LatencyBin(long fromMsInclusive, long toMsExclusive, long count) {
    }

    /** S19 aligned-bucket Pearson correlation. */
    public record CorrelationResult(double pearson, long alignedBuckets) {
    }

    /** S18 read deadline; expiry raises {@link TsdbQueryTimeout}. */
    public record TsdbDeadline(java.time.Duration maxWait) {

        public static TsdbDeadline ofSeconds(long seconds) {
            return new TsdbDeadline(java.time.Duration.ofSeconds(seconds));
        }
    }

    /** S18/S6 read timeout signal — the port's runaway-scan guard. */
    public static final class TsdbQueryTimeout extends RuntimeException {

        public TsdbQueryTimeout(String message) {
            super(message);
        }

        public TsdbQueryTimeout(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
