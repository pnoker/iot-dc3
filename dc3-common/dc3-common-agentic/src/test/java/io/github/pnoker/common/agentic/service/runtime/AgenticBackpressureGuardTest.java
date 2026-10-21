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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/**
 * Bounded-backpressure accounting: transient consumer stalls are tolerated up to the
 * buffer budget, and only a stall that persists past the idle timeout fails the turn.
 */
class AgenticBackpressureGuardTest {

    private static final long LIMIT = 4;
    private static final long TIMEOUT_NANOS = 1_000_000L;

    @Test
    void framesWithinTheBufferBudgetNeverFail() {
        AtomicLong clock = new AtomicLong(0);
        AgenticBackpressureGuard guard = AgenticBackpressureGuard.create(LIMIT, TIMEOUT_NANOS, clock::get);
        for (int i = 0; i < (int) LIMIT; i++) {
            assertThat(guard.onFrameEmitted()).as("frame " + i).isFalse();
        }
    }

    @Test
    void aStallWithinTheGracePeriodIsTolerated() {
        AtomicLong clock = new AtomicLong(0);
        AgenticBackpressureGuard guard = AgenticBackpressureGuard.create(LIMIT, TIMEOUT_NANOS, clock::get);
        for (int i = 0; i < (int) LIMIT + 2; i++) {
            assertThat(guard.onFrameEmitted()).as("over budget but within grace").isFalse();
        }
        // The timeout is measured from the first over-budget frame; not reached yet.
        clock.set(TIMEOUT_NANOS - 1);
        assertThat(guard.onFrameEmitted()).isFalse();
    }

    @Test
    void aStallPastTheGracePeriodFailsTheTurn() {
        AtomicLong clock = new AtomicLong(0);
        AgenticBackpressureGuard guard = AgenticBackpressureGuard.create(LIMIT, TIMEOUT_NANOS, clock::get);
        for (int i = 0; i < (int) LIMIT; i++) {
            guard.onFrameEmitted();
        }
        // First over-budget frame opens the stall window.
        clock.set(1_000_000L);
        assertThat(guard.onFrameEmitted()).as("window opens").isFalse();
        // Still over budget and no demand past the grace period: the turn fails.
        clock.set(1_000_000L + TIMEOUT_NANOS + 1);
        assertThat(guard.onFrameEmitted()).as("grace exceeded with zero demand").isTrue();
    }

    @Test
    void downstreamDemandResetsTheStallTimer() {
        AtomicLong clock = new AtomicLong(0);
        AgenticBackpressureGuard guard = AgenticBackpressureGuard.create(LIMIT, TIMEOUT_NANOS, clock::get);
        for (int i = 0; i < (int) LIMIT + 3; i++) {
            guard.onFrameEmitted();
        }
        // The consumer catches up: the stall window must reset, not accumulate.
        guard.onDownstreamRequest(LIMIT + 3);
        clock.set(TIMEOUT_NANOS * 10);
        assertThat(guard.onFrameEmitted()).as("demand arrived, timer reset").isFalse();
    }

    @Test
    void aSecondStallAfterCatchUpUsesAFreshWindow() {
        AtomicLong clock = new AtomicLong(0);
        AgenticBackpressureGuard guard = AgenticBackpressureGuard.create(LIMIT, TIMEOUT_NANOS, clock::get);
        for (int i = 0; i < (int) LIMIT + 1; i++) {
            guard.onFrameEmitted();
        }
        guard.onDownstreamRequest(LIMIT + 1);
        // New stall far in the future: frames within the budget stay fine, the first
        // over-budget frame opens a fresh window.
        clock.set(TIMEOUT_NANOS * 5);
        for (int i = 0; i < (int) LIMIT; i++) {
            assertThat(guard.onFrameEmitted()).isFalse();
        }
        assertThat(guard.onFrameEmitted()).as("window reopens").isFalse();
        // Window restarts at that frame; a later frame past the new grace period fails.
        clock.set(TIMEOUT_NANOS * 5 + TIMEOUT_NANOS + 1);
        assertThat(guard.onFrameEmitted()).isTrue();
    }
}
