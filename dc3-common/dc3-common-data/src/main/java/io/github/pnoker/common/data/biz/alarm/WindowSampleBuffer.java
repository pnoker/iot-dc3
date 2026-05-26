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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.pnoker.common.data.entity.property.AlarmWindowProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Per-entity ring of recent samples used for short-window alarm evaluation.
 * Backed by Caffeine for top-level eviction (idle keys) and a per-key
 * {@link ConcurrentLinkedDeque} that trims oldest samples on append.
 *
 * <p>The buffer is bounded along two axes: time (samples older than the
 * configured retention are dropped) and count (samples beyond the per-key cap
 * are dropped from the head). Either bound trims independently; the larger
 * cap is simply never reached when the smaller fires first.
 *
 * <p>Reads return an immutable snapshot ordered oldest → newest. Writers and
 * readers can run concurrently because the underlying deque is thread-safe;
 * the snapshot copies under no lock, so a caller may race with a concurrent
 * append (acceptable — a single sample appearing or not appearing in a fold
 * is benign for window-aggregated alarms).
 *
 * @author pnoker
 * @version 2026.5.21
 * @since 2026.5.21
 */
@Slf4j
@Component
public class WindowSampleBuffer {

    private final AlarmWindowProperties properties;

    private final Cache<WindowSampleKey, ConcurrentLinkedDeque<WindowSample>> buffers;

    public WindowSampleBuffer(AlarmWindowProperties properties) {
        this.properties = properties;
        this.buffers = Caffeine.newBuilder()
                .maximumSize(properties.getMaxBufferKeys())
                .expireAfterAccess(properties.getBufferIdleExpiry())
                .build();
    }

    /**
     * Append a sample and trim by time + count. Trim happens inline on every
     * append to keep memory bounded — the caller already pays the deque
     * traversal once when reading, doing it twice (write + read) is fine.
     */
    public void append(WindowSampleKey key, WindowSample sample) {
        if (Objects.isNull(key) || Objects.isNull(sample) || Objects.isNull(sample.timestamp())) {
            return;
        }
        ConcurrentLinkedDeque<WindowSample> deque = buffers.get(key, k -> new ConcurrentLinkedDeque<>());
        if (Objects.isNull(deque)) {
            return;
        }
        deque.addLast(sample);
        trim(deque, sample.timestamp());
    }

    /**
     * Read all samples in {@code [from, to]} for {@code key}, ordered oldest
     * → newest. Returns an empty list if the buffer is unknown.
     */
    public List<WindowSample> snapshot(WindowSampleKey key, LocalDateTime from, LocalDateTime to) {
        if (Objects.isNull(key)) {
            return List.of();
        }
        Deque<WindowSample> deque = buffers.getIfPresent(key);
        if (Objects.isNull(deque)) {
            return List.of();
        }
        List<WindowSample> result = new ArrayList<>(deque.size());
        for (WindowSample sample : deque) {
            LocalDateTime ts = sample.timestamp();
            if (Objects.isNull(ts)) {
                continue;
            }
            if (Objects.nonNull(from) && ts.isBefore(from)) {
                continue;
            }
            if (Objects.nonNull(to) && ts.isAfter(to)) {
                continue;
            }
            result.add(sample);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Number of samples currently retained for {@code key} (post-trim).
     */
    public int size(WindowSampleKey key) {
        if (Objects.isNull(key)) {
            return 0;
        }
        Deque<WindowSample> deque = buffers.getIfPresent(key);
        return Objects.nonNull(deque) ? deque.size() : 0;
    }

    /**
     * Drop everything for {@code key}; used by tests and admin tools.
     */
    public void invalidate(WindowSampleKey key) {
        if (Objects.nonNull(key)) {
            buffers.invalidate(key);
        }
    }

    public void invalidateAll() {
        buffers.invalidateAll();
    }

    private void trim(Deque<WindowSample> deque, LocalDateTime now) {
        Duration retention = properties.getLocalCutoff();
        int maxSamples = properties.getMaxSamplesPerBuffer();

        // Trim by time first — older samples are at the head because we always
        // addLast, so the head is the oldest.
        if (Objects.nonNull(retention) && !retention.isZero() && !retention.isNegative()) {
            LocalDateTime cutoff = now.minus(retention);
            for (Iterator<WindowSample> it = deque.iterator(); it.hasNext(); ) {
                WindowSample first = it.next();
                if (Objects.isNull(first.timestamp()) || first.timestamp().isBefore(cutoff)) {
                    it.remove();
                } else {
                    break;
                }
            }
        }

        // Trim by count. Concurrent removes here are benign — at most we under-
        // shoot maxSamples for a moment.
        int drift = deque.size();
        while (drift > maxSamples && Objects.nonNull(deque.pollFirst())) {
            drift--;
        }
    }

}
