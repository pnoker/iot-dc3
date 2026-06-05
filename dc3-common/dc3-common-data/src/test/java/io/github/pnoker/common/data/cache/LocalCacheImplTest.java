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

package io.github.pnoker.common.data.cache;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class LocalCacheImplTest {

    private LocalCacheImpl cache;

    @BeforeEach
    void setUp() {
        cache = new LocalCacheImpl();
        cache.init();
    }

    @Test
    void setKeyWithoutTtlPersistsValueIndefinitely() {
        cache.setKey("k1", "v1");
        assertThat((String) cache.getKey("k1")).isEqualTo("v1");
    }

    @Test
    void setKeyWithTtlReturnsValueWithinWindow() {
        cache.setKey("k2", "v2", 5, TimeUnit.SECONDS);
        assertThat((String) cache.getKey("k2")).isEqualTo("v2");
    }

    @Test
    void setKeyWithNonPositiveTtlFallsBackToInfinite() {
        cache.setKey("k3", "v3", 0, TimeUnit.SECONDS);
        assertThat((String) cache.getKey("k3")).isEqualTo("v3");
        cache.setKey("k4", "v4", -1, TimeUnit.SECONDS);
        assertThat((String) cache.getKey("k4")).isEqualTo("v4");
    }

    @Test
    void setKeyMapPersistsAllEntries() {
        cache.setKey(Map.of("a", "1", "b", "2"));
        assertThat((String) cache.getKey("a")).isEqualTo("1");
        assertThat((String) cache.getKey("b")).isEqualTo("2");
    }

    @Test
    void setKeyMapIgnoresNullOrEmpty() {
        assertThatNoException().isThrownBy(() -> cache.setKey((Map<String, String>) null));
        assertThatNoException().isThrownBy(() -> cache.setKey(Map.of()));
    }

    @Test
    void getKeyReturnsNullForUnknownKey() {
        assertThat((String) cache.getKey("missing")).isNull();
    }

    @Test
    void getKeyListReturnsValuesInOrderWithNullForMisses() {
        cache.setKey("a", "1");
        cache.setKey("c", "3");
        List<String> hits = cache.getKey(List.of("a", "b", "c"));
        assertThat(hits).containsExactly("1", null, "3");
    }

    @Test
    void getKeyListReturnsEmptyForBlankInput() {
        assertThat(cache.getKey((List<String>) null)).isEmpty();
        assertThat(cache.getKey(List.of())).isEmpty();
    }

    @Test
    void onExpireRegistersListenerThatFiresOnTtlEviction() {
        AtomicReference<String> lastKey = new AtomicReference<>();
        AtomicReference<Object> lastValue = new AtomicReference<>();
        cache.onExpire((key, value) -> {
            lastKey.set(key);
            lastValue.set(value);
        });

        cache.setKey("ephemeral", "v", 50, TimeUnit.MILLISECONDS);
        // Caffeine evaluates expiry lazily on access; touch the cache once expiry has
        // elapsed so the removal listener fires.
        Awaitility.await().atMost(Duration.ofSeconds(2)).pollInterval(Duration.ofMillis(60))
                .untilAsserted(() -> {
                    cache.getKey("ephemeral");
                    assertThat(lastKey.get()).isEqualTo("ephemeral");
                    assertThat(lastValue.get()).isEqualTo("v");
                });
    }

    @Test
    void onExpireDoesNotFireForExplicitOverwrite() {
        AtomicReference<String> lastKey = new AtomicReference<>();
        cache.onExpire((key, value) -> lastKey.set(key));
        cache.setKey("k", "v1");
        cache.setKey("k", "v2");
        assertThat(lastKey.get()).isNull();
    }

    @Test
    void onExpireIgnoresNullListener() {
        assertThatNoException().isThrownBy(() -> cache.onExpire(null));
    }
}
