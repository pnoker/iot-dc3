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

    public static ProblemDetailsResponse of(int status, String code, String title, String detail,
                                            String instance, String traceId) {
        return new ProblemDetailsResponse("about:blank", title, status, code, detail, instance, traceId, Map.of());
    }
}
