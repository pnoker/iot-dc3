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
package io.github.pnoker.db.r2dbc.core.error;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
/** RFC 7807-style problem payload for transport-neutral error reporting. */

public record ProblemDetails(
        URI type,
        String title,
        int status,
        String code,
        String detail,
        URI instance,
        String traceId,
        Map<String, List<String>> errors) {

    public ProblemDetails {
        type = type == null ? URI.create("about:blank") : type;
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (status < 400 || status > 599) {
            throw new IllegalArgumentException("status must be an HTTP error status");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        errors = immutableErrors(errors);
    }

    private static Map<String, List<String>> immutableErrors(Map<String, List<String>> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> copy = new LinkedHashMap<>();
        source.forEach((field, messages) -> {
            if (field == null || field.isBlank()) {
                throw new IllegalArgumentException("error field must not be blank");
            }
            if (messages == null) {
                throw new IllegalArgumentException("error messages must not be null");
            }
            List<String> messageCopy = messages.stream()
                    .map(message -> Objects.requireNonNull(message, "error message must not be null"))
                    .collect(Collectors.toUnmodifiableList());
            copy.put(field, messageCopy);
        });
        return Map.copyOf(copy);
    }
}
