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
package io.github.pnoker.common.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

/** RFC 9457 problem details payload used by every WebFlux endpoint. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ProblemDetailsResponse(
        String type,
        String title,
        int status,
        String code,
        String detail,
        String instance,
        String traceId,
        Map<String, List<String>> errors) {

    public ProblemDetailsResponse {
        type = type == null || type.isBlank() ? "about:blank" : type;
        title = title == null || title.isBlank() ? "Request failed" : title;
        detail = detail == null ? title : detail;
        errors = errors == null ? Map.of() : Map.copyOf(errors);
    }

    /** Create the response from the problem fields. */
    public static ProblemDetailsResponse of(
            int status, String code, String title, String detail, String instance, String traceId) {
        return new ProblemDetailsResponse("about:blank", title, status, code, detail, instance, traceId, Map.of());
    }
}
