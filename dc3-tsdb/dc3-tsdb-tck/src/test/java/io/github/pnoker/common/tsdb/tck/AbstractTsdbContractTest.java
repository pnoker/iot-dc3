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

package io.github.pnoker.common.tsdb.tck;

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
import io.github.pnoker.common.tsdb.spi.TsdbStore;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Store-neutral time-series contract suite (docs/design/tsdb-abstraction.md §10).
 * An adapter passes this suite ⇒ it is compliant; this is the acceptance bar for
 * community stores (TDengine, InfluxDB, IoTDB, GreptimeDB, ClickHouse ...).
 * Every case owns freshly generated series keys, so suites never interfere.
 *
 * @author pnoker
 * @since 2026.8.20
 */
public abstract class AbstractTsdbContractTest {

    protected static final TsdbDeadline DEADLINE = TsdbDeadline.ofSeconds(20);

    protected abstract TsdbStore store();

    protected long freshId() {
        return Math.abs(UUID.randomUUID().getLeastSignificantBits()) % 1_000_000 + 1000;
    }

    @BeforeEach
    void requireStore() {
        assertThat(store()).as("harness must provide a live store").isNotNull();
    }

    private PointValueSample sample(SeriesKey key, Instant time, double value, long latencyMs) {
        return new PointValueSample(key, time, time.plusMillis(latencyMs),
                String.valueOf(value), String.valueOf(value), value, 0,
                "tck-" + UUID.randomUUID(), 1, "tck-node", 1, 1, 1);
    }

    private TimeWindow window(Instant base, Duration length) {
        return new TimeWindow(base, base.plus(length));
    }

    @Test
    void appendReadbackPreservesEveryField() {
        SeriesKey key = new SeriesKey(900001, freshId(), freshId());
        Instant time = Instant.parse("2026-08-20T10:00:00.123456Z");
        PointValueSample sent = new PointValueSample(key, time, time.plusMillis(7),
                "raw-42", "cal-42.0", 42.0, 3, "mid-1", 2, "node-a", 11, 22, 33);
        store().append(List.of(sent));

        List<PointValueSample> back = store().last(SeriesFilter.of(key), 10, DEADLINE).get(key);
        assertThat(back).hasSize(1);
        PointValueSample got = back.get(0);
        assertThat(got.series()).isEqualTo(key);
        assertThat(got.deviceTime()).isEqualTo(sent.deviceTime());
        assertThat(got.receiveTime()).isEqualTo(sent.receiveTime());
        assertThat(got.rawValue()).isEqualTo("raw-42");
        assertThat(got.calValue()).isEqualTo("cal-42.0");
        assertThat(got.numericValue()).isEqualTo(42.0);
        assertThat(got.quality()).isEqualTo(3);
        assertThat(got.messageId()).isEqualTo("mid-1");
        assertThat(got.schemaVersion()).isEqualTo(2);
        assertThat(got.driverNode()).isEqualTo("node-a");
        assertThat(got.sequence()).isEqualTo(11);
        assertThat(got.fencingToken()).isEqualTo(22);
        assertThat(got.driverId()).isEqualTo(33);
    }

