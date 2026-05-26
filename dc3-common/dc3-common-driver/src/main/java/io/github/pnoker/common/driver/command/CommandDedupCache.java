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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Idempotent deduplication cache for point commands.
 * Tracks commandIds that have already been processed within a 5-minute window.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
@Component
public class CommandDedupCache {

    private final Cache<String, Boolean> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(50_000)
            .build();

    /**
     * Try to acquire the given commandId. Returns true if this is the first
     * acquisition (command should proceed), false if already seen.
     *
     * @param commandId unique command identifier
     * @return true if the commandId was newly acquired, false if duplicate
     */
    public boolean tryAcquire(String commandId) {
        return cache.asMap().putIfAbsent(commandId, Boolean.TRUE) == null;
    }

    /**
     * Release a previously acquired commandId when a command is going to be retried.
     *
     * @param commandId unique command identifier
     */
    public void release(String commandId) {
        if (commandId != null) {
            cache.invalidate(commandId);
        }
    }
}
