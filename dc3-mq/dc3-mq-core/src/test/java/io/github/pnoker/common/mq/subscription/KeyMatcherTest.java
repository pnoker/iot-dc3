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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the topic-binding wildcard semantics of {@link KeyMatcher}: exact
 * match, {@code *} single word, {@code #} zero-or-more words, prefix patterns and
 * mismatch cases (Rabbit topic-binding parity for the adapters' client-side routers).
 *
 * @author pnoker
 * @since 2026.8.25
 */
class KeyMatcherTest {

    @Test
    void blankPatternMatchesEverything() {
        assertThat(KeyMatcher.matches("", "driver.node1")).isTrue();
        assertThat(KeyMatcher.matches(null, "driver.node1")).isTrue();
        assertThat(KeyMatcher.matches("", "")).isTrue();
        assertThat(KeyMatcher.matches("", null)).isTrue();
    }

    @Test
    void exactLiteralMatch() {
        assertThat(KeyMatcher.matches("driver.node1", "driver.node1")).isTrue();
        assertThat(KeyMatcher.matches("dc3.driver.service", "dc3.driver.service")).isTrue();
        assertThat(KeyMatcher.matches("driver.node1", "driver.node2")).isFalse();
        assertThat(KeyMatcher.matches("driver", "driver.node1")).isFalse();
        assertThat(KeyMatcher.matches("driver.node1.extra", "driver.node1")).isFalse();
    }

    @Test
    void starMatchesExactlyOneWord() {
        assertThat(KeyMatcher.matches("driver.*", "driver.node1")).isTrue();
        assertThat(KeyMatcher.matches("driver.*", "driver.node2")).isTrue();
        assertThat(KeyMatcher.matches("*.node1", "driver.node1")).isTrue();
        assertThat(KeyMatcher.matches("*.*", "driver.node1")).isTrue();
        // one word only: no zero-word and no multi-word matches
        assertThat(KeyMatcher.matches("driver.*", "driver")).isFalse();
        assertThat(KeyMatcher.matches("driver.*", "driver.node1.extra")).isFalse();
        assertThat(KeyMatcher.matches("driver.*.*", "driver.node1")).isFalse();
    }

    @Test
    void hashMatchesZeroOrMoreWords() {
        assertThat(KeyMatcher.matches("driver.#", "driver.node1")).isTrue();
        assertThat(KeyMatcher.matches("driver.#", "driver.node1.extra")).isTrue();
        assertThat(KeyMatcher.matches("driver.#", "driver")).isTrue();
        assertThat(KeyMatcher.matches("#", "anything.at.all")).isTrue();
        assertThat(KeyMatcher.matches("#", "")).isTrue();
        assertThat(KeyMatcher.matches("a.#.b", "a.b")).isTrue();
        assertThat(KeyMatcher.matches("a.#.b", "a.x.y.b")).isTrue();
        assertThat(KeyMatcher.matches("a.#.b", "a.x.y")).isFalse();
        assertThat(KeyMatcher.matches("a.#.b", "x.a.b")).isFalse();
    }

    @Test
    void businessPrefixPatterns() {
        assertThat(KeyMatcher.matches("driver.*", "driver.dc3-driver-mqtt")).isTrue();
        assertThat(KeyMatcher.matches("device.*", "device.dc3-1001")).isTrue();
        assertThat(KeyMatcher.matches("device.*", "driver.dc3-driver-mqtt")).isFalse();
        assertThat(KeyMatcher.matches("driver.*", "device.dc3-1001")).isFalse();
    }

    @Test
    void mismatchCases() {
        assertThat(KeyMatcher.matches("driver.*", null)).isFalse();
        assertThat(KeyMatcher.matches("driver.*", "")).isFalse();
        assertThat(KeyMatcher.matches("a.b.c", "a.b")).isFalse();
        assertThat(KeyMatcher.matches("*", "a.b")).isFalse();
        assertThat(KeyMatcher.matches("*.*.*", "a.b")).isFalse();
        assertThat(KeyMatcher.matches("#.b", "a")).isFalse();
        assertThat(KeyMatcher.matches("a.#", "b")).isFalse();
    }

    @Test
    void keyRoutesRoundRobinAmongMatchingListeners() {
        KeyRoutes<String> routes = new KeyRoutes<>();
        routes.add("driver.*", "driverA");
        routes.add("device.*", "deviceA");

        assertThat(routes.next("driver.n1")).isEqualTo("driverA");
        assertThat(routes.next("driver.n2")).isEqualTo("driverA");
        assertThat(routes.next("device.d1")).isEqualTo("deviceA");
        // no match -> null (adapter acks and skips)
        assertThat(routes.next("task.t1")).isNull();
        assertThat(routes.next("driver.n1")).isEqualTo("driverA");

        // a blank-pattern listener matches every key and competes in the rotation
        routes.add("", "all");
        assertThat(routes.next("task.t1")).isEqualTo("all");
        assertThat(routes.next("driver.n1")).isIn("driverA", "all");
        assertThat(routes.next("driver.n1")).isIn("driverA", "all");
    }

    @Test
    void keyRoutesAlternatesWhenSeveralListenersMatchTheSamePattern() {
        KeyRoutes<String> routes = new KeyRoutes<>();
        routes.add("driver.*", "driverA");
        routes.add("driver.*", "driverB");

        boolean sawA = false;
        boolean sawB = false;
        for (int i = 0; i < 20; i++) {
            String next = routes.next("driver.n" + i);
            sawA = sawA || "driverA".equals(next);
            sawB = sawB || "driverB".equals(next);
            assertThat(next).isIn("driverA", "driverB");
        }
        assertThat(sawA).isTrue();
        assertThat(sawB).isTrue();
    }

    @Test
    void keyRoutesBlankPatternMatchesAllKeys() {
        KeyRoutes<String> routes = new KeyRoutes<>();
        routes.add("", "catchAll");
        assertThat(routes.isEmpty()).isFalse();
        assertThat(routes.next("driver.n1")).isEqualTo("catchAll");
        assertThat(routes.next("")).isEqualTo("catchAll");
        assertThat(routes.next(null)).isEqualTo("catchAll");
    }
}
