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

package io.github.pnoker.common.driver.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Per-device serialization lock to prevent concurrent command execution on
 * the same device, avoiding protocol-level interleaving.
 * <p>
 * Lock instances are created lazily per deviceId and removed when no caller is
 * using or waiting on them. This keeps malformed command ids from growing the
 * lock table permanently.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
@Slf4j
@Component
public class DeviceLockManager {

    private final ConcurrentHashMap<Long, LockRef> locks = new ConcurrentHashMap<>();

    /**
     * Execute an action under the per-device exclusive lock.
     *
     * @param deviceId device to serialize against
     * @param action   the I/O action to execute
     */
    public void runExclusive(Long deviceId, Runnable action) {
        runExclusive(deviceId, () -> {
            action.run();
            return null;
        });
    }

    /**
     * Execute a supplier under the per-device exclusive lock and return its result.
     *
     * @param deviceId device to serialize against
     * @param action   the I/O action to execute
     * @param <T>      result type
     * @return the result of {@code action}
     */
    public <T> T runExclusive(Long deviceId, Supplier<T> action) {
        if (Objects.isNull(deviceId)) {
            throw new IllegalArgumentException("deviceId must not be null");
        }
        LockRef ref = acquire(deviceId);
        ref.lock.lock();
        try {
            return action.get();
        } finally {
            try {
                ref.lock.unlock();
            } finally {
                release(deviceId, ref);
            }
        }
    }

    /**
     * Acquire (or increment the reference count on) the lock ref for a device.
     *
     * @param deviceId the device to lock
     * @return the lock ref
     */
    private LockRef acquire(Long deviceId) {
        return locks.compute(deviceId, (id, current) -> {
            LockRef ref = Objects.nonNull(current) ? current : new LockRef();
            ref.references++;
            return ref;
        });
    }

    /**
     * Release the lock ref for a device, decrementing its reference count and removing
     * it when the count reaches zero. Stale refs (from a different acquire) are ignored.
     *
     * @param deviceId the device to unlock
     * @param ref      the lock ref returned by acquire
     */
    private void release(Long deviceId, LockRef ref) {
        locks.computeIfPresent(deviceId, (id, current) -> {
            if (current != ref) {
                return current;
            }
            current.references--;
            return current.references == 0 ? null : current;
        });
    }

    private static class LockRef {
        private final ReentrantLock lock = new ReentrantLock();
        private int references;
    }

}
