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
package io.github.pnoker.common.agentic.service.runtime;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Bounded-backpressure accounting for the agent stream: how many emitted frames are
 * still un-consumed by the downstream SSE subscriber, and whether the consumer has
 * been stalled beyond the buffer budget for longer than the idle timeout.
 *
 * <p>A slow consumer is tolerated up to the buffer budget; the turn only fails when the
 * buffer stays over budget AND downstream demand stays absent for the whole grace
 * period. Transient stalls (tab backgrounding, slow proxies) therefore no longer kill
 * long generations, while a genuinely abandoned stream cannot grow memory unboundedly.</p>
 *
 * @author pnoker
 * @since 2026.10.10
 */
public final class AgenticBackpressureGuard {

    private static final long NOT_STALLED = 0L;

    private final AtomicLong outstanding = new AtomicLong();

    private final AtomicLong stalledSince = new AtomicLong(NOT_STALLED);

    private final long bufferLimit;

    private final long idleTimeoutNanos;

    private final LongSupplier clock;

    private AgenticBackpressureGuard(long bufferLimit, long idleTimeoutNanos, LongSupplier clock) {
        this.bufferLimit = bufferLimit;
        this.idleTimeoutNanos = idleTimeoutNanos;
        this.clock = clock;
    }

    /**
     * Create a guard.
     *
     * @param bufferLimit     frames tolerated ahead of downstream demand
     * @param idleTimeoutNanos grace period a full buffer may stay un-demanded before failing
     * @param clock           monotonic time source in nanoseconds (injectable for tests)
     * @return the guard
     */
    public static AgenticBackpressureGuard create(long bufferLimit, long idleTimeoutNanos, LongSupplier clock) {
        return new AgenticBackpressureGuard(bufferLimit, idleTimeoutNanos, clock);
    }

    /**
     * Account one emitted frame and decide whether the stream must fail.
     *
     * @return {@code true} when the buffer has been over budget for longer than the idle
     * timeout — the caller must terminate the turn
     */
    public boolean onFrameEmitted() {
        long backlog = outstanding.incrementAndGet();
        if (backlog <= bufferLimit) {
            stalledSince.set(NOT_STALLED);
            return false;
        }
        long now = clock.getAsLong();
        long since = stalledSince.updateAndGet(current -> current == NOT_STALLED ? now : current);
        return now - since > idleTimeoutNanos;
    }

    /**
     * Account downstream demand arriving from the subscriber.
     *
     * @param requested number of frames the subscriber requested
     */
    public void onDownstreamRequest(long requested) {
        if (requested <= 0) {
            return;
        }
        outstanding.addAndGet(-requested);
        if (outstanding.get() <= bufferLimit) {
            stalledSince.set(NOT_STALLED);
        }
    }
}
