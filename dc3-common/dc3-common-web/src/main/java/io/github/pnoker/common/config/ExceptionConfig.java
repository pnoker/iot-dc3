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

import io.github.pnoker.common.constant.common.RequestIdConstant;
import io.github.pnoker.common.enums.ErrorCode;
import io.github.pnoker.common.exception.BusinessException;
import io.github.pnoker.common.exception.PasswordChangeRequiredException;
import io.github.pnoker.common.exception.TenantNotScopedException;
import io.github.pnoker.common.filter.RequestIdWebFilter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Global RFC 9457 error translation for reactive HTTP endpoints. */
@AutoConfiguration
@Slf4j
@RestControllerAdvice
public class ExceptionConfig {

    /** Translate password-change-required failures to RFC 9457 responses. */
    @ExceptionHandler(PasswordChangeRequiredException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Mono<ProblemDetailsResponse> passwordChangeRequiredException(
            PasswordChangeRequiredException exception, ServerWebExchange exchange) {
        return problem(exchange, exception.getErrorCode(), exception.getMessage());
    }

    /**
     * Translate method-security denials to RFC 9457 responses. Method-level
     * {@code @PreAuthorize} rejections surface here (not at the security filter
     * chain's access-denied handler) because they are thrown from inside the
     * handler invocation — without this mapping every permission denial would
     * fall through to the generic handler and masquerade as a 500 server error.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Mono<ProblemDetailsResponse> accessDeniedException(
            org.springframework.security.access.AccessDeniedException exception, ServerWebExchange exchange) {
        return problem(exchange, ErrorCode.FORBIDDEN, exception.getMessage());
    }

    /**
     * Translate business failures to RFC 9457 responses with their error code. 5xx-classified
     * failures are logged once here (no stack — the throwing layer logs the detail), while
     * routine 4xx client errors stay silent to keep the log signal clean.
     */
    @ExceptionHandler(BusinessException.class)
    public Mono<ProblemDetailsResponse> businessException(BusinessException exception, ServerWebExchange exchange) {
        ErrorCode errorCode = exception.getErrorCode();
        if (errorCode.getHttpStatus() >= 500) {
            log.warn(
                    "Business failure, path={}, errorCode={}, message={}",
                    exchange.getRequest().getURI().getRawPath(),
                    errorCode,
                    exception.getMessage());
        }
        exchange.getResponse().setStatusCode(HttpStatusCode.valueOf(errorCode.getHttpStatus()));
        return problem(exchange, errorCode, exception.getMessage());
    }

    /** Translate framework status exceptions to RFC 9457 responses. */
    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ProblemDetailsResponse> responseStatusException(
            ResponseStatusException exception, ServerWebExchange exchange) {
        HttpStatusCode status = exception.getStatusCode();
        exchange.getResponse().setStatusCode(status);
        ErrorCode errorCode = mapStatusToErrorCode(status.value());
        String detail = exception.getReason() == null ? status.toString() : exception.getReason();
        return problem(exchange, errorCode, detail, Map.of(), status.value());
    }

    /** Translate request validation failures to RFC 9457 responses with field errors. */
    @ExceptionHandler({BindException.class, MethodArgumentNotValidException.class})
    public Mono<ProblemDetailsResponse> methodArgumentNotValidException(
            BindException exception, ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNPROCESSABLE_CONTENT);
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(error.getField(), ignored -> new java.util.ArrayList<>())
                    .add(error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage());
        }
        return problem(exchange, ErrorCode.VALIDATION, "Request validation failed", errors);
    }

    /**
     * Input-contract violations thrown by shared paging/query validation (for example
     * {@code PageRequest} range checks) are client errors, not server failures.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ProblemDetailsResponse> illegalArgumentException(
            IllegalArgumentException exception, ServerWebExchange exchange) {
        log.warn(
                "Invalid request argument, path={}, message={}",
                exchange.getRequest().getURI().getRawPath(),
                exception.getMessage());
        return problem(exchange, ErrorCode.OUT_OF_RANGE, exception.getMessage());
    }

    /** Translate missing tenant scope to a 500 system error. */
    @ExceptionHandler(TenantNotScopedException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ProblemDetailsResponse> tenantNotScopedException(
            TenantNotScopedException exception, ServerWebExchange exchange) {
        return problem(exchange, ErrorCode.FAILURE, "System error: tenant scope missing");
    }

    /** Translate unhandled exceptions to a 500 system error without leaking details. */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ProblemDetailsResponse> globalException(Exception exception, ServerWebExchange exchange) {
        log.error(
                "Global exception, path={}, message={}",
                exchange.getRequest().getURI().getRawPath(),
                exception.getMessage(),
                exception);
        return problem(exchange, ErrorCode.FAILURE, "Internal server error");
    }

    private Mono<ProblemDetailsResponse> problem(ServerWebExchange exchange, ErrorCode code, String detail) {
        return problem(exchange, code, detail, Map.of());
    }

    private Mono<ProblemDetailsResponse> problem(
            ServerWebExchange exchange, ErrorCode code, String detail, Map<String, List<String>> errors) {
        return problem(exchange, code, detail, errors, code.getHttpStatus());
    }

    private Mono<ProblemDetailsResponse> problem(
            ServerWebExchange exchange,
            ErrorCode code,
            String detail,
            Map<String, List<String>> errors,
            int status) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        response.setStatusCode(HttpStatusCode.valueOf(status));
        String title = code.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        String traceId = resolveTraceId(exchange);
        return Mono.just(new ProblemDetailsResponse(
                "about:blank",
                title,
                status,
                code.getCode(),
                detail,
                request.getURI().getPath(),
                traceId,
                errors));
    }

    /**
     * Resolve the correlation id for problem details: the id the request-id filter resolved
     * (inbound header, trace id, or generated fallback) whenever the filter ran, falling back
     * to the raw inbound header for requests the filter never reached.
     */
    private String resolveTraceId(ServerWebExchange exchange) {
        String traceId = exchange.getAttribute(RequestIdWebFilter.EXCHANGE_ATTRIBUTE_KEY);
        if (traceId != null) return traceId;
        return exchange.getRequest().getHeaders().getFirst(RequestIdConstant.HEADER);
    }

    private ErrorCode mapStatusToErrorCode(int status) {
        return switch (status) {
            case 401 -> ErrorCode.UNAUTHORIZED;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.NOT_FOUND;
            case 422 -> ErrorCode.VALIDATION;
            default -> ErrorCode.FAILURE;
        };
    }
}
