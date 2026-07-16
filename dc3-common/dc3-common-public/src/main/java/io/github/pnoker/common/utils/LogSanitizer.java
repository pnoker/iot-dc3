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

package io.github.pnoker.common.utils;

/**
 * Lightweight log-argument sanitizer that prevents log-forging via
 * line-break injection. Use the {@link #sanitize(Object)} variant
 * inside SLF4J parameterized log calls whose arguments may contain
 * user-controlled strings.
 *
 * @author pnoker
 * @version 2026.7.0
 * @since 2026.7.0
 */
public final class LogSanitizer {

    private LogSanitizer() {
    }

    /**
     * Replace CR and LF characters with their escaped representations.
     *
     * @param input the value to sanitize; may be {@code null}
     * @return the sanitized string, or {@code "null"} for a null input
     */
    public static String sanitize(Object input) {
        if (input == null) {
            return "null";
        }
        return input.toString()
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
