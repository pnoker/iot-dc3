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

import io.github.pnoker.common.enums.ErrorCode;
import io.github.pnoker.common.exception.BusinessException;
import io.github.pnoker.common.exception.PasswordChangeRequiredException;
import io.github.pnoker.common.exception.TenantNotScopedException;
import java.util.LinkedHashMap;
import java.util.List;
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
import reactor.core.publisher.Mono;

/** Global RFC 9457 error translation for reactive HTTP endpoints. */
@AutoConfiguration
@Slf4j
@RestControllerAdvice
public class ExceptionConfig {

    @ExceptionHandler(PasswordChangeRequiredException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Mono<ProblemDetailsResponse> passwordChangeRequiredException(
            PasswordChangeRequiredException exception, ServerHttpRequest request, ServerHttpResponse response) {
        return problem(response, request, exception.getErrorCode(), exception.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public Mono<ProblemDetailsResponse> businessException(
            BusinessException exception, ServerHttpRequest request, ServerHttpResponse response) {
        ErrorCode errorCode = exception.getErrorCode();
        response.setStatusCode(HttpStatusCode.valueOf(errorCode.getHttpStatus()));
        return problem(response, request, errorCode, exception.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ProblemDetailsResponse> responseStatusException(
            ResponseStatusException exception, ServerHttpRequest request, ServerHttpResponse response) {
        HttpStatusCode status = exception.getStatusCode();
        response.setStatusCode(status);
        ErrorCode errorCode = mapStatusToErrorCode(status.value());
        String detail = exception.getReason() == null ? status.toString() : exception.getReason();
        return problem(response, request, errorCode, detail, Map.of(), status.value());
    }

    @ExceptionHandler({BindException.class, MethodArgumentNotValidException.class})
    public Mono<ProblemDetailsResponse> methodArgumentNotValidException(
            BindException exception, ServerHttpRequest request, ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNPROCESSABLE_CONTENT);
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(error.getField(), ignored -> new java.util.ArrayList<>())
                    .add(error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage());
        }
        return problem(response, request, ErrorCode.VALIDATION, "Request validation failed", errors);
    }

    /**
     * Input-contract violations thrown by shared paging/query validation (for example
     * {@code PageRequest} range checks) are client errors, not server failures.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ProblemDetailsResponse> illegalArgumentException(
            IllegalArgumentException exception, ServerHttpRequest request, ServerHttpResponse response) {
        log.warn(
                "Invalid request argument, path={}, message={}",
                request.getURI().getRawPath(),
                exception.getMessage());
        return problem(response, request, ErrorCode.OUT_OF_RANGE, exception.getMessage());
    }

    @ExceptionHandler(TenantNotScopedException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ProblemDetailsResponse> tenantNotScopedException(
            TenantNotScopedException exception, ServerHttpRequest request, ServerHttpResponse response) {
        return problem(response, request, ErrorCode.FAILURE, "System error: tenant scope missing");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ProblemDetailsResponse> globalException(
            Exception exception, ServerHttpRequest request, ServerHttpResponse response) {
        log.error(
                "Global exception, path={}, message={}",
                request.getURI().getRawPath(),
                exception.getMessage(),
                exception);
        return problem(response, request, ErrorCode.FAILURE, "Internal server error");
    }

    private Mono<ProblemDetailsResponse> problem(
            ServerHttpResponse response, ServerHttpRequest request, ErrorCode code, String detail) {
        return problem(response, request, code, detail, Map.of());
    }

    private Mono<ProblemDetailsResponse> problem(
            ServerHttpResponse response,
            ServerHttpRequest request,
            ErrorCode code,
            String detail,
            Map<String, List<String>> errors) {
        return problem(response, request, code, detail, errors, code.getHttpStatus());
    }

    private Mono<ProblemDetailsResponse> problem(
            ServerHttpResponse response,
            ServerHttpRequest request,
            ErrorCode code,
            String detail,
            Map<String, List<String>> errors,
            int status) {
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        response.setStatusCode(HttpStatusCode.valueOf(status));
        String title = code.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        String traceId = request.getHeaders().getFirst("X-Request-Id");
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
