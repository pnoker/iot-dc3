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

package io.github.pnoker.common.tsdb.timescale;

import io.github.pnoker.common.tsdb.model.TsdbModel.PointValueSample;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesKey;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests of the in-batch natural-key collapse that keeps a single
 * {@code INSERT ... SELECT unnest ON CONFLICT} statement from failing with
 * "cannot affect row a second time". No database involved.
 *
 * @author pnoker
 * @since 2026.8.25
 */
class TimescaleNaturalKeyDedupTest {

    private static final Instant BASE = Instant.parse("2026-08-20T10:00:00Z");

    private static PointValueSample sample(long tenant, long device, long point, Instant time, String message) {
        SeriesKey series = new SeriesKey(tenant, device, point);
        return new PointValueSample(series, time, time.plusMillis(5), message, message,
                Double.parseDouble(message), 0, "mid-" + message, 1, "tck", 1, 1, 1);
    }

    @Test
    void duplicateNaturalKeyKeepsLastOccurrence() {
        PointValueSample first = sample(1, 2, 3, BASE, "10");
        PointValueSample second = sample(1, 2, 3, BASE, "99");

        List<PointValueSample> collapsed = TimescaleTsdbStore.collapseNaturalKeyDuplicates(List.of(first, second));

        assertEquals(1, collapsed.size());
        assertEquals("99", collapsed.getFirst().rawValue(), "last occurrence in input order must win");
    }

    @Test
    void duplicateWithinSameSeriesDifferentTimesStaysApart() {
        PointValueSample a = sample(1, 2, 3, BASE, "10");
        PointValueSample b = sample(1, 2, 3, BASE.plusSeconds(1), "11");

        List<PointValueSample> collapsed = TimescaleTsdbStore.collapseNaturalKeyDuplicates(List.of(a, b));

        assertEquals(2, collapsed.size(), "same series at different device times is not a duplicate");
    }

    @Test
    void sameTripleDifferentTenantsStaysApart() {
        PointValueSample a = sample(1, 2, 3, BASE, "10");
        PointValueSample b = sample(9, 2, 3, BASE, "11");

        List<PointValueSample> collapsed = TimescaleTsdbStore.collapseNaturalKeyDuplicates(List.of(a, b));

        assertEquals(2, collapsed.size(), "tenant is part of the natural key");
    }

    @Test
    void noDuplicatesPassThroughUnchanged() {
        List<PointValueSample> input = List.of(
                sample(1, 2, 3, BASE, "10"),
                sample(1, 2, 3, BASE.plusSeconds(1), "11"),
                sample(1, 2, 4, BASE, "12"),
                sample(1, 5, 3, BASE, "13"));

        List<PointValueSample> collapsed = TimescaleTsdbStore.collapseNaturalKeyDuplicates(input);

        assertEquals(input, collapsed, "duplicate-free input must pass through with identical content");
        assertNotSame(input, collapsed, "the helper returns a new list, never mutates the input");
    }

    @Test
    void orderOfDistinctKeysIsStable() {
        PointValueSample a = sample(1, 2, 3, BASE, "1");
        PointValueSample b = sample(1, 2, 4, BASE, "2");
        PointValueSample bAgain = sample(1, 2, 4, BASE, "22");
        PointValueSample c = sample(1, 2, 5, BASE, "3");
        PointValueSample aAgain = sample(1, 2, 3, BASE, "11");

        List<PointValueSample> collapsed = TimescaleTsdbStore.collapseNaturalKeyDuplicates(
                List.of(a, b, bAgain, c, aAgain));

        assertEquals(3, collapsed.size());
        assertEquals("11", collapsed.get(0).rawValue(), "first key keeps its slot, value is the last write");
        assertEquals("22", collapsed.get(1).rawValue());
        assertEquals("3", collapsed.get(2).rawValue());
        assertTrue(collapsed.get(0).series().equals(a.series())
                && collapsed.get(1).series().equals(b.series())
                && collapsed.get(2).series().equals(c.series()), "relative order of distinct keys is stable");
    }

    @Test
    void emptyInputYieldsEmptyOutput() {
        assertTrue(TimescaleTsdbStore.collapseNaturalKeyDuplicates(List.of()).isEmpty());
    }
}
