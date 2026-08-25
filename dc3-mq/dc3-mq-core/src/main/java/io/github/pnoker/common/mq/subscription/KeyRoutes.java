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

package io.github.pnoker.common.mq.subscription;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-adapter registry of (keyPattern, listener) routes for one broker subscription —
 * the client-side router used by adapters whose broker has no binding-level key filter
 * ({@link KeyMatcher} documents the matching semantics).
 *
 * <p>Routing rules:
 * <ul>
 *     <li>a message is offered only to listeners whose keyPattern matches its partition
 *     key (blank pattern matches everything);</li>
 *     <li>when several listeners in this JVM match, delivery <b>round-robins</b> among
 *     them, so same-pattern competing listeners keep exactly-once-per-JVM semantics;
 *     cross-JVM load balancing stays the broker's job via the shared consumer group;</li>
 *     <li>when no listener in this JVM matches, {@link #next(String)} returns null and
 *     the adapter acknowledges and skips the message (Rabbit unroutable-drop semantics —
 *     with one consumer per (topic, group) per JVM, a matching listener on another JVM
 *     would have been the message's only home when it exists).</li>
 * </ul>
 *
 * @author pnoker
 * @since 2026.8.25
 */
public final class KeyRoutes<L> {

    private record Route<L>(String keyPattern, L listener) {
    }

    private final List<Route<L>> routes = new CopyOnWriteArrayList<>();
    private final AtomicInteger cursor = new AtomicInteger();

    /**
     * @return true when no route is registered
     */
    public boolean isEmpty() {
        return routes.isEmpty();
    }

    /**
     * @return the number of registered routes
     */
    public int size() {
        return routes.size();
    }

    /**
     * Register a listener under a key pattern.
     *
     * @param keyPattern subscription key pattern; blank matches every key
     * @param listener   the raw listener
     */
    public void add(String keyPattern, L listener) {
        routes.add(new Route<>(Objects.requireNonNull(keyPattern, "keyPattern"), Objects.requireNonNull(listener)));
    }

    /**
     * Pick the listener that should handle a message with this key, round-robin among
     * the matching routes.
     *
     * @param key the message partition key
     * @return the chosen listener, or null when no registered pattern matches
     */
    public L next(String key) {
        if (routes.isEmpty()) {
            return null;
        }
        if (routes.size() == 1) {
            Route<L> only = routes.get(0);
            return KeyMatcher.matches(only.keyPattern(), key) ? only.listener() : null;
        }
        int start = Math.abs(cursor.getAndIncrement());
        for (int offset = 0; offset < routes.size(); offset++) {
            Route<L> route = routes.get((start + offset) % routes.size());
            if (KeyMatcher.matches(route.keyPattern(), key)) {
                return route.listener();
            }
        }
        return null;
    }
}
