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

import java.util.Objects;

/**
 * Key-pattern matching with RabbitMQ topic-binding wildcard semantics, used by the
 * adapters' client-side topic routers (brokers without binding-level key filters).
 *
 * <p>Semantics (identical to a RabbitMQ topic exchange binding):
 * <ul>
 *     <li>a blank pattern matches every key (topic default subscription);</li>
 *     <li>a pattern without wildcards matches only the exactly equal key;</li>
 *     <li>pattern and key are split into words on {@code '.'};</li>
 *     <li>{@code *} matches exactly one word;</li>
 *     <li>{@code #} matches zero or more words.</li>
 * </ul>
 *
 * <p>Unroutable-delivery rule for routers built on this matcher: when a message's key
 * matches no registered listener in this JVM, the adapter acknowledges and skips it
 * (logged at debug). With one consumer per (topic, group) per JVM every message reaches
 * exactly one JVM, so this mirrors Rabbit semantics — a message unroutable within a
 * binding set is dropped rather than parked.
 *
 * @author pnoker
 * @since 2026.8.25
 */
public final class KeyMatcher {

    private KeyMatcher() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Match a key against a subscription pattern.
     *
     * @param pattern subscription key pattern; blank/null matches everything
     * @param key     the message partition key; null is treated as the empty key
     * @return true when the pattern covers the key
     */
    public static boolean matches(String pattern, String key) {
        if (Objects.isNull(pattern) || pattern.isBlank()) {
            return true;
        }
        if (Objects.isNull(key)) {
            key = "";
        }
        if (pattern.equals(key)) {
            return true;
        }
        return matchesWords(pattern.split("\\."), key.split("\\."), 0, 0);
    }

    /**
     * Classic topic-binding recursion: {@code star} consumes exactly one key word,
     * {@code hash} consumes zero or more.
     */
    private static boolean matchesWords(String[] patternWords, String[] keyWords, int patternIndex, int keyIndex) {
        while (patternIndex < patternWords.length) {
            String word = patternWords[patternIndex];
            if ("#".equals(word)) {
                if (patternIndex == patternWords.length - 1) {
                    // trailing hash consumes everything, including nothing
                    return true;
                }
                for (int skip = keyIndex; skip <= keyWords.length; skip++) {
                    if (matchesWords(patternWords, keyWords, patternIndex + 1, skip)) {
                        return true;
                    }
                }
                return false;
            }
            if (keyIndex >= keyWords.length) {
                return false;
            }
            if (!"*".equals(word) && !word.equals(keyWords[keyIndex])) {
                return false;
            }
            patternIndex++;
            keyIndex++;
        }
        return keyIndex == keyWords.length;
    }
}