    @Test
    void lastReturnsNewestFirstWithExactLimit() {
        SeriesKey key = new SeriesKey(900002, freshId(), freshId());
        Instant base = Instant.parse("2026-08-20T11:00:00Z");
        List<PointValueSample> batch = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            batch.add(sample(key, base.plusSeconds(i), i, 1));
        }
        store().append(batch);
        List<PointValueSample> top3 = store().last(SeriesFilter.of(key), 3, DEADLINE).get(key);
        assertThat(top3).hasSize(3);
        assertThat(top3.get(0).numericValue()).isEqualTo(9);
        assertThat(top3.get(1).numericValue()).isEqualTo(8);
        assertThat(top3.get(2).numericValue()).isEqualTo(7);
    }

    @Test
    void historyCursorPagesWithoutSkipOrDuplicate() {
        SeriesKey key = new SeriesKey(900003, freshId(), freshId());
        Instant base = Instant.parse("2026-08-20T12:00:00Z");
        List<PointValueSample> batch = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            batch.add(sample(key, base.plusSeconds(i), i, 1));
        }
        store().append(batch);

        TimeWindow window = window(base.minusSeconds(1), Duration.ofMinutes(5));
        List<PointValueSample> collected = new ArrayList<>();
        Cursor cursor = null;
        int pages = 0;
        do {
            CursorPage<PointValueSample> page = store().history(SeriesFilter.of(key), window, cursor, 7, DEADLINE);
            collected.addAll(page.items());
            cursor = page.nextCursor();
            pages++;
            assertThat(pages).as("pagination must terminate").isLessThan(20);
        } while (Objects.nonNull(cursor));

        assertThat(collected).hasSize(25);
        List<String> ids = collected.stream().map(PointValueSample::messageId).toList();
        assertThat(ids).doesNotHaveDuplicates();
        List<Instant> times = collected.stream().map(PointValueSample::deviceTime).toList();
        assertThat(times).isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    void aggregateSkipsNonNumericAndCountsEverything() {
        SeriesKey key = new SeriesKey(900004, freshId(), freshId());
        Instant base = Instant.parse("2026-08-20T13:00:00Z");
        List<PointValueSample> batch = new ArrayList<>(List.of(
                sample(key, base, 10, 1),
                sample(key, base.plusSeconds(1), 20, 1),
                sample(key, base.plusSeconds(2), 30, 1)));
        batch.add(new PointValueSample(key, base.plusSeconds(3), base.plusSeconds(3).plusMillis(1),
                "text", "text", null, 0, "tck-str", 1, "tck", 1, 1, 1));
        store().append(batch);

        TimeWindow window = window(base.minusSeconds(1), Duration.ofMinutes(2));
        Map<SeriesKey, WindowAggregate> avg = store().aggregate(SeriesFilter.of(key),
                AggregateFunction.AVG, window, null, DEADLINE);
        assertThat(avg.get(key).value()).isEqualTo(20.0);
        assertThat(avg.get(key).sampleCount()).isEqualTo(4);

        Map<SeriesKey, WindowAggregate> count = store().aggregate(SeriesFilter.of(key),
                AggregateFunction.COUNT, window, null, DEADLINE);
        assertThat(count.get(key).value()).isEqualTo(4.0);
    }

    @Test
    void bucketedAggregateAlignsEpochAnchoredBuckets() {
        SeriesKey key = new SeriesKey(900005, freshId(), freshId());
        Instant t1 = Instant.parse("2026-08-20T14:00:10Z");
        Instant t2 = Instant.parse("2026-08-20T14:01:05Z");
        store().append(List.of(
                sample(key, t1, 10, 1), sample(key, t1.plusSeconds(1), 20, 1),
                sample(key, t2, 30, 1)));

        TimeWindow window = window(Instant.parse("2026-08-20T14:00:00Z"), Duration.ofMinutes(2));
        Map<SeriesKey, List<BucketAggregate>> buckets = store().bucketedAggregate(
                SeriesFilter.of(key), AggregateFunction.MAX, window, Duration.ofMinutes(1), null, DEADLINE);
        List<BucketAggregate> series = buckets.get(key);
        assertThat(series).hasSize(2);
        assertThat(series.get(0).bucketStart()).isEqualTo(Instant.parse("2026-08-20T14:00:00Z"));
        assertThat(series.get(0).value()).isEqualTo(20.0);
        assertThat(series.get(1).bucketStart()).isEqualTo(Instant.parse("2026-08-20T14:01:00Z"));
        assertThat(series.get(1).value()).isEqualTo(30.0);
    }

    @Test
    void countServesSeriesAndTenantScopes() {
        long deviceId = freshId();
        SeriesKey a = new SeriesKey(900006, deviceId, freshId());
        SeriesKey b = new SeriesKey(900006, deviceId, freshId());
        Instant base = Instant.parse("2026-08-20T15:00:00Z");
        store().append(List.of(sample(a, base, 1, 1), sample(a, base.plusSeconds(1), 2, 1),
                sample(b, base, 3, 1)));

        TimeWindow window = window(base.minusSeconds(1), Duration.ofMinutes(1));
        assertThat(store().count(SeriesFilter.of(a), window, DEADLINE)).isEqualTo(2);
        Assumptions.assumeTrue(store().capabilities().tenantWideScan(),
                "store declares tenantWideScan=false");
        assertThat(store().count(SeriesFilter.tenantWide(900006), window, DEADLINE)).isEqualTo(3);
    }

    @Test
    void duplicateSeriesTimestampUpsertsLastWrite() {
        SeriesKey key = new SeriesKey(900007, freshId(), freshId());
        Instant time = Instant.parse("2026-08-20T16:00:00Z");
        store().append(List.of(sample(key, time, 1, 1)));
        store().append(List.of(sample(key, time, 99, 1)));

        TimeWindow window = window(time.minusSeconds(1), Duration.ofMinutes(1));
        assertThat(store().count(SeriesFilter.of(key), window, DEADLINE)).isEqualTo(1);
        Map<SeriesKey, WindowAggregate> max = store().aggregate(SeriesFilter.of(key),
                AggregateFunction.MAX, window, null, DEADLINE);
        assertThat(max.get(key).value()).isEqualTo(99.0);
    }

    @Test
    void backfillOlderThanNewestIsAccepted() {
        Assumptions.assumeTrue(store().capabilities().backfill(), "store declares backfill=false");
        SeriesKey key = new SeriesKey(900008, freshId(), freshId());
        Instant base = Instant.parse("2026-08-20T17:00:00Z");
        store().append(List.of(sample(key, base.plusSeconds(60), 2, 1)));
        store().append(List.of(sample(key, base.plusSeconds(10), 1, 1)));

        TimeWindow window = window(base, Duration.ofMinutes(5));
        Map<SeriesKey, WindowAggregate> min = store().aggregate(SeriesFilter.of(key),
                AggregateFunction.MIN, window, null, DEADLINE);
        assertThat(min.get(key).value()).isEqualTo(1.0);
    }

    @Test
    void crossTenantReadsSeeNothing() {
        SeriesKey mine = new SeriesKey(900009, freshId(), freshId());
        SeriesKey theirs = new SeriesKey(999999, freshId(), freshId());
        Instant time = Instant.parse("2026-08-20T18:00:00Z");
        store().append(List.of(sample(theirs, time, 42, 1)));

        TimeWindow window = window(time.minusSeconds(1), Duration.ofMinutes(1));
        assertThat(store().last(SeriesFilter.of(mine), 10, DEADLINE)).doesNotContainKey(mine);
        assertThat(store().count(SeriesFilter.of(mine), window, DEADLINE)).isZero();
    }

    @Test
    void microsecondPrecisionRoundTrips() {
        SeriesKey key = new SeriesKey(900011, freshId(), freshId());
        Instant time = Instant.parse("2026-08-20T19:00:00.123456Z");
        store().append(List.of(sample(key, time, 1, 1)));
        List<PointValueSample> back = store().last(SeriesFilter.of(key), 1, DEADLINE).get(key);
        // stores with coarser native precision may round; the contract is predictability
        assertThat(back.get(0).deviceTime()).isEqualTo(time);
    }

    @Test
    void fiveThousandSampleBurstLandsComplete() {
        SeriesKey key = new SeriesKey(900012, freshId(), freshId());
        Instant base = Instant.parse("2026-08-20T20:00:00Z");
        List<PointValueSample> burst = new ArrayList<>(5000);
        for (int i = 0; i < 5000; i++) {
            burst.add(sample(key, base.plusMillis(i), i % 100, 1));
        }
        store().append(burst);

        TimeWindow window = window(base.minusMillis(1), Duration.ofMinutes(1));
        Awaitility.await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(store().count(SeriesFilter.of(key), window, DEADLINE))
                        .isEqualTo(5000));
    }

    @Test
    void tenantBucketedCountAggregatesAcrossSeries() {
        Assumptions.assumeTrue(store().capabilities().tenantWideAnalytics(),
                "store declares tenantWideAnalytics=false");
        long deviceId = freshId();
        SeriesKey a = new SeriesKey(900013, deviceId, freshId());
        SeriesKey b = new SeriesKey(900013, deviceId, freshId());
        Instant base = Instant.parse("2026-08-20T21:00:00Z");
        store().append(List.of(sample(a, base, 1, 1), sample(a, base.plusSeconds(1), 1, 1),
                sample(b, base, 1, 1)));

        List<BucketAggregate> buckets = store().bucketedCount(900013,
                window(base.minusSeconds(1), Duration.ofMinutes(2)), Duration.ofMinutes(1), DEADLINE);
        assertThat(buckets).hasSize(1);
        assertThat(buckets.get(0).sampleCount()).isEqualTo(3);
    }

    @Test
    void countByDimensionRanksCorrectly() {
        Assumptions.assumeTrue(store().capabilities().tenantWideAnalytics(),
                "store declares tenantWideAnalytics=false");
        SeriesKey a = new SeriesKey(900014, freshId(), freshId());
        SeriesKey b = new SeriesKey(900014, freshId(), freshId());
        Instant base = Instant.parse("2026-08-20T22:00:00Z");
        store().append(List.of(sample(a, base, 1, 1), sample(a, base.plusSeconds(1), 1, 1),
                sample(b, base, 1, 1)));

        List<DimensionCount> byPoint = store().countByDimension(900014,
                window(base.minusSeconds(1), Duration.ofMinutes(1)), GroupDimension.POINT, 10, DEADLINE);
        assertThat(byPoint).isNotEmpty();
        assertThat(byPoint.get(0).count()).isEqualTo(2);
    }

    @Test
    void seriesCountsGroupByFullSeriesIdentity() {
        Assumptions.assumeTrue(store().capabilities().tenantWideAnalytics(),
                "store declares tenantWideAnalytics=false");
        long deviceId = freshId();
        SeriesKey sharedPointOnA = new SeriesKey(900024, deviceId, freshId());
        SeriesKey sharedPointOnB = new SeriesKey(900024, freshId(), sharedPointOnA.pointId());
        Instant base = Instant.parse("2026-08-21T01:00:00Z");
        store().append(List.of(sample(sharedPointOnA, base, 1, 1),
                sample(sharedPointOnA, base.plusSeconds(1), 2, 1),
                sample(sharedPointOnB, base, 3, 1)));

        List<SeriesCount> counts = store().seriesCounts(900024,
                window(base.minusSeconds(1), Duration.ofMinutes(2)), DEADLINE);
        // The same point id reported under two devices must stay two distinct
        // rows — single-dimension grouping would merge them into one count.
        assertThat(counts).anySatisfy(row -> {
            assertThat(row.series()).isEqualTo(sharedPointOnA);
            assertThat(row.count()).isEqualTo(2);
        });
        assertThat(counts).anySatisfy(row -> {
            assertThat(row.series()).isEqualTo(sharedPointOnB);
            assertThat(row.count()).isEqualTo(1);
        });
    }


    @Test
    void rollupTierReadsStayConsistentWithRawScans() {
        SeriesKey key = new SeriesKey(900025, freshId(), freshId());
        // Five minute-aligned minutes of 10-second samples with distinct values.
        Instant base = Instant.parse("2026-08-21T02:00:00Z");
        List<PointValueSample> batch = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            batch.add(sample(key, base.plusSeconds(10L * i), i, 1));
        }
        store().append(batch);
        TimeWindow window = window(base, Duration.ofMinutes(5));

        // COUNT via (possibly tiered) minute buckets must equal the raw count.
        long tierCount = store().bucketedAggregate(SeriesFilter.of(key), AggregateFunction.COUNT,
                        window, Duration.ofMinutes(1), null, DEADLINE)
                .getOrDefault(key, List.of()).stream().mapToLong(BucketAggregate::sampleCount).sum();
        assertThat(tierCount).isEqualTo(store().count(SeriesFilter.of(key), window, DEADLINE));

        // Coarse AVG recombined from tier sums must equal the raw window average.
        BucketAggregate coarse = store().bucketedAggregate(SeriesFilter.of(key), AggregateFunction.AVG,
                window, Duration.ofMinutes(5), null, DEADLINE).get(key).getFirst();
        WindowAggregate rawAvg = store().aggregate(SeriesFilter.of(key), AggregateFunction.AVG,
                window, null, DEADLINE).get(key);
        assertThat(coarse.value()).isCloseTo(rawAvg.value(), org.assertj.core.data.Offset.offset(1e-9));
        assertThat(coarse.sampleCount()).isEqualTo(rawAvg.sampleCount());

        // Coarse LAST recombined from tier lasts must equal the raw window last.
        BucketAggregate coarseLast = store().bucketedAggregate(SeriesFilter.of(key), AggregateFunction.LAST,
                window, Duration.ofMinutes(5), null, DEADLINE).get(key).getFirst();
        WindowAggregate rawLast = store().aggregate(SeriesFilter.of(key), AggregateFunction.LAST,
                window, null, DEADLINE).get(key);
        assertThat(coarseLast.value()).isEqualTo(rawLast.value());

        // Tenant-wide bucketed count agrees with the raw tenant count.
        long tenantBuckets = store().bucketedCount(key.tenantId(), window, Duration.ofMinutes(1), DEADLINE)
                .stream().mapToLong(BucketAggregate::sampleCount).sum();
        assertThat(tenantBuckets).isEqualTo(store().count(SeriesFilter.tenantWide(key.tenantId()),
                window, DEADLINE));

        // PERCENTILE never uses tiers; stores that support it must serve bucketed
        // percentiles on the raw path without supertable-style failures.
        if (store().capabilities().percentile()) {
            List<BucketAggregate> p50 = store().bucketedAggregate(SeriesFilter.of(key),
                            AggregateFunction.PERCENTILE, window, Duration.ofMinutes(1), 0.5, DEADLINE)
                    .getOrDefault(key, List.of());
            assertThat(p50).hasSize(5);
            assertThat(p50).allSatisfy(bucket -> assertThat(bucket.value()).isNotNull());
        }
    }

    @Test
    void lastSeenPerSeriesReportsNewestSample() {
        Assumptions.assumeTrue(store().capabilities().tenantWideAnalytics(),
                "store declares tenantWideAnalytics=false");
        SeriesKey key = new SeriesKey(900015, freshId(), freshId());
        Instant base = Instant.parse("2026-08-20T23:00:00Z");
        store().append(List.of(sample(key, base, 1, 1), sample(key, base.plusSeconds(30), 2, 1)));

        List<SeriesLastSeen> seen = store().lastSeenPerSeries(900015,
                window(base.minusSeconds(1), Duration.ofMinutes(2)), DEADLINE);
        assertThat(seen).anyMatch(s -> s.series().equals(key)
                && s.lastSeen().equals(base.plusSeconds(30)));
    }

    @Test
    void latencyHistogramBinsReceiveMinusDeviceTime() {
        Assumptions.assumeTrue(store().capabilities().latencyHistogram(),
                "store declares latencyHistogram=false");
        SeriesKey key = new SeriesKey(900016, freshId(), freshId());
        Instant base = Instant.parse("2026-08-20T23:30:00Z");
        store().append(List.of(sample(key, base, 1, 50), sample(key, base.plusSeconds(1), 2, 500)));

        List<LatencyBin> bins = store().latencyHistogram(900016,
                window(base.minusSeconds(1), Duration.ofMinutes(1)), List.of(100L, 1000L), DEADLINE);
        assertThat(bins.get(0).count()).isEqualTo(1);
        assertThat(bins.get(1).count()).isEqualTo(1);
    }

    @Test
    void multiSeriesFilterKeepsSeriesApart() {
        SeriesKey a = new SeriesKey(900017, freshId(), freshId());
        SeriesKey b = new SeriesKey(900017, freshId(), freshId());
        Instant base = Instant.parse("2026-08-21T00:00:00Z");
        store().append(List.of(sample(a, base, 1, 1), sample(a, base.plusSeconds(1), 2, 1),
                sample(b, base, 99, 1)));

        Map<SeriesKey, List<PointValueSample>> last =
                store().last(SeriesFilter.of(List.of(a, b)), 10, DEADLINE);
        assertThat(last.get(a)).hasSize(2);
        assertThat(last.get(b)).hasSize(1);
        assertThat(last.get(b).get(0).numericValue()).isEqualTo(99);
        assertThat(last.keySet()).containsExactlyInAnyOrder(a, b);
    }

    @Test
    void firstLastFormM4QuadruplePerBucket() {
        SeriesKey key = new SeriesKey(900018, freshId(), freshId());
        Instant base = Instant.parse("2026-08-21T01:00:00Z");
        store().append(List.of(sample(key, base, 5, 1), sample(key, base.plusSeconds(10), 1, 1),
                sample(key, base.plusSeconds(20), 9, 1), sample(key, base.plusSeconds(30), 3, 1)));

        TimeWindow window = window(base.minusSeconds(1), Duration.ofMinutes(1));
        Map<SeriesKey, List<BucketAggregate>> firsts = store().bucketedAggregate(
                SeriesFilter.of(key), AggregateFunction.FIRST, window, Duration.ofMinutes(1), null, DEADLINE);
        Map<SeriesKey, List<BucketAggregate>> lasts = store().bucketedAggregate(
                SeriesFilter.of(key), AggregateFunction.LAST, window, Duration.ofMinutes(1), null, DEADLINE);
        assertThat(firsts.get(key).get(0).value()).isEqualTo(5.0);
        assertThat(lasts.get(key).get(0).value()).isEqualTo(3.0);
    }

    @Test
    void percentileWithinDeclaredTolerance() {
        Assumptions.assumeTrue(store().capabilities().percentile(), "store declares percentile=false");
        SeriesKey key = new SeriesKey(900019, freshId(), freshId());
        Instant base = Instant.parse("2026-08-21T02:00:00Z");
        List<PointValueSample> batch = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            batch.add(sample(key, base.plusSeconds(i), i, 1));
        }
        store().append(batch);

        Map<SeriesKey, WindowAggregate> p50 = store().aggregate(SeriesFilter.of(key),
                AggregateFunction.PERCENTILE, window(base, Duration.ofMinutes(2)), 0.5, DEADLINE);
        assertThat(p50.get(key).value()).isBetween(45.0, 55.0);
    }

    @Test
    void qualityFlagSurvivesRoundTrip() {
        SeriesKey key = new SeriesKey(900021, freshId(), freshId());
        Instant time = Instant.parse("2026-08-21T03:00:00Z");
        PointValueSample bad = new PointValueSample(key, time, time.plusMillis(1), "x", "x",
                1.0, 12, "tck-q", 1, "tck", 1, 1, 1);
        store().append(List.of(bad));
        List<PointValueSample> back = store().last(SeriesFilter.of(key), 1, DEADLINE).get(key);
        assertThat(back.get(0).quality()).isEqualTo(12);
    }

    @Test
    void deadlineGuardDoesNotHang() {
        SeriesKey key = new SeriesKey(900022, freshId(), freshId());
        Instant base = Instant.parse("2026-08-21T04:00:00Z");
        List<PointValueSample> burst = new ArrayList<>(3000);
        for (int i = 0; i < 3000; i++) {
            burst.add(sample(key, base.plusMillis(i), i % 50, 1));
        }
        store().append(burst);
        // the contract is "does not hang": under a sub-second deadline the read either
        // raises the store's timeout or completes promptly. JDBC-backed stores round
        // deadlines up to whole seconds, so an indexed count may legitimately finish
        // first — wall-clock boundedness is what must hold everywhere.
        long start = System.nanoTime();
        try {
            store().count(SeriesFilter.tenantWide(900022),
                    window(base.minusSeconds(1), Duration.ofMinutes(1)),
                    new TsdbDeadline(Duration.ofMillis(1)));
        } catch (RuntimeException expected) {
            // timeout-style failure is the ideal outcome
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(elapsedMs).as("deadline-bounded read must not hang").isLessThan(10_000);
    }

    @Test
    void correlationDetectsKnownRelationships() {
        Assumptions.assumeTrue(store().capabilities().correlation(), "store declares correlation=false");
        long deviceId = freshId();
        SeriesKey a = new SeriesKey(900023, deviceId, freshId());
        SeriesKey b = new SeriesKey(900023, deviceId, freshId());
        SeriesKey c = new SeriesKey(900023, deviceId, freshId());
        Instant base = Instant.parse("2026-08-21T05:00:00Z");
        List<PointValueSample> batch = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            Instant t = base.plusSeconds(i);
            batch.add(sample(a, t, i, 1));
            batch.add(sample(b, t, 2 * i + 10, 1));
            batch.add(sample(c, t, (i * 37) % 60, 1));
        }
        store().append(batch);
        TimeWindow window = window(base.minusSeconds(1), Duration.ofMinutes(2));

        CorrelationResult ab = store().correlation(a, b, window, Duration.ofSeconds(1), DEADLINE);
        assertThat(ab.pearson()).isGreaterThan(0.99);
        assertThat(ab.alignedBuckets()).isGreaterThanOrEqualTo(50);

        CorrelationResult ac = store().correlation(a, c, window, Duration.ofSeconds(1), DEADLINE);
        assertThat(ac.pearson()).isLessThan(0.5);
    }
}
