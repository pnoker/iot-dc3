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

package io.github.pnoker.common.driver.service;

/**
 * Driver lifecycle hooks invoked by the SDK at well-defined moments — once at
 * startup and on every periodic schedule tick. Drivers that only need to
 * customise their lifecycle can implement this contract on its own; the larger
 * {@link DriverCustomService} aggregates this with the other driver SPIs for
 * convenience.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface DriverLifecycle {

    /**
     * One-shot initialization invoked when the driver process starts.
     * <p>
     * Use this hook to allocate connection pools, register protocol handlers, or
     * warm up any resources the driver needs before the first read or write.
     * Throwing here aborts driver startup; transient failures should be retried
     * inside the implementation rather than escaping the call.
     */
    void initial();

    /**
     * Custom periodic task driven by Quartz.
     * <p>
     * The schedule cron and enable flag are configured through
     * {@code dc3.driver.schedule.custom} so the SDK can drive this method on
     * the driver's behalf without per-driver scheduler wiring.
     * <p>
     * Implementations should keep individual invocations bounded — long-running
     * scans should be split across triggers because the SDK guards against
     * overlapping fires with {@code @DisallowConcurrentExecution}.
     */
    void schedule();

}
