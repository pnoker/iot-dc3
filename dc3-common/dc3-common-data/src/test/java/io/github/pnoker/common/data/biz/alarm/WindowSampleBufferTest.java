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

package io.github.pnoker.common.data.biz.alarm;

import io.github.pnoker.common.data.entity.property.AlarmWindowProperties;
import io.github.pnoker.common.enums.AlarmTargetTypeFlagEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WindowSampleBufferTest {

    private AlarmWindowProperties properties;
    private WindowSampleBuffer buffer;

    private static final WindowSampleKey KEY_A = new WindowSampleKey(7L, AlarmTargetTypeFlagEnum.POINT, 11L);
    private static final WindowSampleKey KEY_B = new WindowSampleKey(7L, AlarmTargetTypeFlagEnum.POINT, 12L);

    @BeforeEach
    void setUp() {
        properties = new AlarmWindowProperties();
        properties.setLocalCutoff(Duration.ofMinutes(5));
        properties.setMaxSamplesPerBuffer(10);
        properties.setMaxBufferKeys(100);
        buffer = new WindowSampleBuffer(properties);
    }

    private static WindowSample sample(double value, LocalDateTime at) {
        return new WindowSample(value, Double.toString(value), at);
    }

    @Test
    void appendAndSnapshotReturnsAllInWindow() {
        LocalDateTime now = LocalDateTime.now();
        buffer.append(KEY_A, sample(80.0, now.minusSeconds(30)));
        buffer.append(KEY_A, sample(82.5, now.minusSeconds(20)));
        buffer.append(KEY_A, sample(85.0, now.minusSeconds(10)));

        List<WindowSample> snap = buffer.snapshot(KEY_A, now.minusMinutes(1), now);
        assertThat(snap).extracting(WindowSample::numValue).containsExactly(80.0, 82.5, 85.0);
    }

    @Test
    void snapshotFiltersByTimeRange() {
        LocalDateTime now = LocalDateTime.now();
        buffer.append(KEY_A, sample(1.0, now.minusMinutes(10))); // outside retention but appended before trim runs?
        buffer.append(KEY_A, sample(2.0, now.minusSeconds(60)));
        buffer.append(KEY_A, sample(3.0, now.minusSeconds(10)));

        // Sample 1.0 is dropped by the time-based trim (cutoff = now - 5min).
        List<WindowSample> snap = buffer.snapshot(KEY_A, now.minusSeconds(30), now);
        assertThat(snap).extracting(WindowSample::numValue).containsExactly(3.0);
    }

    @Test
    void enforcesPerBufferSizeCap() {
        properties.setMaxSamplesPerBuffer(3);
        buffer = new WindowSampleBuffer(properties);
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 10; i++) {
            buffer.append(KEY_A, sample(i, now.plusNanos(i)));
        }

        List<WindowSample> snap = buffer.snapshot(KEY_A, null, null);
        assertThat(snap).extracting(WindowSample::numValue).containsExactly(7.0, 8.0, 9.0);
    }

    @Test
    void enforcesTimeBasedRetention() {
        properties.setLocalCutoff(Duration.ofMinutes(1));
        buffer = new WindowSampleBuffer(properties);
        LocalDateTime now = LocalDateTime.now();
        // Two old samples + one fresh; appending fresh triggers the trim and
        // drops the old ones.
        buffer.append(KEY_A, sample(1.0, now.minusMinutes(5)));
        buffer.append(KEY_A, sample(2.0, now.minusMinutes(2)));
        buffer.append(KEY_A, sample(3.0, now));

        assertThat(buffer.size(KEY_A)).isEqualTo(1);
        assertThat(buffer.snapshot(KEY_A, null, null))
                .extracting(WindowSample::numValue).containsExactly(3.0);
    }

    @Test
    void distinctKeysAreIsolated() {
        LocalDateTime now = LocalDateTime.now();
        buffer.append(KEY_A, sample(80.0, now));
        buffer.append(KEY_B, sample(99.9, now));

        assertThat(buffer.size(KEY_A)).isEqualTo(1);
        assertThat(buffer.size(KEY_B)).isEqualTo(1);
        assertThat(buffer.snapshot(KEY_A, null, null))
                .extracting(WindowSample::numValue).containsExactly(80.0);
        assertThat(buffer.snapshot(KEY_B, null, null))
                .extracting(WindowSample::numValue).containsExactly(99.9);
    }

    @Test
    void invalidateDropsKey() {
        LocalDateTime now = LocalDateTime.now();
        buffer.append(KEY_A, sample(80.0, now));
        buffer.invalidate(KEY_A);
        assertThat(buffer.size(KEY_A)).isZero();
    }

    @Test
    void appendNullSampleIsNoop() {
        buffer.append(KEY_A, null);
        assertThat(buffer.size(KEY_A)).isZero();
    }

}
